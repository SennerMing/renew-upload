package com.zw.renewupload.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.zw.renewupload.append.DefectiveAppendFileStorageClient;
import com.zw.renewupload.append.FileRedisUtil;
import com.zw.renewupload.common.*;
import com.zw.renewupload.entity.UploadChunk;
//import com.zw.renewupload.task.MyTaskExecutor;
import com.zw.renewupload.task.UploadChunkExcutor;
import com.zw.renewupload.utils.UpLoadRedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import sun.security.provider.MD5;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
@CrossOrigin
@Controller
@RequestMapping("/upload/chunkUpload")
@Scope(value = "request")
public class ChunkUpload {

    
    //获取配置
    /**
     * 用户在文件管理页面点击进入上传页面调用的接口，又获取了一边文件服务器地址
     * @return
     */
    @RequestMapping("/config")
    @ResponseBody
    public String config(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileServerUrl", "http:\\\\"+ReadProper.getResourceValue("fileServerUrl")+"/");
        jsonObject.put("maxSize", ReadProper.getResourceValue("maxSize"));
        return jsonObject.toString();
    }

    @PostMapping("/upload_do")
    public
    @ResponseBody
    ApiResult upload_do(@RequestParam Map<String, Object> paramMap, HttpServletRequest request) throws IOException {
        String noGroupPath;//存储在fastdfs不带组的路径
        String fileMd5= (String) paramMap.get("fileMd5");
        //继续上传刚删除的文件，要加锁来阻止其上传,否则会出现丢块问题
        String chunklockName=UpLoadConstant.chunkLock+fileMd5;

        String temOwner= RandomUtil.randomUUID();
        boolean currOwner=false;//真正的拥有者
        try {
//            String userName=  (String) request.getSession().getAttribute("name");
//            if (StrUtil.isEmpty(userName)){
//                return  ApiResult.fail("请先登录");
//            }

            if (!paramMap.containsKey("chunk")){
                paramMap.put("chunk","0");
            }

            if (!paramMap.containsKey("chunks")){
                paramMap.put("chunks","1");
            }
            //通过Uploading+lock:+chunkLock:+（前端传来的File MD5）+1，加指定值 ,key 不存在时候会设置 key,并认为原来的 value 是 0
            Long lock= RedisUtil.incrBy(chunklockName,1);
            if (lock>1){
                return ApiResult.fail("请求块锁失败");
            }

            //到这，说明之前没有使用过这个chunklockName上传过文件，并写入锁的当前拥有者
            currOwner=true;
            //开始接收前端上传的文件
            List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
            MultipartFile file = null;
            BufferedOutputStream stream = null;

            String chunk= (String) paramMap.get("chunk");
            //Uploading+file:+chunkCurr:+（前端传来的File MD5）
            String chunkCurrkey= UpLoadConstant.chunkCurr+fileMd5; //redis中记录当前文件应该穿第几块(从0开始)
            //通过这个Key去redis去获取当前上传文件块的标识
            String  chunkCurr=  RedisUtil.getString(chunkCurrkey);
            noGroupPath = "";
            //上传块大小
            Integer chunkSize=Convert.toInt(paramMap.get("chunkSize"));
            //如果redis中当前块标识为null
            if (StrUtil.isEmpty(chunkCurr)){
                return  ApiResult.fail("无法获取当前文件chunkCurr");
                // throw  new RuntimeException("无法获取当前文件chunkCurr");
            }
            //从redis中获取的上传文件块的标识
            Integer chunkCurr_int=Convert.toInt(chunkCurr);
            //前端传来的当前上传文件块的标识
            Integer chunk_int=Convert.toInt(chunk);

            //如果前端的文件块标识比当前redis中的小
            if (chunk_int<chunkCurr_int){
                return ApiResult.fail("当前文件块已上传");
            }else if (chunk_int>chunkCurr_int){
                //主要是重试上传
                return ApiResult.fail("当前文件块需要等待上传,稍后请重试");
            }
            //  System.out.println("***********开始**********");
            StorePath path = null;
            //暂时不支持多文件上传,后续版本可以再加上
            for (int i = 0; i < files.size(); ++i) {
                file = files.get(i);
                if (!file.isEmpty()) {
                    try {
                        //获取已经上传文件大小
                        Long historyUpload=0L;
                        //historyUpload:+(前端传来的fileMD5) 去数据库中获得历史上传大小
                        String historyUploadStr= RedisUtil.getString(UpLoadConstant.historyUpload+fileMd5);
                        if (StrUtil.isNotEmpty(historyUploadStr)){
                            historyUpload=Convert.toLong(historyUploadStr);
                        }
                        log.debug("historyUpload大小:"+historyUpload);
                        //如果从来没上传过
                        if (chunk_int==0){
                            //chunkCurrkey=Uploading+file:+chunkCurr:+（前端传来的File MD5）,0+1
                            RedisUtil.setString(chunkCurrkey,Convert.toStr(chunkCurr_int+1));
                            log.debug(chunk+":redis块+1");//0+":redis块+1"
                            try {
                                //第一次上传文件获得文件路径
                                path = appendFileStorageClient.uploadAppenderFile(UpLoadConstant.DEFAULT_GROUP, file.getInputStream(),
                                        file.getSize(), FileUtil.extName((String) paramMap.get("name")));
                                log.debug(chunk+":更新完fastdfs");
                                if (path== null ){
                                    //上传失败后，将chunkCurrkey=Uploading+file:+chunkCurr:+（前端传来的File MD5） 设置为 0
                                    RedisUtil.setString(chunkCurrkey,Convert.toStr(chunkCurr_int));
                                    return   ApiResult.fail("获取远程文件路径出错");
                                }
                            } catch (Exception e) {
                                RedisUtil.setString(chunkCurrkey,Convert.toStr(chunkCurr_int));
                               // e.printStackTrace();
                                //还原历史块
                                log.error("初次上传远程文件出错", e);
                                return   ApiResult.fail("上传远程服务器文件出错");
                            }
                            noGroupPath=path.getPath();
                            //fastDfsPath=Uploading:+file:+fastDfsPath:（前端传来的File MD5）
                            RedisUtil.setString(UpLoadConstant.fastDfsPath+fileMd5,path.getPath());
                            log.debug("上传文件 result={}", path);
                        }else {
                            //不是第一次上传
                            RedisUtil.setString(chunkCurrkey,Convert.toStr(chunkCurr_int+1));
                            log.debug(chunk+":redis块+1");
                            //fastDfsPath=Uploading:+file:+fastDfsPath:（前端传来的File MD5）
                            noGroupPath=RedisUtil.getString(UpLoadConstant.fastDfsPath+fileMd5);
                            if (noGroupPath== null ){
                                return ApiResult.fail("无法获取上传远程服务器文件出错");
                            }
                            try {
                                //追加方式实际实用如果中途出错多次,可能会出现重复追加情况,这里改成修改模式,即时多次传来重复文件块,依然可以保证文件拼接正确
                                appendFileStorageClient.modifyFile(UpLoadConstant.DEFAULT_GROUP, noGroupPath, file.getInputStream(),
                                        file.getSize(),historyUpload);
                                log.debug(chunk+":更新完fastdfs");
                            } catch (Exception e) {
                                RedisUtil.setString(chunkCurrkey,Convert.toStr(chunkCurr_int));
                                log.error("更新远程文件出错", e);
                             //   e.printStackTrace();
                              //  throw  new RuntimeException("初次上传远程文件出错");
                                return ApiResult.fail("更新远程文件出错");
                            }
                        }

                        //修改历史上传大小
                        historyUpload=historyUpload+file.getSize();
                        //historyUpload:+（前端传来的File MD5）
                        RedisUtil.setString(UpLoadConstant.historyUpload+fileMd5,Convert.toStr(historyUpload));

                        //最后一块,清空upload,写入数据库
                        String  fileName=  (String) paramMap.get("name");
                        Long size=Convert.toLong(paramMap.get("size"));
                        Integer chunks_int=Convert.toInt(paramMap.get("chunks"));
                        //当前上传块标识+1等于文件块总数
                        if (chunk_int+1==chunks_int){
                            //最后一块了
                            //持久化上传完成文件,也可以存储在mysql中

                            FileResult fileResult=new FileResult();
                            fileResult.setMd5(fileMd5);
                            fileResult.setName(fileName);
                            fileResult.setLength(size);
                            //默认group
                            fileResult.setUrl(UpLoadConstant.DEFAULT_GROUP+"/"+noGroupPath);
                            //redis完成列表信息加入redis中（Uploading:+completedList）
                            RedisUtil.rpush(UpLoadConstant.completedList, JSONUtil.toJsonStr(fileResult));
                            //清除redis中上传信息
                            RedisUtil.delKeys(new String[]{UpLoadConstant.chunkCurr+fileMd5,
                                    UpLoadConstant.fastDfsPath+fileMd5,
                                    UpLoadConstant.currLocks+fileMd5,
                                    UpLoadConstant.lockOwner+fileMd5
                            });
                        }
                    } catch (Exception e) {
                        log.error("上传文件错误", e);
                        //e.printStackTrace();
                        return ApiResult.fail("上传错误 " + e.getMessage());
                    }
                }
                break;
            }
        } finally {
            //锁的当前拥有者才能释放块上传锁
            if (currOwner){
                RedisUtil.setString(chunklockName,"0");
            }

        }
        //  System.out.println("***********结束**********");
        return ApiResult.success("http://18.18.18.22:8013/api/v1/files?storePath="+UpLoadConstant.DEFAULT_GROUP+"/"+noGroupPath+"&fileName="+(String) paramMap.get("name"));
    }


