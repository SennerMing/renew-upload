package com.zw.renewupload.common;

public class UpLoadConstant {
    private UpLoadConstant() {

    }

    private  final static  String uploading="Uploading:";
    private final  static  String lock=uploading+"lock:";
    private  final  static String file=uploading+"file:";
    //当前所有锁
    public  final static  String  currLocks=lock+"currLocks:";
    //当前锁的拥有者
    public  final static  String  lockOwner=lock+"lockOwner:";

    //当前文件传输到第几块
    public final  static  String chunkCurr=file+"chunkCurr:";

    //当前文件上传到fastdfs路径
    public final static String fastDfsPath=file+"fastDfsPath:";

    //默认分组
    public final static  String DEFAULT_GROUP = "group1";

    //全部上传成功已完成
    public final static String completedList=uploading+"completedList";


}
