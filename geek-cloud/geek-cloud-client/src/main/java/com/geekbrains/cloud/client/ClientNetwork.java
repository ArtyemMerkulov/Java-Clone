package com.geekbrains.cloud.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

public class ClientNetwork {

    //    private static final String HOST = "192.168.23.59";
    private static final String HOST = "localhost";
    private static final int PORT = 8189;

    private static SocketChannel channel;

    private static ClientCloud clientCloud = new ClientCloud();

    public ClientNetwork() {
        Thread t = new Thread(() -> {
            NioEventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(workerGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel) {
                                channel = socketChannel;
                                socketChannel.pipeline().addLast(
                                        new ByteArrayEncoder(),
                                        new ByteArrayDecoder(),
                                        new ClientMainHandler(clientCloud)
                                );
                            }
                        });
                ChannelFuture future = b.connect(HOST, PORT).sync();

                getFolderTreeStructure();

                future.channel().closeFuture().sync();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                workerGroup.shutdownGracefully();
            }
        });
        t.start();
    }

    public ClientCloud getClientCloud() {
        return clientCloud;
    }

    public void getFolderTreeStructure() {
        channel.writeAndFlush(new byte[] {3});
    }

    public void close() {
        channel.close();
    }

}