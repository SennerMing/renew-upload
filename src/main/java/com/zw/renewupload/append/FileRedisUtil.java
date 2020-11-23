package com.zw.renewupload.append;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zw.renewupload.common.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: FileRedisPath
 * @Description: TODO
 * @Author: LXR
 * @DATE: 2020/11/20 18:47
 * @Version: 1.0
 */
public class FileRedisUtil {

    /**
     * 根据fileMd5获得已经上传的文件大小
     * @return
     */
    public Long getUploadedSize(){
        Long uploadedSize = 0L;
        String uploadSizeStr = RedisUtil.getString(UpLoadConstant.historyUpload + fileMd5);
        if (StrUtil.isNotEmpty(uploadSizeStr)){
            uploadedSize = Convert.toLong(uploadSizeStr);
        }
        return uploadedSize;
    }

    /**
     * 根据fileMd5设置当前文件已经上传的大小
     * @param upLoadedSize
     */
    public void setUploadedSize(Long upLoadedSize){
        Long uploadedSize = upLoadedSize+getUploadedSize();
        RedisUtil.setString(UpLoadConstant.historyUpload + fileMd5,Convert.toStr(uploadedSize));
    }

    /**
     * 根据fileMd5与当前上传chunk大小更新已上传的文件大小
     * @param upLoadedSize
     */
    public void updateUploadedSize(Long upLoadedSize){
        RedisUtil.setString(UpLoadConstant.historyUpload + fileMd5,Convert.toStr(getUploadedSize()+upLoadedSize));
    }

    /**
     * 根据fileMd5获得当前上传块的在MultipartFile中的Index
     * @return
     */
    public Integer getCurrentChunk(){
        int chunk = 0;
        String chunkstr = RedisUtil.getString(UpLoadConstant.chunkCurr + fileMd5);
        if (!StrUtil.isEmpty(chunkstr)){
            chunk = Convert.toInt(chunkstr);
        }else{
            chunk = -1;
        }
        return chunk;
    }

    /**
     * 根据fileMd5设置当前上传块
     * @param chunk
     */
    public void setCurrentChunk(Integer chunk){
        RedisUtil.setString(UpLoadConstant.chunkCurr + fileMd5,Convert.toStr(chunk));
    }

    /**
     * 根据fileMd5设置当前上传块
     */
    public void incrCurrentChunk(){
        RedisUtil.incrBy(UpLoadConstant.chunkCurr + fileMd5,1);
    }

    /**
     * 根据文件md5获得文件上传路径
     * @return
     */
    public String getNoGroupPath(){
        String noGroupPath = RedisUtil.getString(UpLoadConstant.fastDfsPath + fileMd5);
        return noGroupPath;
    }

    /**
     * 根据md5设置文件上传路径
     * @param noGroupPath
     */
    public void setNoGroupPath(String noGroupPath){
        RedisUtil.setString(UpLoadConstant.fastDfsPath + fileMd5,noGroupPath);
    }

    /**
     * 根据文件md5获得文件上传路径
     * @return
     */
    public String getGroupPath(){
        String groupPath = RedisUtil.getString(UpLoadConstant.fastGroupPath + fileMd5);
        return groupPath;
    }

    /**
     * 根据md5设置文件上传路径
     * @param groupPath
     */
    public void setGroupPath(String groupPath){
        RedisUtil.setString(UpLoadConstant.fastGroupPath + fileMd5,groupPath);
    }

    /**
     * 更新已经上传的文件大小
     */
    public void updateUploadedSize(){
        this.setUploadedSize(chunkSize);
    }

    /**
     * 初始化上传File在redis中的信息
     */
    public void initFileRedisInfo(String groupPath,String noGroupPath){
        this.setGroupPath(groupPath);
        this.setNoGroupPath(noGroupPath);
//        this.setUploadedSize(getCurrentChunk()+getChunkSize());
    }

    /**
     * 当上传完一个分片之后
     *      更新当前应当上传的分片 CurrentChunk
     *      更新当前已经上传文件的大小 UploadedSize
     */
    public void afterChunkUploading(){
        this.incrCurrentChunk();
        this.updateUploadedSize();
    }




    /**
     * 完成上传
     *      将完成上传文件的信息记录到Redis中
     */
    public void upLoadFinished(){
        FileResult fileResult = new FileResult();
        fileResult.setMd5(fileMd5);
        fileResult.setName(fileName);
        fileResult.setLenght(fileSize);
        //默认group
        fileResult.setUrl(getGroupPath()+"/"+getNoGroupPath());
        //redis完成列表信息加入redis中（Uploading:+completedList）
        RedisUtil.rpush(UpLoadConstant.completedList, JSONUtil.toJsonStr(fileResult));
        //清除redis中上传信息
        RedisUtil.delKeys(new String[]{UpLoadConstant.chunkCurr+fileMd5,
                UpLoadConstant.fastDfsPath+fileMd5,
                UpLoadConstant.fastGroupPath+fileMd5,
                UpLoadConstant.historyUpload+fileMd5,
        });
    }

    /**
     * 报错时清空redis中历史信息
     */
    public void clearHistory(){
        RedisUtil.delKeys(new String[]{UpLoadConstant.chunkCurr+fileMd5,
                UpLoadConstant.fastDfsPath+fileMd5,
                UpLoadConstant.fastGroupPath+fileMd5
        });
    }

    public static ApiResult isCompleted(String fileMd5){
        CheckFileResult checkFileResult = new CheckFileResult();
        //模拟从mysql中查询文件表的md5,这里从redis里查询
        List<String> fileList = RedisUtil.getListAll(UpLoadConstant.completedList);
        if (CollUtil.isNotEmpty(fileList)){
            for (String e:fileList){
                JSONObject obj=JSONUtil.parseObj(e);
                if (obj.get("md5").equals(fileMd5)){
                    checkFileResult.setTotalSize(obj.getLong("lenght"));
                    checkFileResult.setViewPath(obj.getStr("url"));
                    return ApiResult.success(checkFileResult);
                }
            }
        }
        return ApiResult.fail();
    }

    private String fileName;
    private String fileMd5;    //文件的MD5码
    private Integer chunks;
    private Integer chunk;
    private Long fileSize;
    private Long chunkSize;    //当前上传块的大小（字节）

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
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



    public FileRedisUtil(String fileMd5, String fileName, Long fileSize, Integer chunks, Integer chunk,Long chunkSize) {
        this.fileMd5 = fileMd5;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.chunks = chunks;
        this.chunk = chunk;
        this.chunkSize = chunkSize;
    }
}
