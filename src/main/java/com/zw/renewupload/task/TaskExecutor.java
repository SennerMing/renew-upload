package com.zw.renewupload.task;

import cn.hutool.core.io.FileUtil;
import com.github.tobato.fastdfs.domain.StorePath;
import com.zw.renewupload.append.DefectiveAppendFileStorageClient;
import com.zw.renewupload.append.FileRedisUtil;
import com.zw.renewupload.common.ApiResult;
import com.zw.renewupload.common.CheckFileResult;
import com.zw.renewupload.controller.ChunkUpload;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
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
public class TaskExecutor {

    @Autowired
    private DefectiveAppendFileStorageClient defectiveClient;

//    @Async("taskExecutor")
//    public void run(FileRedisUtil fileRedisUtil) throws InterruptedException {
//        MultipartFile file = ChunkUpload.md5_filestrem_map.get(fileRedisUtil.getFileMd5())[fileRedisUtil.getChunk()];
//        try {
//            String groupPath = "";
//            String noGroupPath = "";
//            String fullPath = "";
//            if(fileRedisUtil.getCurrentChunk() == -1){
//                StorePath path = defectiveClient.uploadAppenderFile(file.getInputStream(),
//                        file.getSize(), FileUtil.extName(fileRedisUtil.getFileName()));
//                groupPath = path.getGroup();
//                noGroupPath = path.getPath();
//                fullPath = groupPath + noGroupPath;
//                log.info("path.getFullPath():"+path.getFullPath());
//                fileRedisUtil.initFileRedisInfo(groupPath,noGroupPath);
//            }else{
//                defectiveClient.modifyFile(fileRedisUtil.getGroupPath(),fileRedisUtil.getNoGroupPath(), file.getInputStream(),
//                        file.getSize(),fileRedisUtil.getUploadedSize());
//            }
//            fileRedisUtil.afterChunkUploading();
//
//
//        } catch (IOException e) {
//            log.error(e.getMessage());
//        }
//    }

