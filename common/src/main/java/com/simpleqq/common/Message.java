
package com.simpleqq.common;

import java.io.Serializable;

public class Message implements Serializable {
    private MessageType type;
    private String senderId;
    private String receiverId;
    private long timestamp;
    private String content;

    public Message(MessageType type, String senderId, String receiverId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = System.currentTimeMillis();
        this.content = content;
    }

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