    /**
     * 当用户点击“点击上传附件”按钮并完成上传文件的选择与确认后，前端程序自动调用该接口
     *  请求参数（示例）：FormData：
     *          type:0
     *          fileName:arthas-packaging-3.4.4-bin.zip
     *          fileMD5:e085940a313a7806e462f236efe8e1dd
     *          fileSize:12521522(Byte,为文件的占用空间)
     * @param paramMap
     * @param request
     * @return
     * @throws IOException
     */
    @PostMapping("/checkFile")
    @ResponseBody
    ApiResult checkFile(@RequestParam Map<String, Object> paramMap, HttpServletRequest request) throws IOException {
        //storageClient.deleteFile(UpLoadConstant.DEFAULT_GROUP, "M00/00/D1/eSqQlFsM_RWASgIyAAQLLONv59s385.jpg");
//      String userName =  (String) request.getSession().getAttribute("name");
//      if (StrUtil.isEmpty(userName)){
//          return ApiResult.fail("请先登录");
//      }
        
        String fileMd5= (String) paramMap.get("fileMd5");
        if (StrUtil.isEmpty(fileMd5)){
            return ApiResult.fail("fileMd5不能为空");
        }
        CheckFileResult checkFileResult=new CheckFileResult();

       //模拟从mysql中查询文件表的md5,这里从redis里查询
        List<String> fileList=RedisUtil.getListAll(UpLoadConstant.completedList);
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

        //查询锁占用
//        Uploading:lock:currLocks:e085940a313a7806e462f236efe8e1dd
        String lockName=UpLoadConstant.currLocks+fileMd5;
//        12
        Long lock= RedisUtil.incrBy(lockName,1);
//        Uploading:lock:lockOwner:e085940a313a7806e462f236efe8e1dd
        String lockOwner=UpLoadConstant.lockOwner+ fileMd5;
//        Uploading:file:chunkCurr:e085940a313a7806e462f236efe8e1dd
        String chunkCurrkey=UpLoadConstant.chunkCurr+fileMd5;
        //不是第一次上传
        if (lock>1){
            checkFileResult.setLock(1);
            //检查是否为锁的拥有者,如果是放行
            String oWner= RedisUtil.getString(lockOwner);
            if (StrUtil.isEmpty(oWner)){
                return ApiResult.fail("无法获取文件锁拥有者");
            }else {
//                判断这个owner是不是登录的用户
                if (oWner.equals(request.getSession().getAttribute("name"))){
                    //如果是就获得这个当前上传的chunk index是多少，默认是个0
                    String  chunkCurr=  RedisUtil.getString(chunkCurrkey);
                    if (StrUtil.isEmpty(chunkCurr)){
                        return ApiResult.fail("无法获取当前文件chunkCurr");
                    }

                    checkFileResult.setChunkCurr(Convert.toInt(chunkCurr));
                    return ApiResult.success(checkFileResult);
                }else {
                    return ApiResult.fail("当前文件已有人在上传,您暂无法上传该文件");
                }

            }

        }else {
            //初始化锁.分块
            RedisUtil.setString(lockOwner, (String) request.getSession().getAttribute("name"));
            RedisUtil.setString(chunkCurrkey,"0"); //第一块索引是0,与前端保持一致
            checkFileResult.setChunkCurr(0);
            return  ApiResult.success(checkFileResult);
        }
    }


