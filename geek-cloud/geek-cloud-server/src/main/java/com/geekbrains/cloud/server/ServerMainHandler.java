package com.geekbrains.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ServerMainHandler extends ChannelInboundHandlerAdapter {

    private static final List<Channel> channels = new ArrayList<>();

    private static int newClientIndex = 1;

    private String clientName;

    private ServerCloud serverCloud;

    private ByteBuf tmpBuf;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Handler added");
        tmpBuf = ctx.alloc().buffer();
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
            if (getCommand(tmpBuf) == 3) {
                // Высылаем сведения о строении хранилища
                List<String> folderTreeStructure = serverCloud.getFolderTreeStructure()
                        .stream().map(Path::toString).collect(Collectors.toList());
                folderTreeStructure.forEach(System.out::println);
                sendMsg(ctx, getMsgBytesFromListString(folderTreeStructure, new byte[] {3}));
            }
        }
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