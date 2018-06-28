<#assign base=request.contextPath />
<base href="${base}">
<script>
    localStorage.setItem('basePath', '${base}');
    function getBaseUrl() {
        return '${base}'
    }
</script>






<#assign base=request.contextPath />
<base href="${base}">
<script>
    localStorage.setItem('basePath', '${base}');

    function getBaseUrl() {
        return '${base}'
    }
</script>

<link rel="stylesheet" href="${base}/js/zwUploader/webuploader.css">
<script src="${base}/js/zwUploader/webuploader.nolog.js"></script>
<script src="${base}/js/zwUploader/zwUploader.js"></script>
<#--<script src="${base}/assets/plugins/zwUplozwUploaderader/zwUploader.js"></script>-->
<style>
    .zw-document-uploader{position: relative; cursor: pointer;margin-top: 8px;}
    .zw-document-uploader.zw-hidden{display: hidden;}
    .zw-document-uploader.zw-ib{display: inline-block;}
    .zw-document-uploader-item .zw-upload-blank-index{}
    .zw-document-uploader-item .zw-upload-blank-view{color: deepskyblue;cursor:pointer;margin-left: 10px}
    .zw-document-uploader-item .zw-upload-blank-file{margin: 4px 24px 4px 0;display: inline-block;}
    .zw-document-uploader-item .zw-upload-blank-delete{color: red;cursor:pointer;margin-left: 5px}

    .upload-progress-box{
        display: inline-block;
        font-size: 16px;

    }
    .upload-progress-box .upload-progress{
        display: inline-block;
        width: 150px;
        height: 15px;
        border: 1px solid #000;
        margin-left:  10px;
        position: relative;
        top: 3px;
    }
    .upload-progress-box .upload-progress_bg{
        background:rgba(3, 9, 162, 0.78);
        width: 0%;
        height: 100%;
        display: inline-block;
        position: absolute;
    }


</style>
<script>
    function zwblankuploader_accept(){
        return false;
    }
    function zwblankuploader_createUploadBtn(controller, btnId) {
        var hideCss = controller.option.isReadonly ? 'zw-hidden' : 'zw-ib';
        var $btn = $("<div id='fileList_" + btnId + "' class='fileList' style=''></div><div id='" + btnId + "' class='zw-document-uploader "+hideCss+"' >点击上传附件 " + "</div>");
        return $btn;
    }
    function zwblankuploader_createUploadItem(controller, contentView, fileId, fileName, fileUrlWithHost, file) {
        var itemSize = contentView.find(".zw-upload-item").length
        var itemIndex = itemSize + 1;
        var indexItemString = "<span class='zw-upload-blank-index'>" + itemIndex + "、 " + "</span>";
        var delItemString = controller.option.isReadonly ? '' : "<a   fileId="+file.id+"   data-id='" + fileId + "' class='zw-upload-blank-delete'  >删除</a>";
        var $item = $("<div id='item_" + fileId + "' class='zw-upload-item zw-document-uploader-item'>" +
                "<div class='zw-upload-blank-file'>" + indexItemString + file.name + "</div>" +
                "<input type='hidden' name='"+ fileName +"' class='item_file_url'>"+
                "<div class='upload-progress-box'><span class='upload-progress'><span class='upload-progress_bg'></span></span> <span class='percent'>0</span><span>%</span> </div>"+
                delItemString +
                "<span class='upload_state' style='margin-left: 10px'>等待上传</span>"+
                "<a  class=re_upload style='margin-left: 10px;display: none'>重新上传</a>"+
                "<a data-id='" + fileId + "' class='zw-upload-blank-view'  style='display: none' >查看</a>" +
                "</div>");
        contentView.children(".fileList").append($item);
        return $item;
    }



    $(document).on('click','.zw-upload-blank-delete',function () {
        var uplodId=$(this).parents('.adhust_upload').get(0).controller.option.uploaderId;
        var upload=uploadMap.get(uplodId);

        upload.removeFile($(this).attr('fileId'),true);
        var item= $(this).parents('.zw-document-uploader-item');
        var itemParent=item.parent();
        item.remove();

        itemParent.children(".zw-upload-item").each(function (index) {
            $(this).find(".zw-upload-blank-index").text( (index + 1 ) + "、 ");
        });
    });

    $(document).on('click','.zw-upload-blank-view',function () {
        var openurl=$(this).parents('.zw-document-uploader-item').find('.item_file_url').val();
        window.open(ZWUP.fileServerUrl+openurl);
    })


    $(document).on('click','.re_upload',function () {
        var uplodId=$(this).parents('.adhust_upload').get(0).controller.option.uploaderId;
        var upload=uploadMap.get(uplodId);
        upload.retry();
    });





    //修复当上传需要隐藏时,无法出现文件弹出框问题

    $(document).on('click','.webuploader-pick',function () {
        $(this).parent().find('.webuploader-element-invisible').click();
    })
</script>