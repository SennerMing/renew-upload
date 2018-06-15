package com.zw.renewupload.common;

import java.util.HashMap;
import java.util.Map;

//模拟数据库
public class Table {
    private Table(){};
    //用户表
    public static Map user=new HashMap();


    static {
        //添加用户账号密码
        user.put("admin","123456");
        user.put("zhang","123456");
        user.put("wang","123456");
    }
}