    /**
     * 处理缓存的MultipartFile[]
     * @see ChunkUpload#md5_filestrem_map
     * @param fileRedisUtil
     * @throws InterruptedException
     */
//    @Async("taskExecutor")
//    public void run(FileRedisUtil fileRedisUtil) throws InterruptedException {
//        /**
//         *
//         * 如果获取到的当前块是-1（说明Redis还未做初始化工作）
//         *   是则将当前块设为0
//         *   否则更新FileRedisUtil当前块大小
//         * 判断需要上传的[chunk]是否准备完毕
//         *  如果准备就绪，则进行[MultipartFile]的写入
//         *     -- 如果是第一次写入，则要进行Redis与FastDFS的初始化工作
//         *         [StorePath = uploadAppenderFile()]
//         *         [initFileRedisInfo()]
//         *              --初始化文件上传在Redis中路径：groupPath与noGroupPath
//         *     -- 如果不是第一次写入，则调用[modifyFile()]
//         *  [chunk]块写入完成后，进行[CurrentChunk+1]与[UploadFileSize+MultipartFile.size()]的更新操作
//         *
//         *  最后判断整个文件是否上传完毕
//         *      如果上传完毕则，将Redis中所有缓存记录包括(文件地址：groupPath、noGroupPath,文件上传历史大小：historyUpLoadSize,
//         *      当前上传[chunk])，并将文件信息添加到上传完成列表中。
//         *      //如果上传还未结束，则将Redis中的上述上传记录更新，并进入下一次轮询。
//         *
//         *  如果未准备就绪，则继续进入下一次循环进行等待。
//         */
//        MultipartFile[] multipartFiles = ChunkUpload.md5_filestrem_map.get(fileRedisUtil.getFileMd5());
//        MultipartFile file = null;
//        int currentChunk = 0;
//        while(true){
//            //获取当前需要上传块的索引
//            if(fileRedisUtil.getCurrentChunk() == -1){
//                fileRedisUtil.setCurrentChunk(0);
//            }else{
//                currentChunk = fileRedisUtil.getCurrentChunk();
//            }
//            //判断当前需要上传块是否已经缓存
//            if(multipartFiles[currentChunk] == null){
//                continue;
//            }else{
//                file = multipartFiles[currentChunk];
//                fileRedisUtil.setChunkSize(file.getSize());
//            }
//            try {
//                String groupPath = "";
//                String noGroupPath = "";
//                String fullPath = "";
//
//                if(fileRedisUtil.getCurrentChunk() == 0){
//                    StorePath path = defectiveClient.uploadAppenderFile(file.getInputStream(),
//                            file.getSize(), FileUtil.extName(fileRedisUtil.getFileName()));
//                    groupPath = path.getGroup();
//                    noGroupPath = path.getPath();
//                    fullPath = groupPath + noGroupPath;
//                    log.info("path.getFullPath():"+path.getFullPath());
//                    fileRedisUtil.initFileRedisInfo(groupPath,noGroupPath);
//                }else{
//                    defectiveClient.modifyFile(fileRedisUtil.getGroupPath(),fileRedisUtil.getNoGroupPath(), file.getInputStream(),
//                            file.getSize(),fileRedisUtil.getUploadedSize());
//                }
//
//                //上传分片结束之后，更新Redis中的上传记录，并释放缓存[md5_filestream_map]
//                fileRedisUtil.afterChunkUploading();
//                multipartFiles[currentChunk] = null;
//            } catch (IOException e) {
//                fileRedisUtil.clearHistory();
//                log.error(e.getMessage());
//            }
//
//            if(fileRedisUtil.getCurrentChunk() == fileRedisUtil.getChunks()){
//                fileRedisUtil.upLoadFinished();
//                break;
//            }
//        }
//    }


//    /**
//     * 处理缓存的MultipartFile[]
//     * @see ChunkUpload#md5_filestrem_map
//     * @param fileRedisUtil
//     * @throws InterruptedException
//     */
//    @Async("taskExecutor")
//    public Future<CheckFileResult> run(FileRedisUtil fileRedisUtil) throws InterruptedException {
//        /**
//         *
//         * 如果获取到的当前块是-1（说明Redis还未做初始化工作）
//         *   是则将当前块设为0
//         *   否则更新FileRedisUtil当前块大小
//         * 判断需要上传的[chunk]是否准备完毕
//         *  如果准备就绪，则进行[MultipartFile]的写入
//         *     -- 如果是第一次写入，则要进行Redis与FastDFS的初始化工作
//         *         [StorePath = uploadAppenderFile()]
//         *         [initFileRedisInfo()]
//         *              --初始化文件上传在Redis中路径：groupPath与noGroupPath
//         *     -- 如果不是第一次写入，则调用[modifyFile()]
//         *  [chunk]块写入完成后，进行[CurrentChunk+1]与[UploadFileSize+MultipartFile.size()]的更新操作
//         *
//         *  最后判断整个文件是否上传完毕
//         *      如果上传完毕则，将Redis中所有缓存记录包括(文件地址：groupPath、noGroupPath,文件上传历史大小：historyUpLoadSize,
//         *      当前上传[chunk])，并将文件信息添加到上传完成列表中。
//         *      //如果上传还未结束，则将Redis中的上述上传记录更新，并进入下一次轮询。
//         *
//         *  如果未准备就绪，则继续进入下一次循环进行等待。
//         */
//        MultipartFile[] multipartFiles = ChunkUpload.md5_filestrem_map.get(fileRedisUtil.getFileMd5());
//        MultipartFile file = null;
//        int currentChunk = 0;
//        while(true){
//            //获取当前需要上传块的索引
//            if(fileRedisUtil.getCurrentChunk() == -1){
//                fileRedisUtil.setCurrentChunk(0);
//            }else{
//                currentChunk = fileRedisUtil.getCurrentChunk();
//            }
//            //判断当前需要上传块是否已经缓存
//            if(multipartFiles[currentChunk] == null){
//                continue;
//            }else{
//                file = multipartFiles[currentChunk];
//                fileRedisUtil.setChunkSize(file.getSize());
//            }
//            try {
//                String groupPath = "";
//                String noGroupPath = "";
//                String fullPath = "";
//
//                if(fileRedisUtil.getCurrentChunk() == 0){
//                    StorePath path = defectiveClient.uploadAppenderFile(file.getInputStream(),
//                            file.getSize(), FileUtil.extName(fileRedisUtil.getFileName()));
//                    groupPath = path.getGroup();
//                    noGroupPath = path.getPath();
//                    fullPath = groupPath + noGroupPath;
//                    log.info("path.getFullPath():"+path.getFullPath());
//                    fileRedisUtil.initFileRedisInfo(groupPath,noGroupPath);
//                }else{
//                    defectiveClient.modifyFile(fileRedisUtil.getGroupPath(),fileRedisUtil.getNoGroupPath(), file.getInputStream(),
//                            file.getSize(),fileRedisUtil.getUploadedSize());
//                }
//
//                //上传分片结束之后，更新Redis中的上传记录，并释放缓存[md5_filestream_map]
//                fileRedisUtil.afterChunkUploading();
//                multipartFiles[currentChunk] = null;
//            } catch (IOException e) {
//                fileRedisUtil.clearHistory();
//                log.error(e.getMessage());
//            }
//
//            if(fileRedisUtil.getCurrentChunk() == fileRedisUtil.getChunks()){
//                fileRedisUtil.upLoadFinished();
//                break;
//            }
//        }
//        ApiResult checkResult = FileRedisUtil.isCompleted(fileRedisUtil.getFileMd5());
//        CheckFileResult chekFileResult = (CheckFileResult)checkResult.getData();
//        return new AsyncResult<CheckFileResult>(chekFileResult);
//    }

