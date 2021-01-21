# 说说TCP的老毛病：粘包和拆包

​	《TCP/IP详解》里没有对粘包和拆包的定义，但TCP是面向连接、面向流的，发送端给接收端发送数据时，为了更有效更便捷，会使用Nagle算法将多个较小的包合成一个大包，最后将大包发送过去。虽说这样做是为了提高效率，但接收端收到这么大的包就无法从里面拆分出完整的数据了。同时接收端也有可能因为缓冲区太小，无法一次性接收整个包的数据，会分段接受。相反，缓冲区太大也会导致一次性接受了两个包。这些情况都会导致客户端无法识别**分段且完整**的数据，**毕竟流的通信是没有边界保护的**。

​	假设发送端C，服务端S，C要发送两个数据包D1与D2给S。会有以下四种情况：

​	1.S第一次读取到D1，第二次读取到D2，没有发生粘包与拆包。

​	2.S一次性把D1，D2给读到缓冲区，此时S会认为D1+D2是一个完整的数据包，即发生了粘包。

​	3.S第一次读取到D1+D2的一部分，第二次读取到D2的另一部分（缓冲区过大），即发生了拆包。

​	4.S第一次读取到D1的一部分，第二次读取到D1另一部分+D2（缓冲区过小），即发生了拆包。

​	比如Client端的关键代码，连接成功收到服务器的响应后，给他发100个数据：

```java
@Override
public void channelActive(ChannelHandlerContext ctx) throws Exception {
    for (int i = 0; i < 100; i++) {
        ByteBuf byteBuf = Unpooled.copiedBuffer("nihaoabcdefghijklmn:" + 1, CharsetUtil.UTF_8);
        ctx.writeAndFlush(byteBuf);
    }
}
```

​	Server端的关键接收代码：

```java
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ByteBuf byteBuf = (ByteBuf) msg;
    byte[] array = new byte[byteBuf.readableBytes()];
    byteBuf.readBytes(array);
    String content = new String(array, CharsetUtil.UTF_8);
    System.out.println("本次收到的内容是"+content);
}
```

​	然后我们能看到，Server端的console打印了这样的内容：

​	![image-20210120205155694](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20210120205155694.png)

​	可以看到，不仅发生了粘包，还发生了拆包。