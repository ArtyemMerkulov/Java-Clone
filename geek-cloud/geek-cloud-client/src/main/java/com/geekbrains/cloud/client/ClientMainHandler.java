package com.geekbrains.cloud.client;

import com.geekbrains.cloud.Command;
import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Type;
import com.geekbrains.cloud.Utils;
import com.sun.istack.internal.NotNull;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
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
        this.ctx = ctx;
        tmpBuf = ctx.alloc().buffer(BUF_SIZE);

        clientCloud.setIsStart(true);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        clearBuffer();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        tmpBuf.writeBytes((ByteBuf) msg);

        if (tmpBuf.isReadable() && getCommandValue(tmpBuf) <= Command.maxValue()) {
            switch (Command.getCommandByValue(getCommandValue(tmpBuf))) {
                case AUTH_OK:
                    clientCloud.setAuthorized(Command.AUTH_OK);
                    getRemoteDirectoryTreeStructure(clientCloud.getCurrentRemoteDirectory());
                    break;
                case AUTH_NOT_OK:
                    clientCloud.setAuthorized(Command.AUTH_NOT_OK);
                    break;
                case REGISTRATION_OK:
                    clientCloud.setRegistrationMessage(Command.REGISTRATION_OK);
                    break;
                case REGISTRATION_NOT_OK:
                    clientCloud.setRegistrationMessage(Command.REGISTRATION_NOT_OK);
                    break;
                case REQUEST_CLOUD_TREE_STRUCTURE:
                    if (clientCloud.isAuthorized() == 1) {
                        List<FileDescription> remoteFilesList = getFilesDescriptionsFromRequest();

                        clientCloud.changeCurrentRemoteDirectory(clientCloud.getActionFile(), remoteFilesList);
                        clientCloud.setDirectoryStructureReceived(true);
                    }
                    break;
                case RECEIVE_PART_OF_DOWNLOAD_FILE:
                    if (clientCloud.isAuthorized() == 1) {
                        writeFilePart(clientCloud.getActionFile(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }
                    break;
                case RECEIVE_END_PART_OF_DOWNLOAD_FILE:
                    if (clientCloud.isAuthorized() == 1) {
                        writeFilePart(clientCloud.getActionFile(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        clientCloud.setFileReceived(true);
                    }
                    break;
                case UPLOAD_FILE_DESCRIPTION_RECEIVED:
                    if (clientCloud.isAuthorized() == 1) {
                        sendUploadFileData(clientCloud.getActionFile());
                    }
                    break;
                case UPLOAD_FILE_RECEIVED:
                    if (clientCloud.isAuthorized() == 1) {
                        clientCloud.setFileSent(true);
                    }
                    break;
            }
        }

        resetBuffer();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        cause.printStackTrace();
    }

    public void getRemoteDirectoryTreeStructure(FileDescription requestRoot) {
        ctx.writeAndFlush(getRequestTreeStructureByteMsg(requestRoot));
    }

    private ByteBuf getRequestTreeStructureByteMsg(FileDescription requestRoot) {
        byte[] commandBytes = new byte[]{Command.REQUEST_CLOUD_TREE_STRUCTURE.getValue()};
        byte[] requestPathBytes = requestRoot.getPath().toString().getBytes();
        byte[] lenBytes = Utils.intToByteArray(requestPathBytes.length);

        byte[] msg = Utils.concatAll(commandBytes, lenBytes, requestPathBytes);

        return Unpooled.wrappedBuffer(msg);
    }

    public void sendUploadFileData(FileDescription selectedObject) {
        int maxBufSize = BUF_SIZE - 2;
        Path filePath = clientCloud.getCurrentLocalDirectory().getPath().resolve(selectedObject.getPath());

        if (Files.exists(filePath)) {
            try(FileChannel channel = (FileChannel) Files.newByteChannel(filePath, EnumSet.of(StandardOpenOption.READ))) {
                MappedByteBuffer buffer;
                ByteBuf unpooled;
                long bytesRead = 0, fileSize = channel.size();

                while(bytesRead < fileSize) {
                    long bytesLeft = fileSize - bytesRead;
                    int bufSize = bytesLeft / maxBufSize > 0 ? maxBufSize : (int) bytesLeft;

                    buffer = channel.map(FileChannel.MapMode.READ_ONLY, bytesRead, bufSize);
                    unpooled = Unpooled.buffer(bufSize + 1);

                    bytesRead += bufSize;

                    unpooled.setByte(0, (bufSize == maxBufSize ? Command.RECEIVE_PART_OF_UPLOAD_FILE.getValue() :
                            Command.RECEIVE_END_PART_OF_UPLOAD_FILE.getValue()));
                    unpooled.writerIndex(1);
                    unpooled.writeBytes(buffer);

                    sendMsg(unpooled);

                    buffer.clear();
                    unpooled.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMsg(ByteBuf msg) {
        ctx.writeAndFlush(msg);
    }

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
