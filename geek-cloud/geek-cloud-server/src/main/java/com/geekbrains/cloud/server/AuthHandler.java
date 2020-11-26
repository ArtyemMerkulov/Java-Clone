package com.geekbrains.cloud.server;

import com.geekbrains.cloud.Command;
import com.geekbrains.cloud.server.DAO.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private static final int BUF_SIZE = 131070;

    private static final List<Channel> channels = new ArrayList<>();

    private final ConnectionPool connectionPool;

    private final Connection connection;

    private final JDBCUserDAO jdbcUserDAO;

    private ChannelHandlerContext ctx;

    private ByteBuf tmpBuf;

    private boolean isAuthorized = false;

    public AuthHandler(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
        this.connection = connectionPool.getConnection();
        this.jdbcUserDAO = (JDBCUserDAO) new UserDAOFactory().getDAO(connection, DAOType.JDBCUserDAO);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        tmpBuf = ctx.alloc().buffer(BUF_SIZE);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        tmpBuf.writeBytes((ByteBuf) msg);

        if (isAuthorized) {
            passToNextHandler(tmpBuf);
        } else if (tmpBuf.isReadable() && getCommandValue(tmpBuf) <= Command.maxValue()) {
            switch (Command.getCommandByValue(getCommandValue(tmpBuf))) {
                case AUTH_DATA:
                    User recvUser = getUserData(tmpBuf);
                    User authUser = jdbcUserDAO.getByUserLoginAndPassword(
                            recvUser.getUserLogin(),
                            recvUser.getUserPassword()
                    );

                    if (authUser != null) {
                        isAuthorized = true;
                        channels.add(ctx.channel());

                        ServerCloud serverCloud = new ServerCloud(authUser.getUserLogin());
                        passToNextHandler(serverCloud);

                        sendMsg(getAuthMsg(Command.AUTH_OK));
                    } else {
                        sendMsg(getAuthMsg(Command.AUTH_NOT_OK));
                    }

                    break;
                case REGISTRATION_DATA:
                    User regUser = getUserData(tmpBuf);

                    boolean isUserLoginExist = jdbcUserDAO.isUserLoginExist(regUser.getUserLogin());

                    if (!isUserLoginExist) {
                        jdbcUserDAO.insert(regUser);

                        Path userDir = ServerCloud.getUserStoragePath()
                                .resolve(Paths.get(regUser.getUserLogin()));

                        Files.createDirectory(userDir);

                        WatchService watchService = FileSystems.getDefault().newWatchService();
                        userDir.register(watchService, ENTRY_DELETE);

                        sendMsg(getAuthMsg(Command.REGISTRATION_OK));
                    } else {
                        sendMsg(getAuthMsg(Command.REGISTRATION_NOT_OK));
                    }

                    break;
            }
        }

        resetBuffer();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        closeConnection();
    }

    private void passToNextHandler(Object obj) {
        ctx.fireChannelRead(obj);
    }

    private User getUserData(ByteBuf tmpBuf) {
        tmpBuf.skipBytes(1);

        byte[] userPasswordBytes = new byte[32];
        tmpBuf.readBytes(userPasswordBytes);

        byte[] userLoginBytes = new byte[tmpBuf.readableBytes()];
        tmpBuf.readBytes(userLoginBytes);

        String userLogin = getStringFromBytes(userLoginBytes);
        String userPassword = getStringFromBytes(userPasswordBytes);

        return new User(userLogin, userPassword);
    }

    private ByteBuf getAuthMsg(Command command) {
        ByteBuf msg = Unpooled.buffer();
        msg.writeByte(command.getValue());

        return msg;
    }

    private void sendMsg(ByteBuf msg) {
        ctx.writeAndFlush(msg);
    }

    private String getStringFromBytes(byte[] arr) {
        return new String(arr, StandardCharsets.UTF_8);
    }

    private byte getCommandValue(ByteBuf byteBuf) {
        return byteBuf.getByte(0);
    }

    private void closeConnection() {
        isAuthorized = false;
        clearBuffer();
        channels.remove(ctx.channel());
        connectionPool.returnConnectionInPool(connection);
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
}
