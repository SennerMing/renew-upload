package com.zw.renewupload.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.github.tobato.fastdfs.domain.StorePath;
import com.zw.renewupload.append.DefectiveAppendFileStorageClient;
import com.zw.renewupload.entity.UploadChunk;
import com.zw.renewupload.utils.UpLoadRedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.concurrent.Future;

/**
 * @ClassName: UploadChunkExcutor
 * @Description: TODO
 * @Author: LXR
 * @DATE: 2020/12/1 11:11
 * @Version: 1.0
 */
@Slf4j
@Component
public class UploadChunkExcutor {

    @Autowired
    private DefectiveAppendFileStorageClient defectiveClient;

    /**
     * v1.3异步线程主要的处理逻辑
     * @param uploadChunk
     * @return
     * @throws InterruptedException
     */
    @Async
    public Future<UploadChunk> dealWith(UploadChunk uploadChunk) throws InterruptedException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(uploadChunk.getChunkTmpPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //轮询判断当前线程的chunk是否为当前可以写入的块
        int threadChunk = uploadChunk.getChunk();
        while(true){

            boolean isAppended = false;
            int needToAppendChunk = UpLoadRedisUtils.getCurrentChunk(uploadChunk.getFileMd5());
            if(threadChunk == needToAppendChunk){
                synchronized (this){        //存在多个线程在等待的情况，对写入与修改操作加锁。
                    isAppended = deliverToFastDFS(uploadChunk,fis);
                }
            }else{
                if(Thread.currentThread().getPriority() == 5){
                    int degree = uploadChunk.getChunks()/10;
                    int priority = 10-(uploadChunk.getChunk()/degree);
                    Thread.currentThread().setPriority(priority==0?(priority+1):priority);
                }else{
                    Thread.sleep(200);
                }
            }
            if(isAppended){ //判断是否写入成功
                delUploadedFile(fis, uploadChunk.getChunkTmpPath());
                uploadChunk.setStatus(1);
                break;
            }
        }
        return new AsyncResult<>(uploadChunk);
    }

    /**
     * 对需要上传的块进行处理，上传完成后会更新Redis中的数据
     * @param uploadChunk
     * @param fis
     * @return
     */
    private Boolean deliverToFastDFS(UploadChunk uploadChunk, InputStream fis){
        log.info("进入写入方法！");
        Boolean isAppended = false;
        //如果是第一块
        //  1.将上传的块写入FastDFS中
        //  2.记录 [GroupPath:上传的文件存放在FastDFS的哪个group中]
        //  3.记录 [DistrictPath:上传的文件在FastDFS的区域的路径]
        //不是第一次上传
        //  1.从Redis中获得该文件的GroupPath与DistrictPath
        //  2.追加写入当前块
        LocalDateTime startTime = LocalDateTime.now();
        UpLoadRedisUtils upLoadRedisUtils = new UpLoadRedisUtils(uploadChunk.getFileMd5());
        if(uploadChunk.getChunk().equals(0)){
            StorePath path = defectiveClient.uploadAppenderFile(fis,uploadChunk.getChunkSize(),
                    FileUtil.extName(uploadChunk.getFileName()));

            upLoadRedisUtils.setGroupPath(path.getGroup());
            upLoadRedisUtils.setDistrictPath(path.getPath());

        }else{
            defectiveClient.modifyFile(upLoadRedisUtils.getGroupPath(),upLoadRedisUtils.getDistrictPath(), fis,
                    uploadChunk.getChunkSize(),upLoadRedisUtils.getUploadedSize());
        }
        java.time.Duration duration = java.time.Duration.between(startTime,LocalDateTime.now());
        log.info("写入执行时间："+duration);

        //如果上传完成
        //  1.清除redis中的[CurrentChunk:当前需要写入的块的Index]、[UploadedSize:当前已经上传块的总大小]、
        //  [GroupPath:上传的文件存放在FastDFS的哪个group中]、[DistrictPath:上传的文件在FastDFS的区域的路径]
        //  2.将上传完成的信息，存入redis包括：[fileName:文件名称]、[fileSize:文件大小]、[fileSize:文件大小]、[fileMd5:文件的MD5]
        //  以及[viewPath:文件在FastDFS中的位置]。
        //未上传完成：
        //  1.更新 [CurrentChunk:当前需要写入的块的Index]
        //  2.记录 [UploadedSize:当前已经上传块的总大小]
        isAppended = true;
        if(uploadChunk.getChunk().equals(uploadChunk.getChunks()-1)){
            uploadChunk.setFilePath(upLoadRedisUtils.getGroupPath()+"/"+upLoadRedisUtils.getDistrictPath());
            upLoadRedisUtils.addToCompletedList(uploadChunk);
            upLoadRedisUtils.clearAllRecord();
            log.info(JSONUtil.toJsonStr(uploadChunk));
        }else{
            upLoadRedisUtils.setCurrentChunk(uploadChunk.getChunk()+1);
            upLoadRedisUtils.appendUploadedSize(uploadChunk.getChunkSize());
        }
        log.info("已上传文件：["+uploadChunk.getFileName()+"]的第：["+uploadChunk.getChunk()+"]块!");
        return isAppended;
    }

    /**
     * 关闭文件流并删除已经上传过的文件
     * @param fis
     * @param tmpChunkPath
     */
    private void delUploadedFile(InputStream fis,String tmpChunkPath){
        try {
            fis.close();
            File useLessFile = new File(tmpChunkPath);
            useLessFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
