package com.geekbrains.cloud.server.DAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ConnectionPool {

    private static final int initialCapacity = 10;

    private static final String connectionString = "jdbc:sqlite:geek-cloud-server/src/main/resources/DB/cloud_db.db";

    private final List<Connection> connectionPool;

    public ConnectionPool() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        connectionPool = new ArrayList<>(initialCapacity);

        for (int i = 0; i < initialCapacity; i++)
            connectionPool.add(createConnection());
    }

    public Connection getConnection() {
        if (connectionPool.size() >= 1) {
            Connection connection = connectionPool.get(connectionPool.size() - 1);

            removeConnectionFromPool(connection);

            return connection;
        }

        return null;
    }

    public void returnConnectionInPool(Connection connection) {
        connectionPool.add(connection);
    }

    private Connection createConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(connectionString);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return connection;
    }

    private void removeConnectionFromPool(Connection connection) {
        connectionPool.remove(connection);
    }
}
