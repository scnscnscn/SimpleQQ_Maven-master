package com.simpleqq.client;

import com.simpleqq.common.Message;
import com.simpleqq.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * 客户端核心类
 * 负责与服务器建立连接、发送和接收消息
 * 管理客户端的网络通信和消息处理
 */
public class Client {
    private static final String SERVER_IP = "127.0.0.1"; // 服务器IP地址
    private static final int SERVER_PORT = 8888;          // 服务器端口号

    public Socket socket;                    // 与服务器的Socket连接
    private ObjectOutputStream oos;          // 对象输出流，用于发送消息
    private ObjectInputStream ois;           // 对象输入流，用于接收消息
    private User currentUser;                // 当前登录的用户信息
    private Consumer<Message> messageListener; // 消息监听器，处理接收到的消息

    /**
     * 默认构造函数
     */
    public Client() {
    }

    /**
     * 连接到服务器
     * 建立Socket连接并启动消息接收线程
     * @throws IOException 连接失败时抛出异常
     */
    public void connect() throws IOException {
        socket = new Socket(SERVER_IP, SERVER_PORT);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());

        // 启动消息接收线程
        new Thread(() -> {
            try {
                while (true) {
                    // 持续监听服务器消息
                    Message message = (Message) ois.readObject();
                    System.out.println("Client received: " + message);
                    
                    // 如果设置了消息监听器，则调用处理方法
                    if (messageListener != null) {
                        messageListener.accept(message);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Server disconnected or error: " + e.getMessage());
                disconnect(); // 连接断开时自动清理资源
            }
        }).start();
    }

    /**
     * 断开与服务器的连接
     * 关闭所有网络资源
     */
    public void disconnect() {
        try {
            if (socket != null) socket.close();
            if (ois != null) ois.close();
            if (oos != null) oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送消息到服务器
     * @param message 要发送的消息对象
     */
    public void sendMessage(Message message) {
        try {
            oos.writeObject(message);
            oos.flush(); // 确保消息立即发送
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取当前登录用户
     * @return 当前用户对象
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * 设置当前登录用户
     * @param currentUser 用户对象
     */
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * 设置消息监听器
     * @param listener 消息处理函数
     */
    public void setMessageListener(Consumer<Message> listener) {
        this.messageListener = listener;
    }

    /**
     * 主方法，用于测试客户端连接
     */
    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}