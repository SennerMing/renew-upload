package com.zw.renewupload.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.zw.renewupload.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/upload/chunkUpload")
public class ChunkUpload {
    @Autowired
    private AppendFileStorageClient appendFileStorageClient;

    protected Logger _logger = LoggerFactory.getLogger(this.getClass());
    //获取配置
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
        String chunklockName=UpLoadConstant.chunkLock+fileMd5;

        String temOwner= RandomUtil.randomUUID();
        boolean currOwner=false;//真正的拥有者
        try {
            String userName=  (String) request.getSession().getAttribute("name");
            if (StrUtil.isEmpty(userName)){
                return  ApiResult.fail("请先登录");
            }


            if (!paramMap.containsKey("chunk")){
                paramMap.put("chunk","0");
            }

            if (!paramMap.containsKey("chunks")){
                paramMap.put("chunks","1");
            }

            Long lock= RedisUtil.incrBy(chunklockName,1);
            if (lock>1){
                return ApiResult.fail("请求块锁失败");
            }

            //写入锁的当前拥有者
            currOwner=true;

            List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
            MultipartFile file = null;
            BufferedOutputStream stream = null;
            String chunk= (String) paramMap.get("chunk");
            String chunkCurrkey= UpLoadConstant.chunkCurr+fileMd5; //redis中记录当前应该穿第几块(从0开始)
            String  chunkCurr=  RedisUtil.getString(chunkCurrkey);
            noGroupPath = "";
            Integer chunkSize=Convert.toInt(paramMap.get("chunkSize"));
            if (StrUtil.isEmpty(chunkCurr)){
                return  ApiResult.fail("无法获取当前文件chunkCurr");
                // throw  new RuntimeException("无法获取当前文件chunkCurr");
            }
            Integer chunkCurr_int=Convert.toInt(chunkCurr);
            Integer chunk_int=Convert.toInt(chunk);


            if (chunk_int<chunkCurr_int){
                return ApiResult.fail("当前文件块已上传");
            }else if (chunk_int>chunkCurr_int){
                return ApiResult.fail("当前文件块需要等待上传,稍后请重试");
            }
            //  System.out.println("***********开始**********");
            StorePath path = null;
            //暂时不支持多文件上传,后续版本可以再加上
            for (int i = 0; i < files.size(); ++i) {
                file = files.get(i);
                if (!file.isEmpty()) {
                    try {

                        if (chunk_int==0){

                            RedisUtil.setString(chunkCurrkey,Convert.toStr(chunkCurr_int+1));
                            _logger.debug(chunk+":redis块+1");
                            try {
                                path = appendFileStorageClient.uploadAppenderFile(UpLoadConstant.DEFAULT_GROUP, file.getInputStream(),
                                        file.getSize(), FileUtil.extName((String) paramMap.get("name")));
                                _logger.debug(chunk+":更新完fastdfs");
                                if (path== null ){
                                    RedisUtil.setString(chunkCurrkey,Convert.toStr(chunkCurr_int));
                                    return   ApiResult.fail("获取远程文件路径出错");
                                }

                            } catch (Exception e) {
                                RedisUtil.setString(chunkCurrkey,Convert.toStr(chunkCurr_int));
                               // e.printStackTrace();
                                //还原历史块
                                _logger.error("初次上传远程文件出错", e);
                                return   ApiResult.fail("上传远程服务器文件出错");

                            }
                            noGroupPath=path.getPath();
                            RedisUtil.setString(UpLoadConstant.fastDfsPath+fileMd5,path.getPath());
                            _logger.debug("上传文件 result={}", path);
                        }else {
                            RedisUtil.setString(chunkCurrkey,Convert.toStr(chunkCurr_int+1));
                            _logger.debug(chunk+":redis块+1");
                            noGroupPath=RedisUtil.getString(UpLoadConstant.fastDfsPath+fileMd5);
                            if (noGroupPath== null ){
                                return   ApiResult.fail("无法获取上传远程服务器文件出错");
                            }

                            try {
                                //追加方式实际实用如果中途出错多次,可能会出现重复追加情况,这里改成修改模式,即时多次传来重复文件块,依然可以保证文件拼接正确
                                appendFileStorageClient.modifyFile(UpLoadConstant.DEFAULT_GROUP, noGroupPath, file.getInputStream(),
                                        file.getSize(),chunkCurr_int*chunkSize);
                                _logger.debug(chunk+":更新完fastdfs");
                            } catch (Exception e) {
                                RedisUtil.setString(chunkCurrkey,Convert.toStr(chunkCurr_int));
                                _logger.error("更新远程文件出错", e);
                             //   e.printStackTrace();
                              //  throw  new RuntimeException("初次上传远程文件出错");
                                return  ApiResult.fail("更新远程文件出错");
                            }


                        }

                        //最后一块,清空upload,写入数据库

                        String  fileName=  (String) paramMap.get("name");
                        Long size=Convert.toLong(paramMap.get("size"));
                        Integer chunks_int=Convert.toInt(paramMap.get("chunks"));
                        if (chunk_int+1==chunks_int){

                            //持久化上传完成文件,也可以存储在mysql中

                            FileResult fileResult=new FileResult();
                            fileResult.setMd5(fileMd5);
                            fileResult.setName(fileName);
                            fileResult.setLenght(size);
                            fileResult.setUrl(UpLoadConstant.DEFAULT_GROUP+"/"+noGroupPath);

                            RedisUtil.rpush(UpLoadConstant.completedList, JSONUtil.toJsonStr(fileResult));

                            RedisUtil.delKeys(new String[]{UpLoadConstant.chunkCurr+fileMd5,
                                    UpLoadConstant.fastDfsPath+fileMd5,
                                    UpLoadConstant.currLocks+fileMd5,
                                    UpLoadConstant.lockOwner+fileMd5
                            });
                        }


                    } catch (Exception e) {
                        _logger.error("上传文件错误", e);
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
        return  ApiResult.success(UpLoadConstant.DEFAULT_GROUP+"/"+noGroupPath);
    }


    @PostMapping("/checkFile")
    public
    @ResponseBody
    ApiResult checkFile(@RequestParam Map<String, Object> paramMap, HttpServletRequest request) throws IOException {
        //storageClient.deleteFile(UpLoadConstant.DEFAULT_GROUP, "M00/00/D1/eSqQlFsM_RWASgIyAAQLLONv59s385.jpg");
      String userName=  (String) request.getSession().getAttribute("name");
      if (StrUtil.isEmpty(userName)){
          return  ApiResult.fail("请先登录");
      }

        String fileMd5= (String) paramMap.get("fileMd5");
        if (StrUtil.isEmpty(fileMd5)){
            return  ApiResult.fail("fileMd5不能为空");
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
                    return  ApiResult.success(checkFileResult);
                }
            }
        }





        //查询锁占用

        String lockName=UpLoadConstant.currLocks+fileMd5;
        Long lock= RedisUtil.incrBy(lockName,1);
        String lockOwner=UpLoadConstant.lockOwner+ fileMd5;
        String chunkCurrkey=UpLoadConstant.chunkCurr+fileMd5;
        if (lock>1){
            checkFileResult.setLock(1);
            //检查是否为锁的拥有者,如果是放行
            String oWner= RedisUtil.getString(lockOwner);
            if (StrUtil.isEmpty(oWner)){
                return  ApiResult.fail("无法获取文件锁拥有者");
            }else {
                if (oWner.equals(request.getSession().getAttribute("name"))){
                    String  chunkCurr=  RedisUtil.getString(chunkCurrkey);
                    if (StrUtil.isEmpty(chunkCurr)){
                        return  ApiResult.fail("无法获取当前文件chunkCurr");
                    }

                    checkFileResult.setChunkCurr(Convert.toInt(chunkCurr));
                    return  ApiResult.success(checkFileResult);
                }else {
                    return    ApiResult.fail("当前文件已有人在上传,您暂无法上传该文件");
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
}
