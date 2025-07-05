
package com.simpleqq.common;

import java.io.Serializable;

public class User implements Serializable {
    private String id;
    private String username;
    private String password;
    private boolean isOnline;

    public User(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.isOnline = false;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    @Override
    public String toString() {
        return "User{" +
               "id=\'" + id + '\'' +
               ", username=\'" + username + '\'' +
               ", password=\'" + password + '\'' +
               ", isOnline=" + isOnline +
               '}';
    }
}

