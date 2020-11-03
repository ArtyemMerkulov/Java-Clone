package com.geekbrains.cloud.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ClientMainHandler extends ChannelInboundHandlerAdapter {

    private ClientCloud clientCloud;

    private ByteBuf tmpBuf;

    public ClientMainHandler(ClientCloud clientCloud) {
        this.clientCloud = clientCloud;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Handler added");
        tmpBuf = ctx.alloc().buffer(1024);
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
            List<Path> pathsList = getPathsFromFolderTreeBytes(5);

            clientCloud.setTreeStructure(pathsList);

            clearBuffer(tmpBuf);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
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
        buf.capacity(1024);
    }
}
