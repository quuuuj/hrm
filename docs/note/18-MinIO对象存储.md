# 数智人事系统（十八）：MinIO 对象存储与文件读写链路

附件放在应用进程所在磁盘上，本地开发够用；容器重建后目录清空，多副本部署时另一台机器也读不到。本期把文件读写交给 MinIO：上传写入对象存储，下载流式输出，分片合并交给服务端，文本类文件额外压缩。

## 前置准备

需完成第 5 期（公共层统一响应）。本期用到 `Response` 与第 2 期起的 MinIO 容器，不涉及知识库摄入，第 22 期再展开。

## 本期目标

- 在 `docker/local/docker-compose.yml` 启动 MinIO，并在 `application.yml` 配置连接参数
- 启动时确认桶存在，不存在则创建；配置错误直接中止启动
- 上传文件写入对象存储，`sys_docs` 记录对象键、原始大小与实际占用
- 下载时按魔数判断是否解压，以流式方式写出
- 合并分片改用服务端合并接口，不把整个文件读进内存

## 接入 MinIO

容器部分第 2 期已经起好。MinIO 暴露 9000 端口供 SDK 调用，9001 是控制台：

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

`MinioStorageService` 在 `@PostConstruct` 里构建客户端，并调用 `bucketExists` 检查桶，不存在时 `makeBucket` 创建，失败则抛出异常让启动中断（`hrm-server/src/main/java/com/qiujie/common/storage/MinioStorageService.java:43`）。启动即失败比运行期第一次上传才发现连不上要好排查。

写入有两个重载：`put(key, InputStream, contentType)` 用于流式写入，`put(key, byte[])` 用于已知长度的字节数组。前者传 `-1` 作为长度，SDK 按 5MB 分片自动切分；后者供上传完成回调写回压缩后的字节。

## 对象键直接存进 `sys_docs`

对象键在上传完成时生成，格式为 `docs/{staffId}/{uploadId}/{随机串}.{扩展名}`，例如 `docs/1/d7693d934d71437a81685b12ce926846/7b0421d2a947caaa6e5d.txt`。对象键直接写进 `sys_docs.name`，下载时按 `name` 取对象，不再保存磁盘路径、也不需要在库里反查目录结构。

`sys_docs` 里同时记录三列：`size` 是原始大小、按 KB 取整，`stored_size` 是实际写入的字节数，`compressed` 标记是否压缩（`hrm-server/src/main/java/com/qiujie/docs/service/DocsUploadCompletionHandler.java:100`）。`stored_size` 与 `compressed` 用于核对真实占用，业务上一律以 `name` 为准。

