package com.geekbrains.cloud.server;

import com.geekbrains.cloud.Command;
import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Type;
import com.geekbrains.cloud.Utils;
import com.google.common.primitives.Bytes;
import com.sun.istack.internal.NotNull;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ServerMainHandler extends ChannelInboundHandlerAdapter {

    private static final List<Channel> channels = new ArrayList<>();

    private static final int BUF_SIZE = 52428800; // 50 MB

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
                    System.out.println("RECEIVE: " + currDirectory);
                    List<FileDescription> directoryTreeStructure = serverCloud.changeCurrentDirectoryTreeStructure(currDirectory)
                            .getCurrentDirectoryTreeStructure();
                    // Generated a byte array with a response
                    directoryTreeStructure.forEach(System.out::println);
                    byte[] responseMsg = getMsgBytesFromFilesList(directoryTreeStructure,
                            Command.REQUEST_CLOUD_TREE_STRUCTURE);
                    sendMsg(ctx, responseMsg);
                    // Clear buffer
                    tmpBuf.clear();
                    break;
                // Processing a client's file download request
                case REQUEST_DOWNLOAD_FILE:
                    Path filePath = ServerCloud.getCloudRootPath().resolve(getFilePath(tmpBuf));

                    tmpBuf.clear();

                    if (Files.exists(filePath)) {
                        try(final FileChannel channel = new FileInputStream(filePath.toString()).getChannel()) {
                            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0,
                                    channel.size());
                            long bytesRead = 0, fileSize = channel.size();

                            while (bytesRead < fileSize) {
                                int bufSize = (int) ((fileSize - bytesRead) % BUF_SIZE);
                                byte[] downloadMsg = new byte[2 + (bufSize > 0 ? bufSize : BUF_SIZE)];

                                bytesRead += downloadMsg.length - 2;

                                downloadMsg[0] = Command.REQUEST_DOWNLOAD_FILE.getValue();
                                downloadMsg[1] = (byte) (fileSize - bytesRead > 0 ? 1 : 2);

                                buffer.get(downloadMsg, 2, downloadMsg.length - 2);

                                sendMsg(ctx, downloadMsg);
                            }
                        }
                    }

                    break;
            }
        }
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

    private byte[] getMsgBytesFromFilesList(List<FileDescription> filesList, Command command) {
        List<byte[]> msgBytesParts = new ArrayList<>();

        msgBytesParts.add(new byte[] {command.getValue()});
        for (int i = 0; i < filesList.size(); i++) {
            byte[] strBytes = filesList.get(i).getPath().toString().getBytes();
            byte[] typeBytes = new byte[] {filesList.get(i).getType().getValue()};
            byte[] sizeBytes = Utils.intToByteArray(strBytes.length);

            byte[] msgBytesPart = Utils.concatAll(sizeBytes, typeBytes, strBytes);

            msgBytesParts.add(msgBytesPart);
        }

        return Bytes.toArray(
                msgBytesParts.stream()
                        .map(Bytes::asList)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList())
        );
    }

    private byte getCommandValue(ByteBuf byteBuf) {
        return byteBuf.getByte(0);
    }

    private void sendMsg(ChannelHandlerContext ctx, byte[] msg) {
        ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(msg));
    }

    private void closeChannel(ChannelHandlerContext ctx) {
        channels.remove(ctx.channel());
        ctx.close();
    }
}