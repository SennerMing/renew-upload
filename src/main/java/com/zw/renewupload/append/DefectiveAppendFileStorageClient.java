package com.zw.renewupload.append;

import com.github.tobato.fastdfs.domain.StorageNode;
import com.github.tobato.fastdfs.domain.StorageNodeInfo;
import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.proto.storage.StorageModifyCommand;
import com.github.tobato.fastdfs.proto.storage.StorageUploadFileCommand;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.DefaultAppendFileStorageClient;
import com.github.tobato.fastdfs.service.DefaultGenerateStorageClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * @ClassName: DefectiveAppendFileStorageClient
 * @Description: TODO
 * @Author: LXR
 * @DATE: 2020/11/20 16:58
 * @Version: 1.0
 */
@Component
@Primary
public class DefectiveAppendFileStorageClient extends DefaultAppendFileStorageClient {

    public StorePath uploadAppenderFile(InputStream inputStream, long fileSize, String fileExtName) {
        StorageNode client = trackerClient.getStoreStorage();
        StorageUploadFileCommand command = new StorageUploadFileCommand(client.getStoreIndex(), inputStream,
                fileExtName, fileSize, true);
        return connectionManager.executeFdfsCmd(client.getInetSocketAddress(), command);
    }

}
