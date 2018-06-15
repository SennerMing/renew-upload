package com.zw.renewupload.common;
//上传前文件检测
public class CheckFileResult {
    private String fileMd5;
    //0:锁未占用,1:锁占用
    private Integer lock;
    //文件分块数量
    private Integer chunkNum;
    //每块文件大小
    private Integer chunkSize;
    //当前已传输到第几块
    private Integer chunkCurr;
    //当前文件总大小
    private Long totalSize;
    //访问路径
    private  String viewPath;

    public String getViewPath() {
        return viewPath;
    }

    public void setViewPath(String viewPath) {
        this.viewPath = viewPath;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public Integer getLock() {
        return lock;
    }

    public void setLock(Integer lock) {
        this.lock = lock;
    }

    public Integer getChunkNum() {
        return chunkNum;
    }

    public void setChunkNum(Integer chunkNum) {
        this.chunkNum = chunkNum;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getChunkCurr() {
        return chunkCurr;
    }

    public void setChunkCurr(Integer chunkCurr) {
        this.chunkCurr = chunkCurr;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }
}

