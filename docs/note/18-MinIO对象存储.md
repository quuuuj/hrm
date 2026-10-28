# 数智人事系统（十八）：MinIO 对象存储与文件读写链路

附件原先存放在应用进程所在的机器磁盘上，本地开发够用；容器重建后目录清空，多副本部署时另一台机器也读不到。本期把文件读写交给 MinIO：一个兼容 S3 协议（亚马逊的对象存储接口规范）的本地对象存储服务。文件按桶（bucket，存储的顶层容器）划分，桶内每个文件由对象键（object key，桶内唯一名称）定位，不依赖目录层级。上传时把文件写入对象存储，下载时以流式方式输出，分片合并交给服务端完成，文本类文件额外压缩。

## 前置准备

需完成第 5 期（公共层统一响应）。MinIO 容器在第 2 期已通过 `docker/local/docker-compose.yml` 随中间件启动，本期不涉及知识库摄入，这部分留到第 22 期。

## 本期目标

- 在 `application.yml` 里配好 MinIO 的连接参数，端口与第 2 期启动的容器保持一致
- 应用启动时检查桶是否存在，缺失则创建，配置有误直接终止启动
- 上传的文件写入对象存储，`sys_docs` 表记录对象键、原始大小与实际占用大小
- 下载时比对文件开头的魔数（magic number，标识文件格式的固定字节），判断是否需要解压，并以流式方式写出
- 合并分片改为调用服务端的合并接口，不把整个文件读进内存

## 接入 MinIO

MinIO 容器由第 2 期的 `docker/local/docker-compose.yml` 启动，对外暴露两个端口：9000 供 Java SDK（软件开发工具包，这里指 MinIO 客户端库）调用，9001 是控制台：

```yaml
  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
```

应用侧只需要四项配置（`hrm-server/src/main/resources/application.yml:106`）：

```yaml
storage:
  minio:
    endpoint: http://127.0.0.1:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket: hrm
```

`MinioStorageService` 把客户端构建与桶校验放在 `init()` 方法里（`hrm-server/src/main/java/com/qiujie/common/storage/MinioStorageService.java:42`），该方法标注 `@PostConstruct`，由 Spring 在依赖注入完成后自动调用：

```java
@PostConstruct
void init() {
    this.client = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
    boolean bucketExists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
    if (!bucketExists) {
        client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());   // 桶缺失时创建
    }
    // 连接或创建失败抛异常，终止应用启动：
    // 启动即失败，比等到运行期第一次上传才发现连不上更好排查
}
```

客户端只构建一次，其余方法复用同一个 `client` 字段。写入按内容是否已在内存分成两个方法（`MinioStorageService.java:76`）：

```java
// 接收流：内容不在内存、只能边读边发时用
public void put(String key, InputStream inputStream, String contentType) {
    client.putObject(PutObjectArgs.builder()
            .bucket(bucket).object(key)
            // objectSize 传 -1 表示长度未知（SDK 约定的取值，不是错误参数），
            // partSize 给 5MB，SDK 边读边切分、逐段上传
            .stream(inputStream, -1, 5 * 1024 * 1024L)
            .contentType(contentType)
            .build());
}

// 接收完整字节：内部包成字节流，objectSize 传数组长度
public void put(String key, byte[] bytes) {
    try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
        client.putObject(PutObjectArgs.builder()
                .bucket(bucket).object(key)
                .stream(in, bytes.length, -1)
                .build());
    }
}
```

分片写入与压缩结果回写都走字节数组方法。

## 对象键直接存进 `sys_docs`表

对象键在分片合并阶段生成，此时所有分片已经上传完毕，格式为 `docs/{staffId}/{uploadId}/{随机串}.{扩展名}`，例如 `docs/1/d7693d934d71437a81685b12ce926846/7b0421d2a947caaa6e5d.txt`。这个键直接写进 `sys_docs.name`，下载时按 `name` 取对象，数据库里不再保存磁盘路径，也不需要反查目录结构。

