package com.zw.renewupload.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zw.renewupload.common.FileResult;
import com.zw.renewupload.common.RedisUtil;
import com.zw.renewupload.common.UpLoadConstant;
import com.zw.renewupload.entity.UploadChunk;

/**
 * @ClassName: UpLoadRedisUtils
 * @Description: TODO
 * @Author: LXR
 * @DATE: 2020/12/1 14:46
 * @Version: 1.0
 */
public class UpLoadRedisUtils {

    private String fileMd5;

    public UpLoadRedisUtils() {
    }

    public UpLoadRedisUtils(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    //根据FileMD5，在Redis中记录文件上传的GroupPath
    public void setGroupPath(String groupPath){
        RedisUtil.setString(UpLoadConstant.fastGroupPath + fileMd5,groupPath);
    }

    //根据FileMD5，获得在Redis中记录文件上传的GroupPath
    public String getGroupPath(){
        String groupPath = RedisUtil.getString(UpLoadConstant.fastGroupPath + fileMd5);
        return groupPath;
    }

    //根据FileMD5，清除Redis中记录的GroupPath
    public void delGroupPath(){
        RedisUtil.delKey(UpLoadConstant.fastGroupPath+fileMd5);
    }

    //根据FileMD5，在Redis中记录文件上传的DistrictPath
    public void setDistrictPath(String districtPath){
        RedisUtil.setString(UpLoadConstant.fastDfsPath + fileMd5,districtPath);
    }

    //根据fileMD5，获得在Redis中记录文件上传的DistrictPath
    public String getDistrictPath(){
        String districtPath = RedisUtil.getString(UpLoadConstant.fastDfsPath + fileMd5);
        return districtPath;
    }

    //根据FileMD5，清除Redis中记录的GroupPath
    public void delDistrictPath(){
        RedisUtil.delKey(UpLoadConstant.fastDfsPath+fileMd5);
    }

    //根据FileMD5，获得当前已经上传的chunk的总大小
    public Long getUploadedSize(){
        Long uploadedSize = 0L;
        String uploadSizeStr = RedisUtil.getString(UpLoadConstant.historyUpload + fileMd5);
        if (StrUtil.isNotEmpty(uploadSizeStr)){
            uploadedSize = Convert.toLong(uploadSizeStr);
        }
        return uploadedSize;
    }

    //根据FileMD5，设置当前已经上传chunk的总大小
    public void appendUploadedSize(Long chunkSize){
        Long uploadedSize = getUploadedSize() + chunkSize;
        RedisUtil.setString(UpLoadConstant.historyUpload + fileMd5,Convert.toStr(uploadedSize));
    }

    //根据FileMD5，清除Redis中记录的UploadedSize
    public void delUploadedSize(){
        RedisUtil.delKey(UpLoadConstant.historyUpload+fileMd5);
    }

    //根据FileMD5，获得当前需要上传的块的Index
    public Integer getCurrentChunk(){
        int chunk = 0;
        String chunkstr = RedisUtil.getString(UpLoadConstant.chunkCurr + fileMd5);
        if (!StrUtil.isEmpty(chunkstr)){
            chunk = Convert.toInt(chunkstr);
        }
        return chunk;
    }

    //根据FileMD5，获得当前需要上传的块的Index
    public static Integer getCurrentChunk(String fileMd5){
        int chunk = 0;
        String chunkstr = RedisUtil.getString(UpLoadConstant.chunkCurr + fileMd5);
        if (!StrUtil.isEmpty(chunkstr)){
            chunk = Convert.toInt(chunkstr);
        }
        return chunk;
    }

    //根据FileMD5，设置当前需要上传的块的Index
    public void setCurrentChunk(Integer chunk){
        RedisUtil.setString(UpLoadConstant.chunkCurr + fileMd5,Convert.toStr(chunk));
    }

    //根据FileMD5，清除Redis中记录的UploadedSize
    public void delCurrentChunk(){
        RedisUtil.delKey(UpLoadConstant.chunkCurr+fileMd5);
    }


    //根据FileMD5,清除所有的记录信息
    public void clearAllRecord(){
//        this.delCurrentChunk();
//        this.delUploadedSize();
//        this.delGroupPath();
//        this.delDistrictPath();
        //清除redis中上传信息
        RedisUtil.delKeys(new String[]{UpLoadConstant.chunkCurr+fileMd5,
                UpLoadConstant.fastGroupPath+fileMd5,
                UpLoadConstant.fastDfsPath+fileMd5,
                UpLoadConstant.historyUpload+fileMd5,
        });
    }

    //将上传完成信息添加到已完成列表中
    public void addToCompletedList(UploadChunk uploadChunk){
        //redis完成列表信息加入redis中（Uploading:+completedList）
        RedisUtil.rpush(UpLoadConstant.completedList, JSONUtil.toJsonStr(uploadChunk));
    }

}
