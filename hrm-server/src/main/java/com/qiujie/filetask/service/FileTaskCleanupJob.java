package com.qiujie.filetask.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.filetask.entity.FileTask;
import com.qiujie.filetask.entity.FileTaskError;
import com.qiujie.filetask.mapper.FileTaskMapper;
import com.qiujie.filetask.store.ArtifactStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 过期文件任务与关联产物清理定时任务。
 */
@Component
public class FileTaskCleanupJob {

    private final FileTaskMapper fileTaskMapper;
    private final FileTaskErrorService fileTaskErrorService;
    private final ArtifactStore artifactStore;

    public FileTaskCleanupJob(FileTaskMapper fileTaskMapper,
                              FileTaskErrorService fileTaskErrorService,
                              ArtifactStore artifactStore) {
        this.fileTaskMapper = fileTaskMapper;
        this.fileTaskErrorService = fileTaskErrorService;
        this.artifactStore = artifactStore;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredTaskFiles() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(7);
        QueryWrapper<FileTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.lt("create_time", Timestamp.valueOf(expireTime));
        List<FileTask> expiredTasks = fileTaskMapper.selectList(queryWrapper);
        for (FileTask task : expiredTasks) {
            if (task.getSourceFilePath() != null) {
                artifactStore.delete(task.getSourceFilePath());
            }
            if (task.getResultFilePath() != null) {
                artifactStore.delete(task.getResultFilePath());
            }
            if (task.getErrorFilePath() != null) {
                artifactStore.delete(task.getErrorFilePath());
            }
            fileTaskErrorService.remove(new QueryWrapper<FileTaskError>().eq("task_id", task.getId()));
            fileTaskMapper.deleteById(task.getId());
        }
    }
}
