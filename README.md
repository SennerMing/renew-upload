# 大文件上传断点续传fastdfs

#### 项目介绍
实现h5与fastdfs之间的断点续传,大文件上传,

#### 软件架构
软件架构说明

webuploader+springboot+redis+fastdfs(服务端)+FastDFS_Client(非官网,这个客户端自带连接池)
#### 安装教程
1.fastdfs服务安装可参考 https://blog.csdn.net/wlwlwlwl015/article/details/52619851

2.配置好application.properties地址运行项目即可


#### 使用说明

1.上传前要先登录,


2.上传前端已封装成jquery插件,前端使用步骤
1>页面引用
<#include "./upload_common.ftl" />
2>定义dom元素
     <div  style="" class="adhust_upload" id="user_other_documents" data-zw-upload-name="user_other_documents"
                  data-zw-upload-preview=""
                  data-zw-upload-preview-names="">
            </div>
 3>定义js代码
 <script>
     //上传
     $(".adhust_upload").zwUploader({
         accept: zwblankuploader_accept, //可以上传文件类型,一般用组件默认即可
         createUploadBtn: zwblankuploader_createUploadBtn,
         createUploadItem: zwblankuploader_createUploadItem,
     });
 </script>

