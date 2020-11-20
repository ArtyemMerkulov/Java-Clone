package com.geekbrains.cloud.client;

import com.geekbrains.cloud.Command;
import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Type;
import com.sun.istack.internal.NotNull;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class ClientMainHandler extends ChannelInboundHandlerAdapter {

    private static final int BUF_SIZE = 131071;

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
        clearBuffer();
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        tmpBuf.writeBytes((ByteBuf) msg);

        switch (Command.getCommandByValue(getCommandValue(tmpBuf))) {
            case REQUEST_CLOUD_TREE_STRUCTURE:
                List<FileDescription> remoteFilesList = getFilesDescriptionsFromRequest();

                clientCloud.changeCurrentRemoteDirectory(clientCloud.getActionFile(), remoteFilesList);
                clientCloud.setDirectoryStructureReceived(true);

                break;
            case RECEIVE_PART_OF_DOWNLOAD_FILE:
                writeFilePart(clientCloud.getActionFile(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                break;
            case RECEIVE_END_PART_OF_DOWNLOAD_FILE:
                writeFilePart(clientCloud.getActionFile(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                clientCloud.setFileReceived(true);
                break;
        }

        resetBuffer();

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
//        Path fileName = clientCloud.getActionFile();
//
//        if (!Files.exists(fileName)) writeFilePart(fileName, StandardOpenOption.CREATE);
//        else writeFilePart(fileName, StandardOpenOption.APPEND);
//
//        clearBuffer(tmpBuf);
//    }

    private void writeFilePart(FileDescription file, StandardOpenOption... option) {
        tmpBuf.skipBytes(1);

        byte[] buf = new byte[tmpBuf.readableBytes()];
        tmpBuf.readBytes(buf);

        try {
            Files.write(clientCloud.getCurrentLocalDirectory().getPath().resolve(file.getFileName()), buf, option);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private void resetBuffer() {
        clearBuffer();
        tmpBuf = ctx.alloc().buffer(BUF_SIZE);
    }

    private void clearBuffer() {
        tmpBuf.clear();
        tmpBuf.release();
        tmpBuf = null;
    }
}
