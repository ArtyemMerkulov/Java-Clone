package com.geekbrains.cloud.server;

import com.geekbrains.cloud.Command;
import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Type;
import com.geekbrains.cloud.Utils;
import com.google.common.primitives.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import sun.misc.Cleaner;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class ServerMainHandler extends ChannelInboundHandlerAdapter {

    private static final int BUF_SIZE = 131070;

    private ServerCloud serverCloud;

    private ByteBuf tmpBuf;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        tmpBuf = ctx.alloc().buffer(BUF_SIZE);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        tmpBuf.release();
        tmpBuf = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ServerCloud) {
            serverCloud = (ServerCloud) msg;
        } else if (msg instanceof ByteBuf) {
            tmpBuf.writeBytes((ByteBuf) msg);

            if (tmpBuf.isReadable() && getCommandValue(tmpBuf) <= Command.maxValue()) {
                // Got a request command
                switch (Command.getCommandByValue(getCommandValue(tmpBuf))) {
                    // Handling a client request for a directory structure
                    case REQUEST_CLOUD_TREE_STRUCTURE:
                        // Extract request path and make file description
                        Path currDirectoryPath = getRequestCurrentDirectory(tmpBuf);
                        FileDescription currDirectory = new FileDescription(currDirectoryPath, Type.DIRECTORY);
                        // Got a list of files contained in this path
                        List<FileDescription> directoryTreeStructure = serverCloud.changeCurrentDirectoryTreeStructure(currDirectory)
                                .getCurrentDirectoryTreeStructure();
                        // Generated a byte array with a response
                        ByteBuf responseMsg = getMsgBytesFromFilesList(directoryTreeStructure, Command.REQUEST_CLOUD_TREE_STRUCTURE);
                        sendMsg(ctx, responseMsg);

                        break;
                    // Processing a client's file download request
                    case REQUEST_DOWNLOAD_FILE:
                        Path filePath = ServerCloud.getCloudRootPath().resolve(getFilePath(tmpBuf));

                        tmpBuf.clear();

                        if (Files.exists(filePath)) {
                            MappedByteBuffer buffer = null;
                            ByteBuf unpooled;

                            try (FileChannel channel = (FileChannel) Files.newByteChannel(filePath,
                                    EnumSet.of(StandardOpenOption.READ))) {
                                long bytesRead = 0, fileSize = channel.size();

                                while(bytesRead < fileSize) {
                                    long bytesLeft = fileSize - bytesRead;
                                    int bufSize = bytesLeft / BUF_SIZE > 0 ? BUF_SIZE : (int) bytesLeft;

                                    buffer = channel.map(FileChannel.MapMode.READ_ONLY, bytesRead, bufSize);
                                    unpooled = Unpooled.buffer(bufSize + 1);

                                    bytesRead += bufSize;

                                    unpooled.setByte(0, (bufSize == BUF_SIZE ? Command.RECEIVE_PART_OF_DOWNLOAD_FILE.getValue() :
                                            Command.RECEIVE_END_PART_OF_DOWNLOAD_FILE.getValue()));
                                    unpooled.writerIndex(1);
                                    unpooled.writeBytes(buffer);

                                    sendMsg(ctx, unpooled);

                                    buffer.clear();
                                    clearBuffer(unpooled);
                                }
                            } finally {
                                assert buffer != null;
                                Cleaner cleaner = ((sun.nio.ch.DirectBuffer) buffer).cleaner();
                                if (cleaner != null) {
                                    cleaner.clean();
                                }
                            }
                        }

                        break;
                    // Processing a client's file upload request
                    case RECEIVE_UPLOAD_FILE_DESCRIPTION:
                        serverCloud.setActionFile(getUploadFileDescription());

                        sendMsg(ctx, getReceiveUploadFileDescriptionMsg());

                        break;
                    case RECEIVE_PART_OF_UPLOAD_FILE:
                        writeFilePart(serverCloud.getActionFile(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                        break;
                    case RECEIVE_END_PART_OF_UPLOAD_FILE:
                        writeFilePart(serverCloud.getActionFile(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                        serverCloud.setActionFile(null);

                        sendMsg(ctx, getReceiveUploadFileEndMsg());
                        // Extract current root directory
                        currDirectory = serverCloud.getCurrentRoot();
                        // Got a list of files contained in this path
                        directoryTreeStructure = serverCloud.changeCurrentDirectoryTreeStructure(currDirectory)
                                .getCurrentDirectoryTreeStructure();
                        // Generated a byte array with a response
                        ByteBuf updateDirStructureBeforeUploadFileMsg = getMsgBytesFromFilesList(directoryTreeStructure,
                                Command.REQUEST_CLOUD_TREE_STRUCTURE);
                        sendMsg(ctx, updateDirStructureBeforeUploadFileMsg);

                        break;
                }
            }

            resetBuffer();
        }
    }

    private void resetBuffer() {
        clearBuffer();
        tmpBuf = Unpooled.buffer(BUF_SIZE);
    }

    private void clearBuffer() {
        tmpBuf.clear();
        tmpBuf.release();
        tmpBuf = null;
    }

    private FileDescription getUploadFileDescription() {
        tmpBuf.skipBytes(1);

        Type fileType = Type.getTypeByValue(tmpBuf.readByte());

        int nameLen = tmpBuf.readInt();
        byte[] nameBytes = new byte[nameLen];

        tmpBuf.readBytes(nameBytes, 0, nameLen);

        Path fileName = Paths.get(new String(nameBytes, StandardCharsets.UTF_8));

        return new FileDescription(fileName, fileType);
    }

    private ByteBuf getReceiveUploadFileDescriptionMsg() {
        byte[] commandBytes = new byte[] {Command.UPLOAD_FILE_DESCRIPTION_RECEIVED.getValue()};

        return Unpooled.wrappedBuffer(commandBytes);
    }

    private ByteBuf getReceiveUploadFileEndMsg() {
        byte[] commandBytes = new byte[] {Command.UPLOAD_FILE_RECEIVED.getValue()};

        return Unpooled.wrappedBuffer(commandBytes);
    }

    private void writeFilePart(FileDescription file, StandardOpenOption... option) {
        tmpBuf.skipBytes(1);

        byte[] buf = new byte[tmpBuf.readableBytes()];
        tmpBuf.readBytes(buf);

        try {
            Files.write(ServerCloud.getCloudRootPath().resolve(file.getPath()), buf, option);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearBuffer(ByteBuf buf) {
        buf.clear();
    }

    private Path getRequestCurrentDirectory(ByteBuf tmpBuf) {
        int len = tmpBuf.getInt(1);
        byte[] stringPathBytes = new byte[len];

        tmpBuf.skipBytes(5);
        tmpBuf.readBytes(stringPathBytes);

        return Paths.get(new String(stringPathBytes, StandardCharsets.UTF_8));
    }

    private Path getFilePath(ByteBuf tmpBuf) {
        byte[] filePathBytes = new byte[tmpBuf.readableBytes() - 1];

        tmpBuf.getBytes(1, filePathBytes);

        return Paths.get(new String(filePathBytes));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        closeChannel(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        closeChannel(ctx);
        cause.printStackTrace();
    }

    private ByteBuf getMsgBytesFromFilesList(List<FileDescription> filesList, Command command) {
        List<byte[]> msgBytesParts = new ArrayList<>();

        msgBytesParts.add(new byte[] {command.getValue()});
        for (FileDescription fileDescription : filesList) {
            byte[] strBytes = fileDescription.getPath().toString().getBytes();
            byte[] typeBytes = new byte[]{fileDescription.getType().getValue()};
            byte[] sizeBytes = Utils.intToByteArray(strBytes.length);

            byte[] msgBytesPart = Utils.concatAll(sizeBytes, typeBytes, strBytes);

            msgBytesParts.add(msgBytesPart);
        }

        return Unpooled.wrappedBuffer(
                Bytes.toArray(msgBytesParts.stream()
                        .map(Bytes::asList)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()))
        );
    }

    private byte getCommandValue(ByteBuf byteBuf) {
        return byteBuf.getByte(0);
    }

    private void sendMsg(ChannelHandlerContext ctx, ByteBuf msg) {
        ctx.writeAndFlush(msg);
    }

    private void closeChannel(ChannelHandlerContext ctx) {
        ctx.close();
    }
}