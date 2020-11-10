package com.geekbrains.cloud.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

import java.nio.file.Path;
import java.util.Arrays;

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
                getRemoteFolderTreeStructure();
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                workerGroup.shutdownGracefully();
            }
        });
        t.start();
    }

    public void close() {
        channel.close();
    }

    public ClientCloud getClientCloud() {
        return clientCloud;
    }

    public void getRemoteFolderTreeStructure() {
        channel.writeAndFlush(new byte[] {3});
    }

    public void requestDownloadFile(FileDescription selectedObject, int lvl) {
        Path target = ClientCloud.getFullPath(clientCloud.getRemoteTreeStructure(), selectedObject, lvl);
        byte[] m = getDownloadMsg(target);
        System.out.println(Arrays.toString(m));
        channel.writeAndFlush(m);
    }

    public void requestUploadFile(FileDescription selectedObject, int lvl) {
        channel.writeAndFlush(new byte[] {3});
    }

    private byte[] getDownloadMsg(Path target) {
        byte[] downloadFlag = new byte[] {4};
        byte[] targetPathBytes = target.toString().getBytes();

        return Utils.concatAll(downloadFlag, targetPathBytes);
    }

    private byte[] getUploadMsg(Path target) {
        byte[] downloadFlag = new byte[] {4};
        byte[] targetPathBytes = target.toString().getBytes();
        return Utils.concatAll(downloadFlag, targetPathBytes);
    }
}