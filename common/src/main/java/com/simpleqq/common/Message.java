package com.simpleqq.common;

import java.io.Serializable;

/**
 * 消息实体类
 * 用于封装客户端和服务器之间传输的所有消息数据
 * 实现Serializable接口以支持对象序列化传输
 */
public class Message implements Serializable {
    private MessageType type;     // 消息类型，决定消息的处理方式
    private String senderId;      // 发送者用户ID
    private String receiverId;    // 接收者用户ID（群聊时为群组ID）
    private long timestamp;       // 消息时间戳，用于排序和显示时间
    private String content;       // 消息内容（文本内容或图片数据）

    /**
     * 构造函数
     * @param type 消息类型
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param content 消息内容
     */
    public Message(MessageType type, String senderId, String receiverId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = System.currentTimeMillis(); // 自动设置当前时间戳
        this.content = content;
    }

    // Getter方法
    public MessageType getType() {
        return type;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getContent() {
        return content;
    }

    // Setter方法
    public void setType(MessageType type) {
        this.type = type;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 重写toString方法，便于调试和日志输出
     */
    @Override
    public String toString() {
        return "Message{" +
               "type=" + type +
               ", senderId='" + senderId + '\'' +
               ", receiverId='" + receiverId + '\'' +
               ", timestamp=" + timestamp +
               ", content='" + content + '\'' +
               '}';
    }
}