    /**
     * v1.3上传之前的检查
     * @param uploadChunk
     * @param request
     * @return
     * @throws IOException
     */
    @GetMapping("/uploadCheck")
    @ResponseBody
    ApiResult uploadCheck(UploadChunk uploadChunk, HttpServletRequest request) throws IOException {

        JSONObject resultObj = new JSONObject();
        resultObj.put("fileName",uploadChunk.getFileName());
        resultObj.put("chunks",uploadChunk.getChunks());
        resultObj.put("fileSize",uploadChunk.getFileSize());

        ApiResult checkReuslt = FileRedisUtil.isCompleted(uploadChunk.getFileMd5());
        if("0".equals(checkReuslt.getCode())){
            return checkReuslt;
        }else{
            int currentChunk = 0;
            String rcchunk = RedisUtil.getString("Uploading:file:chunkCurr:"+uploadChunk.getFileMd5());
            if(StrUtil.isNotEmpty(rcchunk)){
                currentChunk = Convert.toInt(rcchunk);
            }
            resultObj.put("uploadedChunks",currentChunk);
            Integer[] uploadedArr = UpLoadRedisUtils.getCachedIndexs(uploadChunk.getFileMd5());
            System.out.println(Arrays.toString(uploadedArr));
            return ApiResult.success(resultObj);
        }
    }