    /**
     * 处理缓存的MultipartFile[]
     * @see ChunkUpload#md5_filestrem_map
     * @param fileRedisUtil
     * @throws InterruptedException
     */
    @Async("taskExecutor")
    public Future<CheckFileResult> run(FileRedisUtil fileRedisUtil) throws InterruptedException {
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
                return null;
            }
            if(multipartFiles[currentChunk] == null){
                continue;
            }else{
                filepath = multipartFiles[currentChunk];
                try {
                    fis = new FileInputStream(filepath);
                    FileChannel fc = fis.getChannel();
                    fileRedisUtil.setChunkSize(fc.size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
//            try {
                String groupPath = "";
                String noGroupPath = "";
                String fullPath = "";

                if(fileRedisUtil.getCurrentChunk() == 0){
                    StorePath path = defectiveClient.uploadAppenderFile(fis,
                            fileRedisUtil.getChunkSize(), FileUtil.extName(fileRedisUtil.getFileName()));
                    groupPath = path.getGroup();
                    noGroupPath = path.getPath();
                    fullPath = groupPath + noGroupPath;
                    log.info("path.getFullPath():"+path.getFullPath());
                    fileRedisUtil.initFileRedisInfo(groupPath,noGroupPath);
                }else{
                    defectiveClient.modifyFile(fileRedisUtil.getGroupPath(),fileRedisUtil.getNoGroupPath(), fis,
                            fileRedisUtil.getChunkSize(),fileRedisUtil.getUploadedSize());
                }

                //上传分片结束之后，更新Redis中的上传记录，并释放缓存[md5_filestream_map]
                fileRedisUtil.afterChunkUploading();
                multipartFiles[currentChunk] = null;
//            } catch (IOException e) {
//                fileRedisUtil.clearHistory();
//                log.error(e.getMessage());
//            }

            if(fileRedisUtil.getCurrentChunk() == fileRedisUtil.getChunks()){
                fileRedisUtil.upLoadFinished();
                break;
            }
        }
        ApiResult checkResult = FileRedisUtil.isCompleted(fileRedisUtil.getFileMd5());
        CheckFileResult chekFileResult = (CheckFileResult)checkResult.getData();
        return new AsyncResult<CheckFileResult>(chekFileResult);
    }

}