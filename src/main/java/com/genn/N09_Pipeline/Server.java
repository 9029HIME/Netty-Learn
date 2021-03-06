package com.genn.N09_Pipeline;

import com.genn.N05_Netty.Handler.NettyServerHandler;
import com.genn.N09_Pipeline.Handler.ServerDecodeHandler;
import com.genn.N09_Pipeline.Handler.ServerPrintHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Server {
    public static void main(String[] args) throws InterruptedException {
        /*
            BossGroup与WorkerGroup
            1.BossGroup只处理accept请求，workerGroup处理read和write请求
            2.两个group在正式运行时，都是死循环
         */
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        /*
            设置启动参数
         */
        try {
            ServerBootstrap config = new ServerBootstrap();
            config.group(bossGroup, workerGroup).    //设置两个线程组
                    channel(NioServerSocketChannel.class). //设置NioSocketChannel来封装ServerSocketChannel
                    option(ChannelOption.SO_BACKLOG, 128). //TCP/IP协议中的backlog参数，即可连接队列大小
                    childOption(ChannelOption.SO_KEEPALIVE, true). //设置保持活动连接状态
                    childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    //先加decoder，再加print
                    ch.pipeline().addLast(new ServerDecodeHandler())
                    .addLast(new ServerPrintHandler());
                }
            });  //设置Handler

        /*
            绑定端口并且同步，相当于启动服务器了
         */
            ChannelFuture sync = config.bind(8080).sync();
            //这里只是举个例子 如果bind不同步的话，主线程跑完就关掉了
            sync.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(future.isSuccess()){
                        System.out.println("端口8080监听成功");
                    }else{
                        System.out.println("监听失败");
                    }
                }
            });


            //对关闭通道进行监听
            sync.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
