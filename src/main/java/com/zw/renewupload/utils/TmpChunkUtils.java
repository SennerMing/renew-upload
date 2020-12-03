package com.zw.renewupload.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @ClassName: TmpChunkUtils
 * @Description: TODO
 * @Author: LXR
 * @DATE: 2020/12/3 16:09
 * @Version: 1.0
 */
public class TmpChunkUtils {
    /**
     * Tomcat 获得临时文件转存路径
     * @param is
     * @param fileName
     * @return
     */
    public String tmpMdbFile(String tmpdir,InputStream is, String fileName) {
        File dest = new File(tmpdir + System.currentTimeMillis() + "_" + fileName);
        saveFile(is, dest);
        return dest.getAbsolutePath();
    }

    /**
     * 文件转存
     * @param is
     * @param dest
     */
    private void saveFile(InputStream is, File dest) {
        try(FileOutputStream fos = new FileOutputStream(dest)) {
            int len;
            byte[] buffer = new byte[1024];
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
