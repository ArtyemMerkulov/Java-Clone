package com.geekbrains.cloud.client;

import com.geekbrains.cloud.Command;
import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Type;
import com.sun.istack.internal.NotNull;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientMainHandler extends ChannelInboundHandlerAdapter {

    private static final int BUF_SIZE = 52428800; // 50 MB

    private ChannelHandlerContext ctx;

    private final ClientCloud clientCloud;

    private ByteBuf tmpBuf;

    public ClientMainHandler(ClientCloud clientCloud) {
        this.clientCloud = clientCloud;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        System.out.println("Handler added");
        this.ctx = ctx;
        tmpBuf = ctx.alloc().buffer(BUF_SIZE);
        clientCloud.setStart(true);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        System.out.println("Handler removed");
        tmpBuf.release();
        tmpBuf = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        tmpBuf.writeBytes((ByteBuf) msg);

        switch (Command.getCommandByValue(getCommandValue(tmpBuf))) {
            case REQUEST_CLOUD_TREE_STRUCTURE:
                List<FileDescription> remoteFilesList = getFilesDescriptionsFromRequest();
                clientCloud.changeCurrentRemoteDirectory(clientCloud.getActionFilePath(), remoteFilesList);

                clearBuffer(tmpBuf);
                clientCloud.setDirectoryStructureReceived(true);
                break;
            case REQUEST_DOWNLOAD_FILE:


                break;
        }

//        if (getCommand(tmpBuf) == 3 && tmpBuf.writerIndex() == getCommandLen(tmpBuf)) {
//            List<Path> remotePathsList = getPathsFromFolderTreeBytes(5);
//
//            clientCloud.setRemoteTreeStructure(remotePathsList);
//
//            clearBuffer(tmpBuf);
//        } else if (getCommand(tmpBuf) == 4 && tmpBuf.getByte(1) == 1) {
//            s += tmpBuf.readableBytes();
//            write();
//        } else if (getCommand(tmpBuf) == 4 && tmpBuf.getByte(1) == 2) {
//            s += tmpBuf.readableBytes();
//            write();
//            clientCloud.setFileReceived(true);
//        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        cause.printStackTrace();
    }

//    private void write() throws IOException {
//        Path fileName = clientCloud.getActionFilePath();
//
//        if (!Files.exists(fileName)) writeFilePart(fileName, StandardOpenOption.CREATE);
//        else writeFilePart(fileName, StandardOpenOption.APPEND);
//
//        clearBuffer(tmpBuf);
//    }

    private void writeFilePart(Path filePath, StandardOpenOption... option) throws IOException {
        byte[] buf = new byte[tmpBuf.readableBytes()];
        tmpBuf.getBytes(0, buf);
        Files.write(filePath, buf, option);
    }

    @NotNull
    private List<FileDescription> getFilesDescriptionsFromRequest() {
        List<FileDescription> pathsList = new ArrayList<>();

        tmpBuf.skipBytes(1);

        while (tmpBuf.readableBytes() > 0) {
            int pathLen = tmpBuf.readInt();
            Type type = Type.getTypeByValue(tmpBuf.readByte());

            byte[] pathBytes = new byte[pathLen];
            tmpBuf.readBytes(pathBytes, 0, pathLen);
            Path path = getPathFromBytes(pathBytes);

            pathsList.add(new FileDescription(path, type));
        }

        return pathsList;
    }

    @NotNull
    private Path getPathFromBytes(@NotNull byte[] bytes) {
        return Paths.get(new String(bytes, StandardCharsets.UTF_8));
    }

    private byte getCommandValue(@NotNull ByteBuf command) {
        return command.getByte(0);
    }

    @NotNull
    private int getCommandLen(@NotNull ByteBuf tmpBuf) {
        return tmpBuf.getInt(1);
    }

    private void clearBuffer(@NotNull ByteBuf buf) {
        buf.clear();
        buf.capacity(BUF_SIZE);
    }

    @NotNull
    public ChannelHandlerContext getCtx() {
        return ctx;
    }
}
