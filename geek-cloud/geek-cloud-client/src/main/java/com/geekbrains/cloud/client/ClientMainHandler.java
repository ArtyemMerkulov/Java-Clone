package com.geekbrains.cloud.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientMainHandler extends ChannelInboundHandlerAdapter {

    private static final int BUF_SIZE = 10240;

    private ClientCloud clientCloud;

    private ByteBuf tmpBuf;

    private long s = 0;

    public ClientMainHandler(ClientCloud clientCloud) {
        this.clientCloud = clientCloud;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Handler added");
        tmpBuf = ctx.alloc().buffer(BUF_SIZE);
        clientCloud.setStart(true);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Handler removed");
        tmpBuf.release();
        tmpBuf = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        tmpBuf.writeBytes((byte[]) msg);

        if (getCommand(tmpBuf) == 3 && tmpBuf.writerIndex() == getCommandLen(tmpBuf)) {
            List<Path> remotePathsList = getPathsFromFolderTreeBytes(5);

            clientCloud.setRemoteTreeStructure(remotePathsList);

            clearBuffer(tmpBuf);
        } else if (getCommand(tmpBuf) == 4 && tmpBuf.getByte(1) == 1) {
            s += tmpBuf.readableBytes();
            write();
        } else if (getCommand(tmpBuf) == 4 && tmpBuf.getByte(1) == 2) {
            s += tmpBuf.readableBytes();
            write();
            clientCloud.setFileReceived(true);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }

    private void write() throws IOException {
        Path fileName = clientCloud.getActionFilePath();

        if (!Files.exists(fileName)) writeFilePart(fileName, StandardOpenOption.CREATE);
        else writeFilePart(fileName, StandardOpenOption.APPEND);

        System.out.println(s);

        clearBuffer(tmpBuf);
    }

    private void writeFilePart(Path filePath, StandardOpenOption... option) throws IOException {
        byte[] buf = new byte[tmpBuf.readableBytes()];
        tmpBuf.getBytes(0, buf);
        Files.write(filePath, buf, option);
    }

    private List<Path> getPathsFromFolderTreeBytes(int offset) {
        List<Path> pathsList = new ArrayList<>();

        tmpBuf.skipBytes(offset);

        while (tmpBuf.readableBytes() > 0) {
            int len = tmpBuf.readInt();

            byte[] tmpMsg = new byte[len];
            tmpBuf.readBytes(tmpMsg, 0, len);

            pathsList.add(getPathFromFolderTreeBytes(tmpMsg));
        }

        return pathsList;
    }

    private Path getPathFromFolderTreeBytes(byte[] bytes) {
        return Paths.get(new String(bytes, StandardCharsets.UTF_8));
    }

    private byte getCommand(ByteBuf commandMsg) {
        return commandMsg.getByte(0);
    }

    private int getCommandLen(ByteBuf tmpBuf) {
        return tmpBuf.getInt(1);
    }

    private void clearBuffer(ByteBuf buf) {
        buf.clear();
        buf.capacity(BUF_SIZE);
    }
}
