package com.zw.renewupload.entity;

/**
 * @ClassName: UploadChunk
 * @Description: TODO
 * @Author: LXR
 * @DATE: 2020/12/1 10:59
 * @Version: 1.0
 */
public class UploadChunk {
    private String fileName;
    private String fileMd5;
    private Integer chunks;
    private Integer chunk;
    private Long fileSize;
    private Long chunkSize;
    private String chunkTmpPath;

    private Integer status = 0;
    private String filePath;

    public UploadChunk() {
    }

    public UploadChunk(String fileName, String fileMd5, Integer chunks,
                       Integer chunk, Long fileSize, Long chunkSize, String chunkTmpPath) {
        this.fileName = fileName;
        this.fileMd5 = fileMd5;
        this.chunks = chunks;
        this.chunk = chunk;
        this.fileSize = fileSize;
        this.chunkSize = chunkSize;
        this.chunkTmpPath = chunkTmpPath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public Integer getChunks() {
        return chunks;
    }

    public void setChunks(Integer chunks) {
        this.chunks = chunks;
    }

    public Integer getChunk() {
        return chunk;
    }

    public void setChunk(Integer chunk) {
        this.chunk = chunk;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getChunkTmpPath() {
        return chunkTmpPath;
    }

    public void setChunkTmpPath(String chunkTmpPath) {
        this.chunkTmpPath = chunkTmpPath;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
