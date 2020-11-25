package com.geekbrains.cloud.server.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class JDBCUserDAO extends DAO<User> {

    private final Connection connection;

    public JDBCUserDAO(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int insert(User entity) {
        int rows = 0;
        String query = "INSERT INTO users_tbl (user_login_fld, user_password_fld) VALUES (?, ?);";

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);

            preparedStatement.setString(1, entity.getUserLogin());
            preparedStatement.setString(2, entity.getUserPassword());

            rows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return rows;
    }

    @Override
    public int update(User entity) {
        return 0;
    }

    @Override
    public int delete(User entity) {
        return 0;
    }

    @Override
    public List<User> getAll() {
        return null;
    }

    public boolean isUserLoginExist(String userLogin) {
        String query = "SELECT user_login_fld FROM users_tbl WHERE user_login_fld = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, userLogin);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }  catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public User getByUserLoginAndPassword(String userLogin, String userPassword) {
        String query = "SELECT user_id, user_login_fld, user_password_fld " +
                "FROM users_tbl " +
                "WHERE user_login_fld = ? AND user_password_fld = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, userLogin);
            preparedStatement.setString(2, userPassword);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new User(
                            resultSet.getInt(1),
                            resultSet.getString(2),
                            resultSet.getString(3)
                    );
                }
            }  catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
