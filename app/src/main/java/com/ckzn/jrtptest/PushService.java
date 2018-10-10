package com.ckzn.jrtptest;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by wangxiaomei on 2018/9/13.
 */

public class PushService extends Service
{
    private static final String MIME_TYPE = "video/avc";
    private static final int VIDEO_FRAME_PER_SECOND = 25;
    private static final int VIDEO_I_FRAME_INTERVAL = 1;
    private static final int VIDEO_BITRATE = 1200000;

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private Worker mWorker;
    private byte[] mFrameByte;
    private RtpSendManager mSendThread;
    private byte[] mPpsSps;

    @Override
    public void onCreate()
    {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Intent mResultData = intent.getParcelableExtra("data");
        if (mResultData != null)
        {
            mMediaProjection = ((MediaProjectionManager) PushService.this.getSystemService(MEDIA_PROJECTION_SERVICE)).
                    getMediaProjection(Activity.RESULT_OK , mResultData);
            startPush();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopPush();
    }

    public void startPush()
    {
        if (mWorker == null)
        {
            mWorker = new Worker();
            mWorker.setRunning(true);
            mWorker.start();
            mSendThread = new RtpSendManager();
            mSendThread.start();
        }
    }

    public void stopPush()
    {
        if (mWorker != null)
        {
            mWorker.setRunning(false);
            mWorker = null;
            mSendThread.stopSend();
            mSendThread = null;
        }
    }

    private void onSurfaceCreated(Surface surface, int mWidth, int mHeight)
    {
        //将屏幕数据与surface进行关联
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("-display", mWidth, mHeight, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surface, null, null);
    }

    private void onSurfaceDestroyed(Surface surface)
    {
        mVirtualDisplay.release();
        surface.release();
    }

    private class Worker extends Thread
    {
        private MediaCodec.BufferInfo mBufferInfo;
        private MediaCodec mCodec;
        private volatile boolean isRunning;
        private Surface mSurface;
        private final long mTimeoutUse;
        private int mWidth = 1280;
        private int mHeight = 720;

        Worker()
        {
            mBufferInfo = new MediaCodec.BufferInfo();
            mTimeoutUse = 10000L;
        }

        private boolean prepare()
        {
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_PER_SECOND);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_I_FRAME_INTERVAL);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT , 1);
            format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / VIDEO_FRAME_PER_SECOND);
            try
            {
                mCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return false;
            }
            mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mCodec.createInputSurface();
            mCodec.start();
            onSurfaceCreated(mSurface, mWidth, mHeight);
            return true;
        }

        void setRunning(boolean running)
        {
            isRunning = running;
        }

        void onEncodedSample(byte[] data)
        {
            mSendThread.InsertVideoData(data);
        }

        @Override
        public void run()
        {
            if (!prepare())
            {
                isRunning = false;
            }
            while (isRunning)
            {
                encode();
            }
            release();
        }

        void encode()
        {
            if (!isRunning)
            {
                //编码结束，发送结束信号，让surface不在提供数据
                mCodec.signalEndOfInputStream();
            }
            int status = mCodec.dequeueOutputBuffer(mBufferInfo, mTimeoutUse);
            if (status != MediaCodec.INFO_TRY_AGAIN_LATER)
            {
                if (status >= 0)
                {
                    ByteBuffer data = mCodec.getOutputBuffer(status);
                    if (data != null)
                    {
                        byte[] outData = new byte[mBufferInfo.size];
                        data.get(outData);

                        int type = outData[4] & 0x07;
                        if (type == 7 || type == 8)
                        {
                            mPpsSps = outData;
                        }
                        else if (type == 5)
                        {
                            //在关键帧前面加上pps和sps数据
                            if (mPpsSps != null)
                            {
                                byte[] iframeData = new byte[mPpsSps.length + outData.length];
                                System.arraycopy(mPpsSps, 0, iframeData, 0, mPpsSps.length);
                                System.arraycopy(outData, 0, iframeData, mPpsSps.length, outData.length);
                                outData = iframeData;
                            }
                        }

                        onEncodedSample(outData);
                        // 一定要记得释放
                        mCodec.releaseOutputBuffer(status, false);
                    }
                }
            }
        }

        private void release()
        {
            onSurfaceDestroyed(mSurface);
            if (mCodec != null)
            {
                mCodec.stop();
                mCodec.release();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
