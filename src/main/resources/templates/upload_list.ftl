<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>上传管理</title>
</head>
<style>

                          table.hovertable {
                              width: 100%;
                              font-family: verdana,arial,sans-serif;
                              font-size:11px;
                              color:#333333;
                              border-width: 1px;
                              border-color: #999999;
                              border-collapse: collapse;
                          }
    table.hovertable th {
        background-color:#c3dde0;
        border-width: 1px;
        padding: 8px;
        border-style: solid;
        border-color: #a9c6c9;
    }
    table.hovertable tr {
        background-color:#d4e3e5;
    }
    table.hovertable td {
        border-width: 1px;
        padding: 8px;
        border-style: solid;
        border-color: #a9c6c9;
    }



<!-- Table goes in the document BODY -->

</style>
<body style="background: #f0f0f0">
    <div id="contentBox" style="margin: 20px auto;width: 80%;">
                    <div >
                        <button id="go_login">去上传页面</button>
                    </div>

                <div>

                    <div style="margin-top: 20px">当前数据库所有上传成功文件:</div>
                    <table class="hovertable">
                        <tr>
                            <th>序号</td>
                            <th>文件名</td>
                            <th>上传路径</td>
                            <th>md5</td>
                            <th>大小</td>
                            <th>操作</td>
                        </tr>

                        <#list  fileList  as e>
                            <tr>
                                <td>${e_index}</td>
                                <td>${e.name}</td>
                                <td>${e.url}</td>
                                <td>${e.md5}</td>
                                <td>${(e.lenght/1024)?string('#.##')}KB</td>
                                <td>
                                    <a target="_blank" href="http://${fileServerUrl}/${e.url}">查看</a>
                                    <a  class="del" style="margin-left: 10px" md5="${e.md5}">删除</a>
                                </td>
                            </tr>
                        </#list>
                   
                    </table>
                </div>
    </div>
</body>
</html>
<script src="/js/jquery-3.2.1.min.js"></script>
<script src="/js/layer/layer.js"></script>
<script>
    $('.del').click(function () {





        var self=$(this);
        //询问框
        layer.confirm('确定要删除吗', {
        }, function(){
            $.post('/upload/Manage/del',{md5:self.attr('md5')},function (data) {
                if (data.code==0){
                    location.reload();
                }else {
                    alert(data.msg)
                }
            })
        });


    })

    $('#go_login').click(function () {
        window.open('/upload/Manage/toUpload');
    })
</script>