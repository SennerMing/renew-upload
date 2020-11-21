package com.zw.renewupload.task;

import cn.hutool.core.io.FileUtil;
import com.github.tobato.fastdfs.domain.StorePath;
import com.zw.renewupload.append.DefectiveAppendFileStorageClient;
import com.zw.renewupload.append.FileRedisUtil;
import com.zw.renewupload.controller.ChunkUpload;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

//启用log 在控制台输出信息
@Slf4j
//启用@Component 注解将该类注入到spring容器中
@Component
public class TaskExecutor {
    protected Logger _logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private DefectiveAppendFileStorageClient defectiveClient;
    //为Hello类方法设置异步调用的配置类
    @Async("taskExecutor")
    public void run(FileRedisUtil fileRedisUtil) throws InterruptedException {

        MultipartFile file = ChunkUpload.md5_filestrem_map.get(fileRedisUtil.getFileMd5())[fileRedisUtil.getChunk()];
        try {
            String groupPath = "";
            String noGroupPath = "";
            String fullPath = "";
            if(fileRedisUtil.getCurrentChunk() == -1){
                StorePath path = defectiveClient.uploadAppenderFile(file.getInputStream(),
                        file.getSize(), FileUtil.extName(fileRedisUtil.getFileName()));
                groupPath = path.getGroup();
                noGroupPath = path.getPath();
                fullPath = groupPath + noGroupPath;
                _logger.info("path.getFullPath():"+path.getFullPath());
                fileRedisUtil.initFileRedisInfo(groupPath,noGroupPath);
            }else{
                defectiveClient.modifyFile(fileRedisUtil.getGroupPath(),fileRedisUtil.getNoGroupPath(), file.getInputStream(),
                        file.getSize(),fileRedisUtil.getUploadedSize());
            }

            fileRedisUtil.afterChunkUploading();

        } catch (IOException e) {
            _logger.error(e.getMessage());
        }

    }
}