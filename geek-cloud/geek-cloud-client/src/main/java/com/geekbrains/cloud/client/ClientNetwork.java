package com.geekbrains.cloud.client;

import com.geekbrains.cloud.Command;
import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Utils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.file.Path;

public class ClientNetwork {

    //    private static final String HOST = "192.168.23.59";
    private static final String HOST = "localhost";
    private static final int PORT = 8189;

    private static SocketChannel channel;

    private static ClientCloud clientCloud = new ClientCloud();

    private ClientMainHandler clientMainHandler;

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
                                clientMainHandler = new ClientMainHandler(clientCloud);
                                socketChannel.pipeline().addLast(clientMainHandler);
                            }
                        });
                ChannelFuture future = b.connect(HOST, PORT).sync();
                getRemoteDirectoryTreeStructure(clientCloud.getCurrentRemoteDirectory());
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

    public void getRemoteDirectoryTreeStructure(FileDescription requestRoot) {
        channel.writeAndFlush(getRequestTreeStructureByteMsg(requestRoot));
    }

    private ByteBuf getRequestTreeStructureByteMsg(FileDescription requestRoot) {
        byte[] commandBytes = new byte[] {Command.REQUEST_CLOUD_TREE_STRUCTURE.getValue()};
        byte[] requestPathBytes = requestRoot.getPath().toString().getBytes();
        byte[] lenBytes = Utils.intToByteArray(requestPathBytes.length);

        return Unpooled.wrappedBuffer(Utils.concatAll(commandBytes, lenBytes, requestPathBytes));
    }

//    public void requestDownloadFile(FileDescription selectedObject, int lvl) {
//        Path target = ClientCloud.getFullPath(clientCloud.getRemoteTreeStructure(), selectedObject, lvl);
//        byte[] m = getDownloadMsg(target);
//        System.out.println(Arrays.toString(m));
//        channel.writeAndFlush(m);
//    }

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