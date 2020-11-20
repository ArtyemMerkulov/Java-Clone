package com.geekbrains.cloud.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.stream.ChunkedWriteHandler;

public class Server {

    private static final String HOST = "localhost";
    private static final int PORT = 8189;

    private static final int BUF_SIZE = 131070;

    public static void main(String[] args) {
        new Server();
    }

    public Server() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.config().setSendBufferSize(BUF_SIZE);
                            socketChannel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(BUF_SIZE));
                            socketChannel.pipeline().addLast(new ServerMainHandler(), new ChunkedWriteHandler());
                        }
                    });
            ChannelFuture future = b.bind(HOST, PORT).sync();

            System.out.println("Server running...");

            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
