package com.geekbrains.cloud.server.DAO;

public class User {

    private final Integer id;

    private final String userLogin;

    private final String userPassword;

    public User(String userLogin, String userPassword) {
        this.id = null;
        this.userLogin = userLogin;
        this.userPassword = userPassword;
    }

    public User(Integer id, String userLogin, String userPassword) {
        this.id = id;
        this.userLogin = userLogin;
        this.userPassword = userPassword;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public String getUserPassword() {
        return userPassword;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userLogin='" + userLogin + '\'' +
                ", userPassword='" + userPassword + '\'' +
                '}';
    }
}
