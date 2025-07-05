package com.simpleqq.server;

import com.simpleqq.common.Message;
import com.simpleqq.common.MessageType;
import com.simpleqq.common.User;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务器主类
 * 负责启动服务器、管理客户端连接、处理消息转发和数据持久化
 * 使用多线程处理多个客户端的并发连接
 */
public class Server {
    private static final int PORT = 8888;                              // 服务器监听端口
    private UserManager userManager;                                   // 用户管理器，处理用户相关操作
    private GroupManager groupManager;                                 // 群组管理器，处理群组相关操作
    private Map<String, ClientHandler> onlineClients;                 // 在线客户端映射表，key为用户ID
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 日期格式化器

    /**
     * 构造函数
     * 初始化服务器组件和数据结构
     */
    public Server() {
        userManager = new UserManager();
        groupManager = new GroupManager();
        onlineClients = new ConcurrentHashMap<>(); // 使用线程安全的Map
    }

    /**
     * 获取用户管理器
     * @return 用户管理器实例
     */
    public UserManager getUserManager() {
        return userManager;
    }

    /**
     * 获取群组管理器
     * @return 群组管理器实例
     */
    public GroupManager getGroupManager() {
        return groupManager;
    }

    /**
     * 获取在线客户端映射表
     * @return 在线客户端Map
     */
    public Map<String, ClientHandler> getOnlineClients() {
        return onlineClients;
    }

    /**
     * 添加在线客户端
     * 当用户登录成功时调用，将客户端处理器添加到在线列表
     * @param userId 用户ID
     * @param handler 客户端处理器
     */
    public synchronized void addOnlineClient(String userId, ClientHandler handler) {
        onlineClients.put(userId, handler);
        User user = userManager.getUserById(userId);
        if (user != null) {
            user.setOnline(true); // 设置用户在线状态
        }
        System.out.println("User " + userId + " is now online. Total online: " + onlineClients.size());
    }

    /**
     * 移除客户端连接
     * 当用户断开连接时调用，从在线列表中移除并更新状态
     * @param userId 用户ID
     */
    public synchronized void removeClient(String userId) {
        onlineClients.remove(userId);
        User user = userManager.getUserById(userId);
        if (user != null) {
            user.setOnline(false); // 设置用户离线状态
        }
        System.out.println("User " + userId + " went offline. Total online: " + onlineClients.size());
    }

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return true表示在线，false表示离线
     */
    public boolean isUserOnline(String userId) {
        return onlineClients.containsKey(userId);
    }

    /**
     * 启动服务器
     * 创建ServerSocket并持续监听客户端连接请求
     * 为每个新连接创建独立的ClientHandler线程
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            
            while (true) {
                // 等待客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                
                // 为每个客户端创建独立的处理线程
                new ClientHandler(clientSocket, this).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存聊天消息到文件
     * 根据消息类型确定保存的文件名和格式
     * @param message 要保存的消息对象
     */
    public void saveChatMessage(Message message) {
        String chatFileName;
        
        if (message.getType() == MessageType.TEXT_MESSAGE || message.getType() == MessageType.IMAGE_MESSAGE) {
            // 私聊消息：按用户ID字母顺序生成文件名，确保一致性
            String sender = message.getSenderId();
            String receiver = message.getReceiverId();
            if (sender.compareTo(receiver) < 0) {
                chatFileName = "chat_history_" + sender + "_" + receiver + ".txt";
            } else {
                chatFileName = "chat_history_" + receiver + "_" + sender + ".txt";
            }
        } else if (message.getType() == MessageType.GROUP_MESSAGE) {
            // 群聊消息：使用群组ID生成文件名
            chatFileName = "chat_history_group_" + message.getReceiverId() + ".txt";
        } else {
            return; // 不保存其他类型的消息
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(chatFileName, true))) {
            String contentToSave;
            if (message.getType() == MessageType.IMAGE_MESSAGE) {
                // 图片消息只保存文件名，不保存Base64数据
                contentToSave = "[图片: " + message.getContent() + "]";
            } else {
                contentToSave = message.getContent();
            }
            
            // 写入格式：时间戳 [发送者] to [接收者]: 内容
            String logEntry = dateFormat.format(new Date(message.getTimestamp())) + 
                            " [" + message.getSenderId() + "] to [" + message.getReceiverId() + "]: " + 
                            contentToSave;
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 转发消息给指定用户
     * 查找目标用户的客户端处理器并发送消息
     * @param message 要转发的消息
     */
    public void forwardMessage(Message message) {
        String receiverId = message.getReceiverId();
        ClientHandler receiverHandler = onlineClients.get(receiverId);
        
        if (receiverHandler != null) {
            try {
                receiverHandler.sendMessage(message);
            } catch (IOException e) {
                System.err.println("Error forwarding message to " + receiverId + ": " + e.getMessage());
            }
        } else {
            System.out.println("Receiver " + receiverId + " is not online. Message not forwarded.");
        }
    }

    /**
     * 创建群组
     * 委托给群组管理器处理群组创建逻辑
     * @param groupId 群组ID
     * @param creatorId 创建者用户ID
     * @return 创建成功返回true，失败返回false
     */
    public boolean createGroup(String groupId, String creatorId) {
        return groupManager.createGroup(groupId, creatorId);
    }

    /**
     * 主方法，程序入口点
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}