package com.geekbrains.cloud.server;

import com.geekbrains.cloud.server.DAO.ConnectionPool;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public final class Server {

    private static final String HOST = "localhost";

    private static final int PORT = 8189;

    private static final int BUF_SIZE = 131070;

    private static final ConnectionPool connectionPool = new ConnectionPool();

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
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.config().setSendBufferSize(BUF_SIZE);
                            socketChannel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(BUF_SIZE));

                            socketChannel.pipeline().addLast("authHandler", new AuthHandler(connectionPool));
                            socketChannel.pipeline().addLast("ServerMainHandler", new ServerMainHandler());
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
