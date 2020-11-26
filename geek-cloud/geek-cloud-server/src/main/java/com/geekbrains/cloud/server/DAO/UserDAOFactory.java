package com.geekbrains.cloud.server.DAO;

import java.sql.Connection;

public class UserDAOFactory extends DAOFactory<User> {

    @Override
    public DAO<User> getDAO(Connection connection, DAOType type) {
        switch (type) {
            case JDBCUserDAO:
                return new JDBCUserDAO(connection);
        }

        return null;
    }
}
