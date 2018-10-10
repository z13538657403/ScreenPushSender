package com.ckzn.jrtptest;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by wangxiaomei on 2018/9/13.
 */

public class RtpSendManager extends Thread
{
    private Queue<byte[]> _videoDataQueue = new LinkedList<>();
    private Lock _videoDataQueueLock = new ReentrantLock();
    private Boolean mPushIsStart = false;
    private SendTool mSendTool;

    @Override
    public synchronized void start()
    {
        mSendTool = new SendTool("192.168.0.142" , 9002 , 9002);
        mPushIsStart = true;
        super.start();
    }

    public void stopSend()
    {
        mPushIsStart = false;
    }

    @Override
    public void run()
    {
        super.run();

        while (mPushIsStart)
        {
            if(_videoDataQueue.size() == 0)
            {
                try
                {
                    Thread.sleep(30);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                continue;
            }

            byte[] videoData = GetAndReleaseVideoQueue();
            if(videoData != null)
            {
                mSendTool.sendEncodeData(videoData, videoData.length);
            }
            try
            {
                Thread.sleep(4);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        _videoDataQueueLock.lock();
        _videoDataQueue.clear();
        _videoDataQueueLock.unlock();
        mSendTool.sendDestroy();
    }

    public void InsertVideoData(byte[] videoData)
    {
        if(!mPushIsStart)
        {
            return;
        }
        _videoDataQueueLock.lock();
        if(_videoDataQueue.size() > 50)
        {
            _videoDataQueue.clear();
        }
        _videoDataQueue.offer(videoData);
        _videoDataQueueLock.unlock();
    }

    private byte[] GetAndReleaseVideoQueue()
    {
        _videoDataQueueLock.lock();
        byte[] videoData = _videoDataQueue.poll();
        _videoDataQueueLock.unlock();
        return videoData;
    }
}