`sys_docs` 除对象键外还记三列，字段定义见 `db/mysql/hrm.sql:1158`，这里只摘本期用到的四列：

```sql
CREATE TABLE `sys_docs`  (
  `name` varchar(200) NULL DEFAULT NULL COMMENT '文件名称',              -- 实际存的是对象键
  -- …… 其余列省略 ……
  `size` bigint UNSIGNED NULL DEFAULT NULL COMMENT '文件原始大小KB',
  `stored_size` bigint UNSIGNED NULL DEFAULT NULL COMMENT '磁盘实际占用(字节)',
  `compressed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否zstd压缩: 0=否 1=是'
);
```

写入时 `size` 取 `rawBytes.length / 1024`，`stored_size` 取压缩后的字节数（`DocsUploadCompletionHandler.java:95`）。`stored_size` 指对象存储中的占用大小。

![文件中心列表展示类型、大小与上传者](https://cdn.jsdelivr.net/gh/quuuuj/hrm@dabb164e8dba0a24fe665dba1c35c7b3681d72c5/img/note/18/02-docs-list-uploaded.png)

## 服务端合并分片

分片上传时，每个分片在桶里是一个独立对象，对象键为 `uploads/{uploadId}/chunks/{下标}`；合并阶段把这些键交给 `composeObject`（`MinioStorageService.java:103`）。应用层当然也能合并：按顺序把每个分片读出来、拼成一个流再上传，但这样全部字节都要经应用进程搬运一遍，大文件会同时占用内存或临时磁盘与带宽。`composeObject` 把合并请求发给 MinIO 服务端，由服务端把源对象按顺序拼成目标对象，应用进程只发一个请求，不参与数据搬运：

```java
List<ComposeSource> sources = sourceKeys.stream()
        .map(k -> ComposeSource.builder().bucket(bucket).object(k).build())
        .collect(Collectors.toList());
client.composeObject(ComposeObjectArgs.builder()
        .bucket(bucket).object(targetKey)
        .sources(sources)
        .build());
