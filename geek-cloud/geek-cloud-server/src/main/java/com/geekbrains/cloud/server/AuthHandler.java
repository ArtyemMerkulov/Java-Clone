package com.geekbrains.cloud.server;

import com.geekbrains.cloud.Command;
import com.geekbrains.cloud.server.DAO.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

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
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
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
                    User user = getUserData(tmpBuf);
                    User authUser = jdbcUserDAO.getByUserLoginAndPassword(
                            user.getUserLogin(),
                            user.getUserPassword()
                    );

                    System.out.println(user);
                    System.out.println(authUser);

                    if (authUser != null) {
                        sendMsg(getAuthMsg(Command.AUTH_OK));
                        isAuthorized = true;
                        channels.add(ctx.channel());
                    } else {
                        sendMsg(getAuthMsg(Command.AUTH_NOT_OK));
                    }

                    break;
                case REGISTRATION_DATA:
                    User regUser = getUserData(tmpBuf);
                    System.out.println("REG: " + regUser);

                    int isRegistered = jdbcUserDAO.insert(regUser);

                    if (isRegistered == 1) {
                        sendMsg(getAuthMsg(Command.REGISTRATION_OK));
                    } else if (isRegistered == 0) {
                        sendMsg(getAuthMsg(Command.REGISTRATION_NOT_OK));
                    }

                    break;
            }
        }

        tmpBuf.clear();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        closeConnection();
    }

    private void passToNextHandler(ByteBuf buf) {
        ctx.fireChannelRead(buf);
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
        tmpBuf.release();
        channels.remove(ctx.channel());
        connectionPool.returnConnectionInPool(connection);
    }
}
