package com.geekbrains.cloud.server.DAO;

import java.sql.Connection;

public abstract class DAOFactory<T> {

    public abstract DAO<T> getDAO(Connection connection, DAOType type);
}