![文件中心列表展示类型、大小与上传者](https://cdn.jsdelivr.net/gh/quuuuj/hrm@dabb164e8dba0a24fe665dba1c35c7b3681d72c5/img/note/18/02-docs-list-uploaded.png)

## 服务端合并分片

分片上传时每个分片是一个独立对象，路径为 `uploads/{uploadId}/chunks/{下标}`；合并阶段把这些键交给 `composeObject`（`MinioStorageService.java:103`）。SDK 会把合并请求发给 MinIO 服务端，由服务端把源对象按顺序拼成目标对象，应用进程只发一个请求，不参与数据搬运：

```java
List<ComposeSource> sources = sourceKeys.stream()
        .map(k -> ComposeSource.builder().bucket(bucket).object(k).build())
        .collect(Collectors.toList());
client.composeObject(ComposeObjectArgs.builder()
        .bucket(bucket).object(targetKey)
        .sources(sources)
        .build());
```

只有一个分片时改走 `copyObject`，省掉一次合并。分片的上传、断点续传与过期清理属于上传协议，第 19 期展开；本期只关注合并这一步落在服务端。

## 文本文件的压缩与读取

图片与视频通常已是压缩格式，再压缩收益有限；值得额外处理的是文本类附件。`StorageCompressor` 只处理 `txt`、`csv`、`json`、`xml`、`svg`、`log` 六种扩展名，且原始数据不小于 1KB（`hrm-server/src/main/java/com/qiujie/util/StorageCompressor.java:25`）。压缩后若没有变小，按原数据存：

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

压缩结果覆盖同一个对象键，不新增副本。是否压缩只从数据本身判断，因此读取侧不能依赖 `sys_docs.compressed` 决定行为：`download` 先读前 4 个字节，比对 zstd 帧魔数 `28 B5 2F FD`（`hrm-server/src/main/java/com/qiujie/docs/service/DocsService.java:93`）。命中则把 4 个字节拼回剩余流，外面套一层 `ZstdInputStream` 边读边解压；没命中就把这 4 个字节写出去，其余原样流出。

这里的关键是全程流式：`peekMagicBytes` 只取 4 字节，之后 `IoUtil.copy` 按 8KB 缓冲搬运，几十 MB 的附件也不会整块进内存。

![写入与读取两条链路](https://cdn.jsdelivr.net/gh/quuuuj/hrm@dabb164e8dba0a24fe665dba1c35c7b3681d72c5/img/note/18/04-storage-flow.png)

## 任务产物的旧路径兼容

文件任务引擎要读源文件、写结果文件与错行文件，早期版本把它们放在本地临时目录，记录的是绝对路径。`MinioArtifactStore` 提供了一处分流（`hrm-server/src/main/java/com/qiujie/filetask/store/MinioArtifactStore.java:29`）：路径含分隔符或以 `/` 开头视为本地路径，其余视为对象键。远程键在 `resolveSource` 时先下载到临时目录再交给解析器，`upload` 完成后删除本地临时文件。

这条规则让历史任务记录不必迁移：旧记录继续读本地文件，新任务写对象存储。代价是判断依赖路径形态：Windows 上分隔符是反斜杠，含正斜杠的对象键仍判为远程；Linux 上分隔符就是正斜杠，同样的键会被判成本地路径。对象键能否按远程读取，取决于部署机器的操作系统。

## 三处定时清理

三处清理各管一段：

- 过期上传会话：每小时扫描 `expires_at` 已过的会话，逐个删除分片对象与表记录（`hrm-server/src/main/java/com/qiujie/filetask/service/FileUploadService.java:280`）；
- 孤儿文件：每日 4 点扫描桶内所有键，跳过 `sys_docs` 中登记过的对象键，其余对象在最后修改时间超过 24 小时后删除（`DocsService.java:245`）；
- 任务产物：每日 3 点清理创建时间超过保留期的任务，删除源文件、结果文件、错行文件与关联通知，保留期由 `file-task.retention-days` 控制，默认 30 天（`hrm-server/src/main/java/com/qiujie/filetask/service/FileTaskCleanupJob.java:37`）。

## 效果验证

上传一份 6696 字节的纯文本员工手册，列表出现一条记录，类型为 `txt`、大小为 6KB。查询数据库确认压缩结果：

```sql
SELECT id, name, size, stored_size, compressed
FROM hrm.sys_docs ORDER BY id DESC LIMIT 1;
```

`name` 是对象键，`size` 为 6，`stored_size` 为 599，`compressed` 为 1——与压缩日志 `6696 -> 599 bytes` 一致。

在 MinIO 控制台按同一路径逐级进入 `hrm → docs → 1 → uploadId`，能看到唯一一个 `.txt` 对象，大小 599.0 B：

![MinIO 控制台中按对象键定位文件](https://cdn.jsdelivr.net/gh/quuuuj/hrm@dabb164e8dba0a24fe665dba1c35c7b3681d72c5/img/note/18/03-minio-object.png)

点击列表的“下载”，得到的是原文而非压缩帧，说明魔数判断与解压链路生效。

## 小坑提醒

- **下载路由要匹配整段对象键**：对象键含多级斜杠，`@GetMapping("/download/{filename}")` 只匹配单段路径，请求会落到静态资源处理器并返回 300；改用 `{*filename}` 通配后，捕获值带前导斜杠，查对象前需要去掉。
- **是否压缩只能看数据，不能看标记**：`sys_docs.compressed` 是结果记录，读路径按魔数判断；调整压缩规则后，历史文件与新文件可以在同一个桶里共存。
- **孤儿清理只认 `sys_docs` 里的对象键**：任务产物、分片这类键不在表内，同样受 24 小时阈值约束，与任务侧 30 天保留期并不一致，调整任一侧阈值前先确认两类文件的归属。
- **启动即校验**：`@PostConstruct` 里连不上 MinIO 会直接抛异常；本地忘记起容器时，应用不会半途启动成功再在第一次上传时报错。

## 小结

- **对象键直接存进 `sys_docs`**：`sys_docs.name` 存完整键，下载按 `name` 直接取对象，库里不再保存磁盘路径；
- **合并交给服务端**：`composeObject` 只发请求不搬数据，单分片退化为 `copyObject`；
- **压缩有选择性**：仅六种文本扩展名且不小于 1KB，压缩无效时按原数据存，判定全部基于数据本身；
- **读取按魔数解压**：前 4 字节命中 zstd 帧头才解压，全程 8KB 缓冲流式传输；
- **兼容旧路径**：任务产物按路径形态区分本地与远程，历史记录无需迁移，判断结果依赖操作系统；
- **三段清理分工**：过期会话每小时、孤儿对象每日 4 点、任务产物按保留期每日 3 点。

下期进入分片上传协议：`init → chunks → complete` 三阶段如何实现秒传与断点续传，256MB 上限、10MB 分片与 24 小时会话有效期各自约束什么。

**GitHub**：https://github.com/quuuuj/hrm
