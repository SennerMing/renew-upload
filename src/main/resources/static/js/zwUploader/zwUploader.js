



window.ZWUP = window.ZWUP || {};
ZWUP.basePath =getBaseUrl();
ZWUP.flashPath = ZWUP.basePath + "script/Uploader.swf";
ZWUP.configUrl = ZWUP.basePath + "/upload/chunkUpload/config";

ZWUP.fileServerUrl = ZWUP.fileServerUrl || null;
ZWUP.isServerConfigLoaded = false;
ZWUP.maxSize = 1024 * 1024 * 100;
//uploadMap实例
var uploadMap=new myMap();

//线上文件块记录
var chunkMap=new myMap();
(function ($) {

    $.fn.extend({
        'zwUploader': function (option, args, fire) {

            var items = this.each(function () {
                console.log('插件this:',this);
                 if ($.isPlainObject(option))
                    this.controller = new Controller(this, option);
            });
            return items;
        }
    });


    WebUploader.Uploader.register({
        "before-send-file": "beforeSendFile"
        , "before-send": "beforeSend"
    }, {
        beforeSendFile: function(file){
            var fileName = file.name;
            var fileSize = file.size;
            var file_ruid=file.id;
            var that=this.owner.controller;
            console.log(file);
            //秒传验证
            var task = new $.Deferred();
            (new WebUploader.Uploader()).md5File(file, 0, 10*1024*1024).progress(function(percentage){
            }).then(function(val){
                fileMd5 = val;
                var url = that.option.baseUrlString + that.option.checkUrl;
                var data = {
                    type: 0,
                    fileName: fileName,
                    fileMd5: fileMd5,
                    fileSize: fileSize
                };
                $.ajax({
                    type: "POST",
                    url: url,
                    data: data,
                    cache: false,
                    async: false, // 同步
                    dataType: "json",
                    error: function(XMLHttpRequest, textStatus, errorThrown) {
                        file.statusText = '服务器错误';
                        task.reject();

                        var up_self=uploadMap.get(file.source.rid);
                    }
                }).then(function(data, textStatus, jqXHR) {
                    if(data.code == 0) {

                        if(data.data&&data.data.totalSize>0){
                            file.statusText = '该文件已上传过';
                            setProStyle(file,1);
                            var up_self=uploadMap.get(that.option.uploaderId);
                            up_self.skipFile(file);
                            file.viewPath=data.data.viewPath;
                            task.resolve()
                            return
                        }

                        if(!(data.data.chunkCurr>=0)){
                            file.statusText = '无法获取当前文件块';
                            task.reject();
                            return
                        }
                        chunkMap.put(file_ruid,{fileMd5:fileMd5,chunkCurr:data.data.chunkCurr});
                        task.resolve();

                    } else {
                        if(data&&data.msg){
                            alert(data.msg)
                        }else {
                            alert('服务器异常')
                        }

                        task.reject();
                    }
                });



                console.log('beforeSendFile:'+val)
                //task.resolve();
            });
            return $.when(task);
        }
        , beforeSend: function(block){
           // var that=this.owner.controller;
            var task = new $.Deferred();

            var ruid=block.file.id;
            var chunkCurr=chunkMap.get(ruid).chunkCurr;

            if(block.chunk<chunkCurr){
                //console.log(block)
                task.reject();
            }else {
                console.log("第"+block.chunk+"块开始上传")
                task.resolve();
            }

            return $.when(task);
        }
        ,
    });

    var Controller = function (input, option) {
        console.log('插件input:',this);
        var flashPath = ZWUP.flashPath;
        var fileName = $(input).attr('data-ZW-upload-name') || "file";
        var previewNames = $(input).attr('data-ZW-upload-preview') || "";
        var perviewFileNames = $(input).attr('data-ZW-upload-preview-names') || "";
        this.defaultAccept = {
            title:'支持的文件类型',
            extensions: 'jpg,jpeg,png,gif,bmp,doc,docx,pdf,xls,xlsx,ppt,pptx,zip,rar',
            mimeTypes: 'image/jpg,image/jpeg,image/png,image/gif,image/bmp,application/msword,' +
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/pdf,' +
            'application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,' +
            'application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument.presentationml.presentation,application/x-zip-compressed,application/x-rar-compressed'
        };
        this.input = input;
        this.option = $.extend(false, {
            // url
            'baseUrlString': ZWUP.basePath,                                 // string
            'checkUrl': getBaseUrl()+'/upload/chunkUpload/checkFile',     // string
            'uploadUrl': getBaseUrl()+'/upload/chunkUpload/upload_do',   // string
            'flashPath': flashPath,                         // string
            // data
            'accept': this.defaultAccept,                   // function
            'fileName': fileName,                           // string
            'previewNames': previewNames.split(","),        // string
            'perviewFileNames': perviewFileNames.split(","),// string
            'minItems': 1,                                  // number
            'maxItems': 20,                                 // number
            'uploadFinishedHandler': null,                  // function 上传文件完成回调
        }, option);

        if (!ZWUP.isServerConfigLoaded) {
            var that = this;
            _loadServerConfig(function () {
                _setup.apply(that, [input]);
            }, that);
        } else {
            _setup.apply(this, [input]);
        }
    };

    //设置进度条样式
    var setProStyle=function (file,percentage) {
        var progressVal = Math.min(toDecimal(percentage * 100,2), 100);
        var item=$('#item_'+file.id);
        var  progressView=item.find('.upload-progress_bg');
        progressView.width(progressVal+'%');
        var  progressText=item.find('.percent');
        progressText.text(progressVal);
        if(percentage>=1){
            var upload_state=item.find('.upload_state');
            file.statusText='上传成功@'
            var delBtn=item.find('.ZW-upload-blank-delete');
            delBtn.show();
        }
    }

    var _loadServerConfig = function (callback, controller) {
        var configUrl = ZWUP.configUrl;
        var that = this;
        $.ajax(configUrl, {
            async:false,
            'type': 'get',
            'success': function (r) {

                config=JSON.parse(r);
                    ZWUP.fileServerUrl = config.fileServerUrl || ZWUP.fileServerUrl;
                    ZWUP.isServerConfigLoaded = true;
                    ZWUP.maxSize = config.maxSize || ZWUP.maxSize;
                    callback();
            },
            'error': function () {
                console.log('获取配置失败!');
            }
        });
    };






    // ------- Private Method Here -------------
    var _setup = function (input) {
        var that = this;
        var $input = $(input);
        var uploaderId = "file_" + new Date().getTime() + "_" + parseInt(Math.random() * 1000, 10);
        that.option.uploaderId = uploaderId;
        var $uploadContentView = that.option.createUploadBtn(that, uploaderId);
        $input.append($uploadContentView);


        var _uploader = this.uploader("#" + uploaderId);
        _uploader.controller = that;
        uploadMap.put(uploaderId,_uploader);

        // 当有文件被添加进队列的时候
        _uploader.on( 'fileQueued', function( file ) {
            _appendFile.apply(that,['', file]);
            //  this.upload();
        });

        _uploader.on( 'beforeFileQueued', function( file ) {
            var controller = _uploader.controller;
            if (controller.option.isAllowAddFile){
                return controller.option.isAllowAddFile(controller);
            }
            return true;
        });

        _uploader.on( 'uploadBeforeSend', function( block, data ) {

            data.fileMd5 = chunkMap.get(block.file.id).fileMd5;
            data.chunkSize=block.blob.size;

        });

        _uploader.on( 'uploadStart', function( file ) {
            var item=$('#item_'+file.id);
            item.find('.upload_state').text('开始上传');
            item.find('.re_upload').hide();
            //  progressView.show();
        });

        // 文件上传过程中创建进度条实时显示。
        _uploader.on( 'uploadProgress', function( file, percentage ) {
            setProStyle(file,percentage);
        });



        _uploader.on('uploadAccept', function (file, response) {
            console.log("uploadAccept-response:", response._raw)
            var result
            try
            {
                result= JSON.parse(response._raw)
                if(result.code==1){
                    file.file.statusText=result.msg;
                    file.file.viewPath='';
                }else {
                    file.file.viewPath=result.data
                    return true
                }

            }
            catch(err)
            {
                console.log(err)
                return false
            }


            return false
        });

        _uploader.on( 'uploadError', function( file ) {
            var item=$('#item_'+file.id);
            item.find('.upload_state').text('上传出错:'+file.statusText);
            item.find('.re_upload').show();
            console.log('上传error:'+file.statusText)
        });

        _uploader.on('uploadSuccess', function(file ,resp){
            console.log('uploadSuccess:',file,resp)
            var item=$('#item_'+file.id);
            item.find('.upload_state').text(file.statusText);
            //经过传输的file,即时在uploadAccept绑定viewPath,这里也接收不到
            item.find('.item_file_url').val(file.viewPath||resp.data||'');
            item.find('.zw-upload-blank-view').show();

            if(  this.controller.option.uploadFinishedHandler){
                this.controller.option.uploadFinishedHandler(item);
            }

        });

        _uploader.on( 'uploadComplete', function( file ) {
            console.log('uploadComplete')
        });
    };




    var _appendFile = function (fileUrl, file) {
        // var fileId = "file_" + new Date().getTime() + "_" + parseInt(Math.random() * 1000, 10);

        var fileId=file.id;
        console.log("fileIdxxx:"+fileId);
        var fileName = this.option.fileName;
        var contentView = $(this.input);
        var fileUrlWithHost = ZWUP.fileServerUrl + fileUrl;
        file.url = fileUrl;
        var $item = this.option.createUploadItem(this, contentView, fileId, fileName, fileUrlWithHost, file);

    };





    var _error = function (msg) {
        if ($.isFunction(this.option.onerror)) {
            this.option.onerror.apply(this, [msg]);
        }
    };



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
            chunkSize: 1024 * 1024*10, //产品正式上线后尽量不要修改次参数,否则会影响所有上传,
            chunked: true,
            threads:1,
            // auto: true,
        });
    };


})(jQuery);

