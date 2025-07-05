package com.simpleqq.common;

import java.io.Serializable;

/**
 * 用户实体类
 * 封装用户的基本信息和状态
 * 实现Serializable接口以支持对象序列化
 */
public class User implements Serializable {
    private String id;           // 用户唯一标识ID
    private String username;     // 用户显示名称
    private String password;     // 用户登录密码
    private boolean isOnline;    // 用户在线状态标志

    /**
     * 构造函数
     * @param id 用户ID，必须唯一
     * @param username 用户名，用于显示
     * @param password 用户密码，用于登录验证
     */
    public User(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.isOnline = false; // 默认离线状态
    }

    // Getter方法
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

    /**
     * 设置用户在线状态
     * @param online true表示在线，false表示离线
     */
    public void setOnline(boolean online) {
        isOnline = online;
    }

    /**
     * 重写toString方法，便于调试和日志输出
     */
    @Override
    public String toString() {
        return "User{" +
               "id='" + id + '\'' +
               ", username='" + username + '\'' +
               ", password='" + password + '\'' +
               ", isOnline=" + isOnline +
               '}';
    }
}