package com.geekbrains.cloud.server;

import com.geekbrains.cloud.Command;
import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Type;
import com.geekbrains.cloud.Utils;
import com.google.common.primitives.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

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

    private static final List<Channel> channels = new ArrayList<>();

    private static final int BUF_SIZE = 131070;

    private static int newClientIndex = 1;

    private String clientName;

    private ServerCloud serverCloud;

    private ByteBuf tmpBuf;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        System.out.println("Handler added");
        tmpBuf = ctx.alloc().buffer(BUF_SIZE);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        System.out.println("Handler removed");
        tmpBuf.release();
        tmpBuf = null;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        channels.add(ctx.channel());

        clientName = "Клиент #" + newClientIndex;
        newClientIndex++;
        
        System.out.println("Клиент " + clientName + " подключился: " + ctx);

        serverCloud = new ServerCloud();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
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
                    // Clear buffer
                    tmpBuf.clear();
                    break;
                // Processing a client's file download request
                case REQUEST_DOWNLOAD_FILE:
                    Path filePath = ServerCloud.getCloudRootPath().resolve(getFilePath(tmpBuf));

                    tmpBuf.clear();

                    if (Files.exists(filePath)) {
                        try(FileChannel channel = (FileChannel) Files.newByteChannel(filePath, EnumSet.of(StandardOpenOption.READ))) {
                            MappedByteBuffer buffer;
                            ByteBuf unpooled;
                            long bytesRead = 0, fileSize = channel.size();

                            while(bytesRead < fileSize) {
                                long bytesLeft = fileSize - bytesRead;
                                int bufSize = bytesLeft / BUF_SIZE > 0 ? BUF_SIZE : (int) bytesLeft;
                                System.out.println("bytesLeft: " + bytesLeft);
                                System.out.println("bufSize: " + bufSize);

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
                        }
                    }

                    break;
            }
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
        System.out.println("Клиент " + clientName + " вышел из сети");
        closeChannel(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("Клиент " + clientName + " отвалился");
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
        channels.remove(ctx.channel());
        ctx.close();
    }
}