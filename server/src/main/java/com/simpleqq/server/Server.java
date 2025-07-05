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

public class Server {
    private static final int PORT = 8888;
    private UserManager userManager;
    private GroupManager groupManager;
    private Map<String, ClientHandler> onlineClients;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Server() {
        userManager = new UserManager();
        groupManager = new GroupManager();
        onlineClients = new ConcurrentHashMap<>();
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public Map<String, ClientHandler> getOnlineClients() {
        return onlineClients;
    }

    public synchronized void addOnlineClient(String userId, ClientHandler handler) {
        onlineClients.put(userId, handler);
        User user = userManager.getUserById(userId);
        if (user != null) {
            user.setOnline(true);
        }
        System.out.println("User " + userId + " is now online. Total online: " + onlineClients.size());
        // 移除了通知所有在线用户的逻辑，因为已经删除了在线用户列表
    }

    public synchronized void removeClient(String userId) {
        onlineClients.remove(userId);
        User user = userManager.getUserById(userId);
        if (user != null) {
            user.setOnline(false);
        }
        System.out.println("User " + userId + " went offline. Total online: " + onlineClients.size());
        // 移除了通知所有在线用户的逻辑，因为已经删除了在线用户列表
    }

    public boolean isUserOnline(String userId) {
        return onlineClients.containsKey(userId);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                new ClientHandler(clientSocket, this).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveChatMessage(Message message) {
        String chatFileName;
        if (message.getType() == MessageType.TEXT_MESSAGE || message.getType() == MessageType.IMAGE_MESSAGE) {
            // 单聊
            String sender = message.getSenderId();
            String receiver = message.getReceiverId();
            // 保证文件名一致性，按字母顺序排列
            if (sender.compareTo(receiver) < 0) {
                chatFileName = "chat_history_" + sender + "_" + receiver + ".txt";
            } else {
                chatFileName = "chat_history_" + receiver + "_" + sender + ".txt";
            }
        } else if (message.getType() == MessageType.GROUP_MESSAGE) {
            // 群聊，假设receiverId是群ID
            chatFileName = "chat_history_group_" + message.getReceiverId() + ".txt";
        } else {
            return; // 不保存其他类型的消息
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(chatFileName, true))) {
            String contentToSave;
            if (message.getType() == MessageType.IMAGE_MESSAGE) {
                // For image messages, save only the filename
                contentToSave = "[图片: " + message.getContent() + "]";
            } else {
                contentToSave = message.getContent();
            }
            writer.write(dateFormat.format(new Date(message.getTimestamp())) + " [" + message.getSenderId() + "] to [" + message.getReceiverId() + "]: " + contentToSave);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            // Optionally, send a message back to sender that receiver is offline
        }
    }

    public boolean createGroup(String groupId, String creatorId) {
        return groupManager.createGroup(groupId, creatorId);
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}