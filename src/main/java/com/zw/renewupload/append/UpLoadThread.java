package com.zw.renewupload.append;

import cn.hutool.core.io.FileUtil;
import com.github.tobato.fastdfs.domain.StorePath;
import com.zw.renewupload.common.ApiResult;
import com.zw.renewupload.controller.ChunkUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @ClassName: UpLoadThread
 * @Description: TODO
 * @Author: LXR
 * @DATE: 2020/11/20 21:34
 * @Version: 1.0
 */
public class UpLoadThread extends Thread{

    protected Logger _logger = LoggerFactory.getLogger(this.getClass());
    /**
     * 上传分片主要操作
     * @param fileRedisUtil
     * @return
     */
    private void upLoadChunk(FileRedisUtil fileRedisUtil){

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

    public UpLoadThread(FileRedisUtil fileRedisUtil){
        this.fileRedisUtil = fileRedisUtil;
    }

    private FileRedisUtil fileRedisUtil;
    private DefectiveAppendFileStorageClient defectiveClient;

    public FileRedisUtil getFileRedisUtil() {
        return fileRedisUtil;
    }

    public void setFileRedisUtil(FileRedisUtil fileRedisUtil,DefectiveAppendFileStorageClient defectiveClient) {
        this.fileRedisUtil = fileRedisUtil;
        this.defectiveClient = defectiveClient;
    }
}
