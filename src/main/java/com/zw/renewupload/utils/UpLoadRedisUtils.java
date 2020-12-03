package com.zw.renewupload.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zw.renewupload.common.FileResult;
import com.zw.renewupload.common.RedisUtil;
import com.zw.renewupload.common.UpLoadConstant;
import com.zw.renewupload.entity.UploadChunk;
import org.apache.tomcat.util.http.fileupload.UploadContext;

import java.util.ArrayList;
import java.util.List;

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
                UpLoadConstant.cached+fileMd5
        });
    }

    //将上传完成信息添加到已完成列表中
    public void addToCompletedList(UploadChunk uploadChunk){
        //redis完成列表信息加入redis中（Uploading:+completedList）
        RedisUtil.rpush(UpLoadConstant.completedList, JSONUtil.toJsonStr(uploadChunk));
    }


    /**
     * 检查文件是否上传完毕，如果上传完毕则返回完成上传的文件信息，统一返回格式
     * @param fileMd5
     * @return
     */
    public static UploadChunk isCompleted(String fileMd5){
        UploadChunk uploadChunk = new UploadChunk();
        uploadChunk.setStatus(0);  //默认为0，表示没有完成
        List<String> completedList = RedisUtil.getListAll(UpLoadConstant.completedList);
        if (CollUtil.isNotEmpty(completedList)){
            for (String completedObjStr: completedList){
                JSONObject completedObj = JSONUtil.parseObj(completedObjStr);
                if(completedObj.getStr("fileMd5").equals(fileMd5)){
                    uploadChunk = JSONUtil.toBean(completedObj, uploadChunk.getClass());
                    uploadChunk.setStatus(1);
                }
            }
        }
        return uploadChunk;
    }

    //=================================================================================
    //                      下面是将缓存的文件记录到Redis中
    //=================================================================================

    /**
     * 将已经转存到Tmp目录的当前块信息，添加到已上传列表
     * @param uploadChunk
     */
    public static void addToCachedList(UploadChunk uploadChunk){
        RedisUtil.rpush(UpLoadConstant.cached +uploadChunk.getFileMd5(),JSONUtil.toJsonStr(uploadChunk));
    }

    /**
     * 判断当前块信息是否已经缓存
     * @param uploadChunk
     * @return
     */
    public static boolean hasCached(UploadChunk uploadChunk){
        boolean result = false;
        List<String> cachedList = RedisUtil.getListAll(UpLoadConstant.cached+uploadChunk.getFileMd5());
        if (CollUtil.isNotEmpty(cachedList)){
            for (String cachedChunkStr: cachedList){
                JSONObject cachedChunk = JSONUtil.parseObj(cachedChunkStr);
                if(cachedChunk.getInt("chunk").equals(uploadChunk.getChunk())){
                    result = true;
                }
            }
        }
        return result;
    }

    public static void cacheTheChunk(UploadChunk uploadChunk){
        boolean result = false;
        result = hasCached(uploadChunk);
        if(!result){
           UpLoadRedisUtils.addToCachedList(uploadChunk);
        }
    }


    /**
     * 获得已经上传过的信息列表
     * @param fileMd5
     * @return
     */
    public static List<UploadChunk> getCachedInfo(String fileMd5){
        List<UploadChunk> cachedList = new ArrayList<>();
        List<String> cachedStrList = RedisUtil.getListAll(UpLoadConstant.cached+fileMd5);
        if (CollUtil.isNotEmpty(cachedStrList)){
            for (String cachedChunkStr: cachedStrList){
                cachedList.add(JSONUtil.toBean(cachedChunkStr, UploadChunk.class));
            }
        }
        return cachedList;
    }

    /**
     * 获得已经上传过的信息列表
     * @param fileMd5
     * @return
     */
    public static Integer[] getCachedIndexs(String fileMd5){
        List<Integer> cachedList = new ArrayList<>();
        List<String> cachedStrList = RedisUtil.getListAll(UpLoadConstant.cached+fileMd5);
        if (CollUtil.isNotEmpty(cachedStrList)){
            for (String cachedChunkStr: cachedStrList){
                JSONObject cachedChunkObj = JSONUtil.parseObj(cachedChunkStr);
                int chunkIndex = cachedChunkObj.getInt("chunk");
                cachedList.add(chunkIndex);
            }
        }
        Integer[] result = new Integer[cachedList.size()];
        cachedList.toArray(result);
        return result;
    }



}
