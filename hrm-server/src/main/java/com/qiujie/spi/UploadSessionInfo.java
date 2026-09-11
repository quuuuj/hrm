package com.qiujie.spi;

/**
 * 上传会话摘要信息，传递给 UploadCompletionHandler。
 */
public class UploadSessionInfo {

    private final String uploadId;
    private final String fileName;
    private final String fileExt;
    private final Long fileSize;
    private final String fileHash;
    private final Integer staffId;
    private final int chunkCount;
    /** 是否加入知识库：true 时 onComplete 触发 ETL 摄入，false 只做通用文件存储。 */
    private final boolean ingest;

    public UploadSessionInfo(String uploadId, String fileName, String fileExt,
                              Long fileSize, String fileHash, Integer staffId, int chunkCount,
                              boolean ingest) {
        this.uploadId = uploadId;
        this.fileName = fileName;
        this.fileExt = fileExt;
        this.fileSize = fileSize;
        this.fileHash = fileHash;
        this.staffId = staffId;
        this.chunkCount = chunkCount;
        this.ingest = ingest;
    }

    /** 兼容旧调用点：默认不摄入（通用文件）。 */
    public UploadSessionInfo(String uploadId, String fileName, String fileExt,
                              Long fileSize, String fileHash, Integer staffId, int chunkCount) {
        this(uploadId, fileName, fileExt, fileSize, fileHash, staffId, chunkCount, false);
    }

    public String getUploadId() { return uploadId; }
    public String getFileName() { return fileName; }
    public String getFileExt() { return fileExt; }
    public Long getFileSize() { return fileSize; }
    public String getFileHash() { return fileHash; }
    public Integer getStaffId() { return staffId; }
    public int getChunkCount() { return chunkCount; }
    public boolean isIngest() { return ingest; }
}
