package com.qiujie.filetask.store;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.qiujie.common.storage.MinioStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;

/**
 * 任务文件介质：MinIO 对象存储 + 本地临时目录。
 * <p>
 * 任务记录中的文件路径一律是对象存储 key；本地目录只存放流式处理期间的临时文件。
 * </p>
 */
@Component
public class MinioArtifactStore {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "hrm";

    @Autowired
    private MinioStorageService storageService;

    public File resolveSource(String key) {
        File tempFile = new File(TEMP_DIR, key);
        tempFile.getParentFile().mkdirs();
        try (InputStream in = storageService.get(key)) {
            FileUtil.writeFromStream(in, tempFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download source file from MinIO: " + key, e);
        }
        return tempFile;
    }

    public File createTaskFile(String subDir, String originalFilename) {
        String extName = FileUtil.extName(originalFilename);
        String filename = IdUtil.fastSimpleUUID();
        if (extName != null && !"".equals(extName)) {
            filename = filename + "." + extName;
        }
        File dir = new File(TEMP_DIR, subDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, filename);
    }

    public String upload(File file, String subDir) {
        String key = subDir + "/" + file.getName();
        storageService.put(key, FileUtil.readBytes(file));
        file.delete();
        return key;
    }

    public void delete(String key) {
        if (key == null || "".equals(key)) {
            return;
        }
        if (storageService.exists(key)) {
            storageService.delete(key);
        }
    }

    public boolean exists(String key) {
        return key != null && !"".equals(key) && storageService.exists(key);
    }

    public InputStream open(String key) {
        return storageService.get(key);
    }
}