```

只有一个分片时改走 `copyObject`，省掉一次合并。分片的上传、断点续传（中断后接着传剩余分片）与过期清理属于上传协议，第 19 期展开。

## 文本文件的压缩与读取

图片与视频通常已是压缩格式，再压缩收益有限；值得额外处理的是文本类附件。`StorageCompressor` 用 zstd（一种无损压缩算法）处理 `txt`、`csv`、`json`、`xml`、`svg`、`log` 六种扩展名，且原始数据不小于 1KB（`hrm-server/src/main/java/com/qiujie/util/StorageCompressor.java:25`）。压缩后若没有变小，按原数据存：

```java
if (rawData.length < MIN_COMPRESS_BYTES || !isCompressible(extName)) {
    return new CompressionResult(rawData, false);
}
byte[] compressed = compress(rawData);
if (compressed.length >= rawData.length) {
    return new CompressionResult(rawData, false);
}
return new CompressionResult(compressed, true);
```

压缩结果覆盖同一个对象键，不新增副本。读取侧本可以按 `sys_docs.compressed` 字段决定要不要解压，项目没有这样做：这个字段只是写入时的结果记录，压缩规则一旦调整（改阈值、加减扩展名），新旧文件就会在同一个桶里共存，靠字段判断等于要求每次改规则都回填历史行。判断改成只看数据本身，`download` 先读前 4 个字节，与 zstd 压缩帧的魔数 `28 B5 2F FD` 比对（`hrm-server/src/main/java/com/qiujie/docs/service/DocsService.java:91`）：

```java
try (InputStream in = rawStream) {
    byte[] header = StorageCompressor.peekMagicBytes(in);   // 只取 4 字节
    OutputStream out = response.getOutputStream();

    if (header.length == 4
            && header[0] == (byte) 0x28 && header[1] == (byte) 0xB5
            && header[2] == 0x2F && header[3] == (byte) 0xFD) {
        // 命中魔数：把 4 个字节拼回剩余流，外面套一层 ZstdInputStream 边读边解压
        try (ByteArrayInputStream headerStream = new ByteArrayInputStream(header);
             InputStream fullStream = new SequenceInputStream(headerStream, in);
             InputStream decompressed = StorageCompressor.decompressStream(fullStream)) {
            IoUtil.copy(decompressed, out, 8192);
        }
    } else {
        // 没命中：这 4 个字节先写出去，其余原样流出
        out.write(header);
        IoUtil.copy(in, out, 8192);
    }
}
```

读取过程保持流式：`peekMagicBytes` 只取 4 字节，之后 `IoUtil.copy` 按 8KB 缓冲搬运，几十 MB 的附件也不会整块进内存。

下载全程经后端输出流返回，没有使用对象存储的临时直链（预签名 URL，由服务端签出一段限时链接、客户端直接向存储服务取文件）。收益是权限校验与解压留在同一处完成，浏览器也不需要访问 MinIO 的地址；代价是文件流量走应用服务器，附件规模继续增长时需要重新考虑分担方式。

![写入与读取两条链路](https://cdn.jsdelivr.net/gh/quuuuj/hrm@dabb164e8dba0a24fe665dba1c35c7b3681d72c5/img/note/18/04-storage-flow.png)

## 任务产物的旧路径兼容

文件任务引擎负责异步执行 Excel 导入导出（第 20 期展开），需要读取上传的源文件，并写入结果文件与错行文件（记录导入失败的行号、原始数据与错误原因，供下载核对），这三类文件统称任务产物。早期版本把它们放在本地临时目录，数据库里记录的是绝对路径。切换存储介质时，历史记录有两种处理方式：写一个一次性脚本把旧文件全部搬进对象存储并改写库里的路径，或者在读取端同时兼容两种形态、让旧记录保持原样。项目选后者，`MinioArtifactStore` 在读取与删除时按路径形态分流（`hrm-server/src/main/java/com/qiujie/filetask/store/MinioArtifactStore.java:28`）：

```java
@Override
public boolean isRemote(String path) {
    // 含路径分隔符或以 / 开头 → 本地路径；其余 → 对象键
    return path != null && !path.contains(File.separator) && !path.startsWith("/");
}

