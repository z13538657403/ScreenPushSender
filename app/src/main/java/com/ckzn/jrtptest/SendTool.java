package com.ckzn.jrtptest;

/**
 * Created by wangxiaomei on 2018/9/13.
 */

public class SendTool
{
    static
    {
        System.loadLibrary("native-lib");
    }

    public SendTool(String ip , int basePort , int destPort)
    {
        initRtpLib(ip, basePort , destPort);
    }

    public int sendEncodeData(byte[] buffer , int len)
    {
        return sendH264Data(buffer, len);
    }

    public int sendDestroy()
    {
        return deInitRtpLib();
    }

    public native int initRtpLib(String ip , int basePort , int destPort);

    public native int sendH264Data(byte[] encodeData , int dataLen);

    public native int deInitRtpLib();
}
