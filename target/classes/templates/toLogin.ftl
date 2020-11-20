<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>登录</title>
</head>
<body style="background: #f0f0f0">
        <div style="margin: 0 auto;width: 300px">
            <div>
                <span>用户名:</span>
                <input id="name" type="text">
            </div>
            <div style="margin-top: 20px">
                <span>密码:</span>
                <input id="password" type="text" style="margin-left: 15px">
            </div>
            <div style="margin-top: 20px">
                <button type="button" id="dl">登陆</button>
            </div>
        </div>
</body>
</html>

<script src="/js/jquery-3.2.1.min.js"></script>

<script>
    $('#dl').click(function () {
        $.post('/login/doLogin',{name:$('#name').val(),password:$('#password').val()},function (data) {
            if(data.code==1){
                alert(data.msg)
            }else if (data.code==0){
                location.href='/upload/Manage/upload_list'
            }
        })
    })
</script>