@Override
public File resolveSource(String source) {
    if (source == null || !isRemote(source)) {
        return new File(source);            // 旧记录：本地文件直接交给解析器
    }
    File tempFile = new File(TEMP_DIR, source);
    tempFile.getParentFile().mkdirs();
    try (InputStream in = storageService.get(source)) {
        FileUtil.writeFromStream(in, tempFile);   // 新记录：先下载到临时目录
    }
    return tempFile;
}
```

`upload` 完成后删除本地临时文件。

这条规则让历史任务记录不必迁移：旧记录继续读本地文件，新任务写对象存储。代价是判断结果依赖路径形态：Windows 上分隔符是反斜杠，含正斜杠的对象键仍判为远程；Linux 上分隔符就是正斜杠，同样的键会被判成本地路径。对象键能否按远程读取，取决于部署机器的操作系统。

## 三处定时清理

三处清理各管一段：

- 过期上传会话：每小时扫描 `expires_at` 已过的会话，逐个删除分片对象与表记录（`hrm-server/src/main/java/com/qiujie/filetask/service/FileUploadService.java:280`）；
- 任务产物：每日 3 点清理创建时间超过保留期的任务，删除源文件、结果文件、错行文件与关联通知，保留期由 `file-task.retention-days` 控制，默认 30 天（`hrm-server/src/main/java/com/qiujie/filetask/service/FileTaskCleanupJob.java:37`）；
- 孤儿文件：每日 4 点扫描桶内所有对象键（`DocsService.java:245`）：

```java
@Scheduled(cron = "0 0 4 * * ?")
public void cleanOrphanDocsFiles() {
    Set<String> dbFilenames = list().stream()
            .map(Docs::getName)                  // sys_docs 中登记过的对象键
            .collect(Collectors.toSet());

    long cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000;   // 24 小时阈值

    for (String key : storageService.listKeys()) {
        if (dbFilenames.contains(key)) {
            continue;                            // 登记过的不动
        }
        Date lastModified = storageService.getLastModified(key);
        if (lastModified != null && lastModified.getTime() > cutoff) {
            continue;                            // 最后修改时间不足 24 小时的留着
        }
        storageService.delete(key);
    }
}
```

## 效果验证

上传一份 6696 字节的纯文本员工手册后，列表出现一条记录，类型为 `txt`、大小为 6KB。查询数据库确认压缩结果：

```sql
SELECT id, name, size, stored_size, compressed
FROM hrm.sys_docs ORDER BY id DESC LIMIT 1;
```

`name` 是对象键，`size` 为 6，`stored_size` 为 599，`compressed` 为 1——与压缩日志 `6696 -> 599 bytes` 一致。

在 MinIO 控制台按同一对象键逐级进入 `hrm → docs → 1 → uploadId`，能看到唯一一个 `.txt` 对象，大小 599.0 B：

![MinIO 控制台中按对象键定位文件](https://cdn.jsdelivr.net/gh/quuuuj/hrm@dabb164e8dba0a24fe665dba1c35c7b3681d72c5/img/note/18/03-minio-object.png)

点击列表的“下载”，得到的是原文而非压缩帧，说明魔数判断与解压链路生效。

## 小坑提醒

- **下载路由要匹配整段对象键**：对象键含多级斜杠，`@GetMapping("/download/{filename}")` 只匹配单段路径，请求会落到静态资源处理器，找不到资源抛出异常后被兜底异常处理器接住，返回通用失败的 `code=300`；改用 `{*filename}` 通配后，捕获值带前导斜杠，查对象前需要去掉。
- **是否压缩只能看数据，不能看标记**：`sys_docs.compressed` 是结果记录，读路径按魔数判断；调整压缩规则后，历史文件与新文件可以在同一个桶里共存。
- **孤儿清理只认 `sys_docs` 里的对象键**：任务产物、分片这类键不在表内，同样受 24 小时阈值约束，与任务侧 30 天保留期并不一致，调整任一侧阈值前先确认两类文件的归属。
- **启动即校验**：`init()` 里连不上 MinIO 会直接抛异常；本地忘记启动容器时，应用不会半途启动成功再在第一次上传时报错。

## 小结

- **对象键直接存进 `sys_docs`**：`sys_docs.name` 存完整键，下载按 `name` 直接取对象，库里不再保存磁盘路径；
- **合并交给服务端**：`composeObject` 只发请求不搬数据，单分片退化为 `copyObject`，代价是要求分片先全部落成独立对象；
- **压缩有选择性**：仅六种文本扩展名且不小于 1KB，压缩无效时按原数据存；
- **读取按魔数解压**：前 4 字节命中 zstd 帧头才解压，不依赖 `compressed` 字段，全程 8KB 缓冲流式传输；
- **下载经后端代理**：未使用预签名直链，权限校验与解压同处完成，代价是文件流量走应用服务器；
- **兼容旧路径**：任务产物按路径形态区分本地与远程，历史记录无需迁移，判断结果依赖操作系统；
- **三处清理分工**：过期会话每小时、孤儿对象每日 4 点、任务产物按保留期每日 3 点。

下期进入分片上传协议：`init → chunks → complete` 三阶段如何实现秒传（文件哈希已存在时直接复用对象，不再上传）与断点续传，256MB 上限、10MB 分片与 24 小时会话有效期各自约束什么。

**GitHub**：https://github.com/quuuuj/hrm
