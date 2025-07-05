package com.simpleqq.common;

/**
 * 消息类型枚举
 * 定义了系统中所有可能的消息类型，用于客户端和服务器之间的通信协议
 */
public enum MessageType {
    // 用户认证相关消息类型
    LOGIN,                    // 用户登录请求
    REGISTER,                 // 用户注册请求
    LOGIN_SUCCESS,            // 登录成功响应
    LOGIN_FAIL,               // 登录失败响应
    REGISTER_SUCCESS,         // 注册成功响应
    REGISTER_FAIL,            // 注册失败响应
    
    // 消息传输相关类型
    TEXT_MESSAGE,             // 文本消息
    IMAGE_MESSAGE,            // 图片消息
    GROUP_MESSAGE,            // 群组消息
    
    // 好友管理相关类型
    FRIEND_REQUEST,           // 好友请求
    FRIEND_ACCEPT,            // 接受好友请求
    FRIEND_REJECT,            // 拒绝好友请求
    DELETE_FRIEND,            // 删除好友请求
    FRIEND_LIST,              // 获取好友列表
    ADD_FRIEND_SUCCESS,       // 添加好友成功
    ADD_FRIEND_FAIL,          // 添加好友失败
    DELETE_FRIEND_SUCCESS,    // 删除好友成功
    DELETE_FRIEND_FAIL,       // 删除好友失败
    
    // 群组管理相关类型
    GROUP_INVITE,             // 群组邀请
    GROUP_ACCEPT,             // 接受群组邀请
    GROUP_REJECT,             // 拒绝群组邀请
    CREATE_GROUP,             // 创建群组请求
    CREATE_GROUP_SUCCESS,     // 创建群组成功
    CREATE_GROUP_FAIL,        // 创建群组失败
    GET_GROUPS,               // 获取用户群组列表
    GET_GROUP_MEMBERS,        // 获取群组成员列表
    GROUP_JOIN_SUCCESS,       // 加入群组成功
    GROUP_JOIN_FAIL,          // 加入群组失败
    
    // 系统消息类型
    SERVER_MESSAGE,           // 服务器系统消息
    GET_PENDING_REQUESTS      // 获取待处理请求列表
}