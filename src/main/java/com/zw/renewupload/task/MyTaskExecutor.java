package com.zw.renewupload.task;

import cn.hutool.core.io.FileUtil;
import com.github.tobato.fastdfs.domain.StorePath;
import com.zw.renewupload.append.DefectiveAppendFileStorageClient;
import com.zw.renewupload.append.FileRedisUtil;
import com.zw.renewupload.common.ApiResult;
import com.zw.renewupload.common.CheckFileResult;
import com.zw.renewupload.common.FileResult;
import com.zw.renewupload.controller.ChunkUpload;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.authserver.AuthorizationServerProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Future;

/**
 * @author LXR
 * @className TaskConfiguration
 * @date 2020年11月21日10点33分
 * @description
 */
//启用log 在控制台输出信息
@Slf4j
//启用@Component 注解将该类注入到spring容器中
@Component
public class MyTaskExecutor {

    @Autowired
    private DefectiveAppendFileStorageClient defectiveClient;

    /**
     * 处理缓存好的文件流
     * @see ChunkUpload#md5_chunkpath_map
     * @param fileRedisUtil
     * @throws InterruptedException
     */
    @Async
    public Future<CheckFileResult> run(FileRedisUtil fileRedisUtil,Long timeout) throws InterruptedException {
        /**
         *
         * 如果获取到的当前块是-1（说明Redis还未做初始化工作）
         *   是则将当前块设为0
         *   否则更新FileRedisUtil当前块大小
         * 判断需要上传的[chunk]是否准备完毕
         *  如果准备就绪，则进行[MultipartFile]的写入
         *     -- 如果是第一次写入，则要进行Redis与FastDFS的初始化工作
         *         [StorePath = uploadAppenderFile()]
         *         [initFileRedisInfo()]
         *              --初始化文件上传在Redis中路径：groupPath与noGroupPath
         *     -- 如果不是第一次写入，则调用[modifyFile()]
         *  [chunk]块写入完成后，进行[CurrentChunk+1]与[UploadFileSize+MultipartFile.size()]的更新操作
         *
         *  最后判断整个文件是否上传完毕
         *      如果上传完毕则，将Redis中所有缓存记录包括(文件地址：groupPath、noGroupPath,文件上传历史大小：historyUpLoadSize,
         *      当前上传[chunk])，并将文件信息添加到上传完成列表中。
         *      //如果上传还未结束，则将Redis中的上述上传记录更新，并进入下一次轮询。
         *
         *  如果未准备就绪，则继续进入下一次循环进行等待。
         */

        String[] multipartFiles = ChunkUpload.md5_chunkpath_map.get(fileRedisUtil.getFileMd5());
        String filepath = null;
        int currentChunk = 0;
        FileInputStream fis = null;
        //开始时间
        Long totaltime = 0l;
        Instant stime = null;
        CheckFileResult checkFileResult = null;
        int count = 0;

        while(true){
            //获取当前需要上传块的索引
            if(fileRedisUtil.getCurrentChunk() == -1){
                fileRedisUtil.setCurrentChunk(0);
            }else{
                currentChunk = fileRedisUtil.getCurrentChunk();
            }
            //判断当前需要上传块是否已经缓存
            if(currentChunk >= multipartFiles.length)
            {
                break;
            }
            if(multipartFiles[currentChunk] == null){
                //该上传的流获取不到就开始计时
                if(stime == null){
                    stime = java.time.Instant.now();
                }else{
                    //获取计时开始距现在时间(毫秒)
                    totaltime = Duration.between(stime, java.time.Instant.now()).toMillis();
                    //如果总时间大于超时时间的限制，则线程挂起
                    if(totaltime > timeout){
                        //返回挂起信息（假的挂起操作，线程的不太会，有待提高）。
                        System.out.println("执行线程挂起操作,数据上传停在了第:"+currentChunk+"块");
                        checkFileResult = new CheckFileResult(fileRedisUtil.getFileName(),fileRedisUtil.getFileMd5(), 2,
                                fileRedisUtil.getChunks(), null, currentChunk,
                                fileRedisUtil.getFileSize(), "");
                        break;
                    }
                }
                continue;
            }else{
                totaltime = 0l;
                stime = null;

                filepath = multipartFiles[currentChunk];
                try {
                    fis = new FileInputStream(filepath);
                    fileRedisUtil.setChunkSize(fis.getChannel().size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(fileRedisUtil.getCurrentChunk() == 0){
                String groupPath = "";
                String noGroupPath = "";
                StorePath path = defectiveClient.uploadAppenderFile(fis,
                        fileRedisUtil.getChunkSize(), FileUtil.extName(fileRedisUtil.getFileName()));
                groupPath = path.getGroup();
                noGroupPath = path.getPath();
                fileRedisUtil.initFileRedisInfo(groupPath,noGroupPath);
                log.info("第一次将"+filepath+"写入FastDFS："+(count++)+", 文件大小："+fileRedisUtil.getChunkSize()+
                        ", groupPath:"+groupPath+", noGroupPath:"+noGroupPath);
            }else{
                log.info("非第一次将"+filepath+"写入FastDFS："+(count++)+", 文件大小："+fileRedisUtil.getChunkSize()+
                        ", groupPath:"+fileRedisUtil.getGroupPath()+", noGroupPath:"+fileRedisUtil.getNoGroupPath()
                        +", 追加地址："+fileRedisUtil.getUploadedSize());
                defectiveClient.modifyFile(fileRedisUtil.getGroupPath(),fileRedisUtil.getNoGroupPath(), fis,
                        fileRedisUtil.getChunkSize(),fileRedisUtil.getUploadedSize());
            }
            //上传分片结束之后，更新Redis中的上传记录，并释放缓存[md5_filestream_map] 1,697,906,688
            multipartFiles[currentChunk] = null;

            fileRedisUtil.afterChunkUploading();
            try {
                fis.close();
                File useLessFile = new File(filepath);
                useLessFile.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            log.info("CurrentChunk:"+fileRedisUtil.getCurrentChunk()+", Chunks:"+fileRedisUtil.getChunks());

            int current = fileRedisUtil.getCurrentChunk();
            int chunks = fileRedisUtil.getChunks();
            if(current == chunks){
                fileRedisUtil.upLoadFinished();
                ApiResult checkResult = FileRedisUtil.isCompleted(fileRedisUtil.getFileMd5());
                checkFileResult = (CheckFileResult)checkResult.getData();
                log.info("[文件名："+fileRedisUtil.getFileName() +" ,MD5:"+fileRedisUtil.getFileMd5()+", FastDFS地址："+checkFileResult.getViewPath()+"]");
                ChunkUpload.md5_chunkpath_map.remove(fileRedisUtil.getFileMd5());
                break;
            }
        }
        ChunkUpload.md5_asyncresult_map.remove(fileRedisUtil.getFileMd5());
        return new AsyncResult<CheckFileResult>(checkFileResult);
    }






}