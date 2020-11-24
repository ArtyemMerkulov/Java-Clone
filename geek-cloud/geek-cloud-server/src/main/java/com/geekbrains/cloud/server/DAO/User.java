package com.geekbrains.cloud.server.DAO;

public class User {

    private Integer id;

    private String userLogin;

    private String userPassword;

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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
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