    /**
     * v1.3上传接口
     * @param uploadChunk
     * @param request
     * @return
     * @throws IOException
     */
    @PostMapping("/upload")
    @ResponseBody
    ApiResult upload(UploadChunk uploadChunk, HttpServletRequest request) throws IOException {
        uploadChunk.setChunk(uploadChunk.getChunk()-1);
        return importSeq(uploadChunk,request);
    }

    /**
     * v1.3每次请求的主要处理逻辑
     * @param uploadChunk
     * @param request
     * @return
     */
    private ApiResult importSeq(UploadChunk uploadChunk,HttpServletRequest request){
        List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
        MultipartFile file = null;
        if(files.size() > 0){
            file = files.get(0);
        }else{
            log.info("在请求中没有获取到任何文件流，importSeq()方法执行结束！");
            return ApiResult.fail("没有上传任何文件！");
        }
        String chunkTmpPath = "";
        uploadChunk.setChunkTmpPath("");
        if(UpLoadRedisUtils.hasCached(uploadChunk)){
            log.info("文件名："+uploadChunk.getFileName()+"的第：["+uploadChunk.getChunk()+"]已经上传过了!");
            return ApiResult.success("该文件已经上传过！");
        }

        try {
            chunkTmpPath = tmpMdbFile(file.getInputStream(),
                    FileUtil.extName(uploadChunk.getFileName())+uploadChunk.getChunk()+".tmp");
            uploadChunk.setChunkTmpPath(chunkTmpPath);
            UpLoadRedisUtils.addToCachedList(uploadChunk);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Future<UploadChunk> handleResult = null;
        try {
            handleResult = uploadChunkExcutor.dealWith(uploadChunk);
            uploadChunk = handleResult.get();
        } catch (InterruptedException e) {
            uploadChunk.setStatus(-1);
            log.info("线程被打断，第：[" + uploadChunk.getChunk() + "]块上传被终止" + e.getMessage());
        }catch (ExecutionException e){
            uploadChunk.setStatus(-2);
            log.info("线程执行中出现异常，第：[" + uploadChunk.getChunk() + "]块上传被终止" + e.getMessage());
        }

        return ApiResult.success(uploadChunk);
    }

    /**
     * Tomcat 获得临时文件转存路径
     * @param is
     * @param fileName
     * @return
     */
    public String tmpMdbFile(InputStream is, String fileName) {
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

    @Autowired
    private AppendFileStorageClient appendFileStorageClient;

    @Autowired
    private DefectiveAppendFileStorageClient defectiveClient;

    @Value("${sliceupload.timeout}")
    private Long timeout;

    @Value("${sliceupload.tmpdir}")
    private String tmpdir;

//    //自动装配 线程测试类
//    @Autowired
//    private MyTaskExecutor myTaskExecutor;

    @Autowired
    private UploadChunkExcutor uploadChunkExcutor;

    //存放在Map中，tomcat会报错[c:\\user\\...\\tomcat\\...\\root\\xxxxx.tmp文件找不到]
    public static Map<String,MultipartFile[]> md5_filestrem_map = new HashMap<>();//[key:文件的MD5码，value:文件有序的二进制流数组]

    //存放MD5-TMP_FILE_PATH
    public static volatile Map<String,String[]> md5_chunkpath_map = new HashMap<>();//[key:文件的MD5码，value:文件流转存的地址]
    //存放异步线程结果
    public static Map<String,Future<CheckFileResult>> md5_asyncresult_map = new HashMap<>();





}
