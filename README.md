# 大文件上传,断点续传,秒传,fastdfs

#### 项目介绍
实现h5与fastdfs之间的断点续传,大文件上传,秒传

#### 软件架构
软件架构说明

webuploader+springboot+redis+fastdfs(服务端)+FastDFS_Client(非官网,这个客户端自带连接池)
#### 安装教程
1.fastdfs服务安装可参考 https://blog.csdn.net/wlwlwlwl015/article/details/52619851

2.配置好application.properties地址运行项目即可

####项目运行截图
(https://blog.csdn.net/zw521cx/article/details/80704026)

#### 使用说明

1.上传前要先登录,


2.上传前端已封装成jquery插件,前端使用步骤
#
1)页面引用
`<#include "./upload_common.ftl" />`
#
2)定义dom元素
```
     <div  style="" class="adhust_upload" id="user_other_documents" data-zw-upload-name="user_other_documents"
                  data-zw-upload-preview=""
                  data-zw-upload-preview-names="">
       </div>
```
#     
 3)定义js代码
 ```
 <script>
     //上传
        //上传
        $("#user_other_documents").zwUploader({
            accept: zwblankuploader_accept, //可以上传文件类型,一般用组件默认即可
            createUploadBtn: zwblankuploader_createUploadBtn,
            createUploadItem: zwblankuploader_createUploadItem,
            uploadFinishedHandler:function (item) {
                console.log('上传服务器路径:',item.find('.item_file_url').val())
            }
            
        });
 </script>

```

#
注意:文件块的大小要根据实际的需要进行设置,我们项目实际设置的是3m,切片到底设置多大合适可参考
http://itindex.net/detail/49267


#下面代码段是本项目中文件分片大小设置的地方
```
 Controller.prototype.uploader = function (pick) {
        var accept =this.option.accept()||this.defaultAccept;
        var runtimeOrder = this.option.runtimeOrder;
        var flashPath = this.option.flashPath;
        var uploadURLString = this.option.baseUrlString + this.option.uploadUrl;
        return WebUploader.create({
            swf: flashPath,
            pick: pick,
            server: uploadURLString,
            accept: accept,
            runtimeOrder: runtimeOrder,
            resize: false,
            compress: false,
            auto:true,
            chunkSize: 64,//1024 * 1024*3, //产品正式上线后,如果redis有未完成的文件,尽量不要修改次参数,否则会影响所有未完成的上传,
            chunked: true,
            threads:1,
            // auto: true,
        });
    };
    
```
#

#其他说明请看 [重要说明](https://gitee.com/zwlan/renewFastdfs/blob/master/重要说明) 文件

![欢迎大家加入java底层架构技术研究qq群](https://images.gitee.com/uploads/images/2019/0702/120905_8660b4c3_906208.png "欢迎大家加入java底层技术研究qq群")

欢迎大家加入java底层架构技术研究qq群: 304101674
<a target="_blank" href="//shang.qq.com/wpa/qunwpa?idkey=bbe9c65a531d4e38c294a3c17fe1930426f6d76b55f39c55f933fac5b982fd51"><img border="0" src="//pub.idqqimg.com/wpa/images/group.png" alt="Java架构底层核心技术" title="Java架构底层核心技术"></a>