////////////////////////////////////////////////////////////////工具类//////////////////////////////////////////////
//保留两位小数
//功能：将浮点数四舍五入，取小数点后n位
//http://www.cnblogs.com/hongchenok/archive/2011/11/02/2232810.html
function toDecimal(x, n) {
    var num = arguments[1] ? arguments[1] : 2;
    n=num;
    var f = parseFloat(x);
    function  getN(n){
        var sum =1;
        for (var i=0 ;i< n;i++){
            sum*=10;
        }
        return sum ;
    }
    if (isNaN(f)) {
        return 0;
    }

    f = Math.round(x * getN(n)) / getN(n);
    return f;
}


//map方法定义
function myMap() {
    this.elements = new Array();

    //获取MAP元素个数
    this.size = function() {
        return this.elements.length;
    }

    //判断MAP是否为空
    this.isEmpty = function() {
        return(this.elements.length < 1);
    }

    //删除MAP所有元素
    this.clear = function() {
        this.elements = new Array();
    }

    //向MAP中增加元素（key, value)
    this.put = function(_key, _value) {
        var exist=this.containsKey(_key);
        if(exist){
            this.remove(_key);
        }

        this.elements.push({
            key: _key,
            value: _value
        });
    }

    //删除指定KEY的元素，成功返回True，失败返回False
    this.remove = function(_key) {
        var bln = false;
        try {
            for(i = 0; i < this.elements.length; i++) {
                if(this.elements[i].key == _key) {
                    this.elements.splice(i, 1);
                    return true;
                }
            }
        } catch(e) {
            bln = false;
        }
        return bln;
    }

    //获取指定KEY的元素值VALUE，失败返回NULL
    this.get = function(_key) {
        try {
            for(i = 0; i < this.elements.length; i++) {
                if(this.elements[i].key == _key) {
                    return this.elements[i].value;
                }
            }
        } catch(e) {
            return null;
        }
    }

    //获取指定索引的元素（使用element.key，element.value获取KEY和VALUE），失败返回NULL
    this.element = function(_index) {
        if(_index < 0 || _index >= this.elements.length) {
            return null;
        }
        return this.elements[_index];
    }

    //判断MAP中是否含有指定KEY的元素
    this.containsKey = function(_key) {
        var bln = false;
        try {
            for(i = 0; i < this.elements.length; i++) {
                if(this.elements[i].key == _key) {
                    bln = true;
                }
            }
        } catch(e) {
            bln = false;
        }
        return bln;
    }

    //判断MAP中是否含有指定VALUE的元素
    this.containsValue = function(_value) {
        var bln = false;
        try {
            for(i = 0; i < this.elements.length; i++) {
                if(this.elements[i].value == _value) {
                    bln = true;
                }
            }
        } catch(e) {
            bln = false;
        }
        return bln;
    }

    //获取MAP中所有VALUE的数组（ARRAY）
    this.values = function() {
        var arr = new Array();
        for(i = 0; i < this.elements.length; i++) {
            arr.push(this.elements[i].value);
        }
        return arr;
    }

    //获取MAP中所有KEY的数组（ARRAY）
    this.keys = function() {
        var arr = new Array();
        for(i = 0; i < this.elements.length; i++) {
            arr.push(this.elements[i].key);
        }
        return arr;
    }
}