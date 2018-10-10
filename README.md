# ScreenPushSender
无线传屏Android发送端

Android无线传屏发送端的基本思路是：Android5.0以上使用MediaProjection截取屏幕内容，然后通过MediaCodec编码成H264的码流。

接下来就是视频码流传输的部分，这里我使用的JrtpLib这个开源库，是基于RTP协议实现的CPP库（RTP基于UDP的实时传输协议，所以传输速度相对较快，但不保证传输的可靠性，实现下来延时在600ms左右，网络不佳时略有花屏，如果彻底解决花屏的问题还需继续研究）。移植到Android需要自己网上库的源码进行编译成.a或者.so库。如果不想自己编译，可以使用该项目中编译好的。

发送端这边主要配置好接收端的IP地址，端口号不为奇数就行。如果视频帧过大，代码中也实现了分包发送的算法，需要研究可以将源码下载下来。这里只是一个简单的DEMO，可以在此基础上进行功能拓展和优化。
