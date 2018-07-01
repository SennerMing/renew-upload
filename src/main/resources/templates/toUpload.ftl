<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>上传</title>
</head>
<body style="background: #f0f0f0">
        <div style="margin: 0 auto;width: 1000px">

            <div  style="" class="adhust_upload" id="user_other_documents" data-zw-upload-name="user_other_documents"
                  data-zw-upload-preview=""
                  data-zw-upload-preview-names="">
            </div>
        </div>

</body>
</html>
<script src="/js/jquery-3.2.1.min.js"></script>
<#include "./upload_common.ftl" />

<div>
</div>


<script>
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