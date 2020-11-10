package com.geekbrains.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ServerMainHandler extends ChannelInboundHandlerAdapter {

    private static final List<Channel> channels = new ArrayList<>();

    private static int BUF_SIZE = 10240;

    private static int newClientIndex = 1;

    private String clientName;

    private ServerCloud serverCloud;

    private ByteBuf tmpBuf;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Handler added");
        tmpBuf = ctx.alloc().buffer(10240);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Handler removed");
        tmpBuf.release();
        tmpBuf = null;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());

        clientName = "Клиент #" + newClientIndex;
        newClientIndex++;
        
        System.out.println("Клиент " + clientName + " подключился: " + ctx);

        serverCloud = new ServerCloud();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        tmpBuf.writeBytes((byte[]) msg);

        if (tmpBuf.isReadable()) {
            if (getCommand(tmpBuf) == Command.REQUEST_CLOUD_TREE_STRUCTURE.getValue()) {
                List<String> folderTreeStructure = serverCloud.getFolderTreeStructure()
                        .stream().map(Path::toString).collect(Collectors.toList());

                sendMsg(ctx, getMsgBytesFromListString(folderTreeStructure,
                        new byte[] {Command.REQUEST_CLOUD_TREE_STRUCTURE.getValue()}));

                tmpBuf.clear();
            } else if (getCommand(tmpBuf) == Command.REQUEST_DOWNLOAD_FILE.getValue()) {
                Path filePath = ServerCloud.getCloudPath().resolve(getFilePath(tmpBuf));
                tmpBuf.clear();

                if (Files.exists(filePath)) {
                    try(final FileChannel channel = new FileInputStream(filePath.toString()).getChannel()) {
                        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                        long bytesRead = 0, fileSize = channel.size();

                        while (bytesRead < fileSize) {
                            System.out.println("bytesRead: " + bytesRead + " fileSize: " + fileSize);
                            int bufSize = (int) ((fileSize - bytesRead) % BUF_SIZE);
                            byte[] downloadMsg = new byte[2 + (bufSize > 0 ? bufSize : BUF_SIZE)];

                            bytesRead += downloadMsg.length - 2;

                            downloadMsg[0] = 4;
                            downloadMsg[1] = (byte) (fileSize - bytesRead > 0 ? 1 : 2);

                            System.out.println("!: " + buffer.remaining());
                            System.out.println("!: " + (downloadMsg.length - 2));
                            buffer.get(downloadMsg, 2, downloadMsg.length - 2);
                            System.out.println("!: " + downloadMsg[0] + " " + downloadMsg[1]);

                            sendMsg(ctx, downloadMsg);
                        }
                    }
                }
            }
        }
    }

    private Path getFilePath(ByteBuf tmpBuf) {
        byte[] filePathBytes = new byte[tmpBuf.readableBytes() - 1];

        tmpBuf.getBytes(1, filePathBytes);

        return Paths.get(new String(filePathBytes));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Клиент " + clientName + " вышел из сети");
        closeChannel(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Клиент " + clientName + " отвалился");
        closeChannel(ctx);
        cause.printStackTrace();
    }

    private void closeChannel(ChannelHandlerContext ctx) {
        channels.remove(ctx.channel());
        ctx.close();
    }

    private byte[] getMsgBytesFromListString(List<String> stringList, byte[] commandBytes) {
        int intSize = 4, listOffset = 2, commandLen = commandBytes.length + intSize;
        List<ByteBuf> byteBuffers = new ArrayList<>();

        byteBuffers.add(ByteBufAllocator.DEFAULT.buffer().writeBytes(commandBytes));
        byteBuffers.add(ByteBufAllocator.DEFAULT.buffer().capacity(intSize));

        for (int i = listOffset; i < stringList.size() + listOffset; i++) {
            byte[] tmp = stringList.get(i - listOffset).getBytes();

            byteBuffers.add(ByteBufAllocator.DEFAULT.buffer().capacity(tmp.length + intSize));

            byteBuffers.get(i).writeInt(tmp.length);
            byteBuffers.get(i).writeBytes(tmp);

            commandLen += tmp.length + intSize;
        }
        byteBuffers.get(1).writeInt(commandLen);

        ByteBuf msgByteBuf = concat(byteBuffers);

        byte[] msgBytes = new byte[msgByteBuf.readableBytes()];
        msgByteBuf.readBytes(msgBytes);

        releaseByteBufList(byteBuffers);

        return msgBytes;
    }

    private void releaseByteBufList(List<ByteBuf> list) {
        for (ByteBuf byteBuf : list) byteBuf.release();
    }

    private ByteBuf concat(List<ByteBuf> buffers) {
        int length = 0;

        for (ByteBuf bb : buffers) length += bb.readableBytes();

        ByteBuf bbNew = ByteBufAllocator.DEFAULT.buffer().capacity(length);

        for (ByteBuf bb : buffers) bbNew.writeBytes(bb);

        return bbNew;
    }

    private byte getCommand(ByteBuf byteBuf) {
        return byteBuf.getByte(0);
    }

    private void sendMsg(ChannelHandlerContext ctx, byte[] msg) {
        ctx.channel().writeAndFlush(msg);
    }
}