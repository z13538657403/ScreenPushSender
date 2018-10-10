#include <stdio.h>
#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <unistd.h>
#include <signal.h>
#include <stdlib.h>

#include "jrtplib3/rtpsession.h"
#include "jrtplib3/rtpudpv4transmitter.h"
#include "jrtplib3/rtpipv4address.h"
#include "jrtplib3/rtpsessionparams.h"
#include "jrtplib3/rtppacket.h"
#include "jrtplib3/rtperrors.h"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,"Native_Log",__VA_ARGS__)
#define H264 96
#define MAX_RTP_PKT_LENGTH 1360

using namespace std;
using namespace jrtplib;

RTPSession session;

extern "C"
JNIEXPORT jint JNICALL
Java_com_ckzn_jrtptest_SendTool_initRtpLib(JNIEnv *env, jobject instance, jstring ip_, jint basePort, jint destPort)
{
    RTPSessionParams sessionParams;
    RTPUDPv4TransmissionParams transmissionParams;
    int status = 0;

    sessionParams.SetOwnTimestampUnit(1.0/9000.0);
    sessionParams.SetAcceptOwnPackets(true);

    transmissionParams.SetPortbase((uint16_t)basePort);
    status = session.Create(sessionParams , &transmissionParams);
    LOGD("init error info111 = %s" , RTPGetErrorString(status).c_str());

    const char* ipStr = env->GetStringUTFChars(ip_ , 0);
    uint32_t destIp = ntohl(inet_addr(ipStr));
    RTPIPv4Address addr(destIp ,(uint16_t)destPort);
    status = session.AddDestination(addr);
    LOGD("init error info = %s" , RTPGetErrorString(status).c_str());
    session.SetDefaultPayloadType(H264);
    session.SetDefaultMark(true);		           //设置位
    session.SetTimestampUnit(1.0/9000.0);          //设置采样间隔
    session.SetDefaultTimestampIncrement(3600);    //设置时间戳增加间隔

    return 1;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_ckzn_jrtptest_SendTool_sendH264Data(JNIEnv *env, jobject instance, jbyteArray encodeData_, jint buflen)
{
    int status;
    //发送数据指针
    jbyte* bBuffer = env->GetByteArrayElements(encodeData_ , 0);
    unsigned char* pSendbuf = (unsigned char*)bBuffer;
    //发送的数据缓冲
    char sendBuf[1430];
    memset(sendBuf , 0 , 1430);

    if (buflen <= MAX_RTP_PKT_LENGTH)
    {
        memcpy(sendBuf , pSendbuf , (size_t)buflen);
        status = session.SendPacket((void *)sendBuf , (size_t)buflen);
    }
    else if(buflen > MAX_RTP_PKT_LENGTH)
    {
        //设置标志位Mark为0
        session.SetDefaultMark(false);
        //得到该需要用多少长度为MAX_RTP_PKT_LENGTH字节的RTP包来发送
        int k=0,l=0;
        k = buflen / MAX_RTP_PKT_LENGTH;
        l = buflen % MAX_RTP_PKT_LENGTH;
        //用指示当前发送的是第几个分片RTP包
        int t=0;

        while(t < k || (t==k && l>0))
        {
            //第一包到最后包的前一包
            if((0 == t) || (t<k))
            {
                memcpy(sendBuf ,&pSendbuf[t*MAX_RTP_PKT_LENGTH] , MAX_RTP_PKT_LENGTH);
                status = session.SendPacket((void *)sendBuf , MAX_RTP_PKT_LENGTH);
                t++;
            }
            else if((k==t && l>0) || (t== (k-1) && l==0))
            {
                //设置标志位Mark为1, 最后一包
                session.SetDefaultMark(true);

                int iSendLen;
                if ( l > 0)
                {
                    iSendLen = buflen - t * MAX_RTP_PKT_LENGTH;
                }
                else
                {
                    iSendLen = MAX_RTP_PKT_LENGTH;
                }
                memcpy(sendBuf , &pSendbuf[t*MAX_RTP_PKT_LENGTH] , (size_t)iSendLen);
                status = session.SendPacket((void *)sendBuf , (size_t)iSendLen);
                LOGD("send buffer info = %d    , is error = %d" , buflen , status);
                t++;
            }
        }
    }
    return 1;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_ckzn_jrtptest_SendTool_deInitRtpLib(JNIEnv *env, jobject instance)
{
    session.BYEDestroy(RTPTime(3,0),0,0);
    return 1;
}
