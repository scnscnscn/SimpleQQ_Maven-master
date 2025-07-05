package com.simpleqq.server;

import com.simpleqq.common.Message;
import com.simpleqq.common.MessageType;
import com.simpleqq.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * 客户端处理器类
 * 每个连接的客户端都有一个对应的ClientHandler线程
 * 负责处理单个客户端的所有消息请求和响应
 */
public class ClientHandler extends Thread {
    private Socket socket;              // 客户端Socket连接
    private Server server;              // 服务器实例引用
    private ObjectInputStream ois;      // 对象输入流，用于接收客户端消息
    private ObjectOutputStream oos;     // 对象输出流，用于发送消息给客户端
    private String userId;              // 当前连接的用户ID

    /**
     * 构造函数
     * @param socket 客户端Socket连接
     * @param server 服务器实例
     */
    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            // 注意：必须先创建输出流，再创建输入流，避免死锁
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取当前用户ID
     * @return 用户ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 获取输出流对象
     * @return 对象输出流
     */
    public ObjectOutputStream getOos() {
        return oos;
    }

    /**
     * 线程主方法
     * 持续监听客户端消息并进行处理
     */
    @Override
    public void run() {
        try {
            while (true) {
                // 接收客户端消息
                Message message = (Message) ois.readObject();
                System.out.println("Received message from client: " + message);

                // 根据消息类型分发处理
                switch (message.getType()) {
                    case LOGIN:
                        handleLogin(message);
                        break;
                    case REGISTER:
                        handleRegister(message);
                        break;
                    case FRIEND_REQUEST:
                        handleFriendRequest(message);
                        break;
                    case FRIEND_ACCEPT:
                        handleFriendAccept(message);
                        break;
                    case FRIEND_REJECT:
                        handleFriendReject(message);
                        break;
                    case DELETE_FRIEND:
                        handleDeleteFriend(message);
                        break;
                    case TEXT_MESSAGE:
                        handleTextMessage(message);
                        break;
                    case GROUP_MESSAGE:
                        handleGroupMessage(message);
                        break;
                    case IMAGE_MESSAGE:
                        handleImageMessage(message);
                        break;
                    case GROUP_INVITE:
                        handleGroupInvite(message);
                        break;
                    case GROUP_ACCEPT:
                        handleGroupAccept(message);
                        break;
                    case GROUP_REJECT:
                        handleGroupReject(message);
                        break;
                    case CREATE_GROUP:
                        handleCreateGroup(message);
                        break;
                    case GET_GROUPS:
                        sendGroupList(message.getSenderId());
                        break;
                    case GET_PENDING_REQUESTS:
                        sendPendingRequests(message.getSenderId());
                        break;
                    case GET_GROUP_MEMBERS:
                        sendGroupMembers(message.getContent(), message.getSenderId());
                        break;
                    case FRIEND_LIST:
                        sendFriendList(message.getSenderId());
                        break;
                    default:
                        System.out.println("Unknown message type: " + message.getType());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client " + userId + " disconnected.");
            // 客户端断开连接时的清理工作
            server.removeClient(userId);
            if (userId != null) {
                User user = server.getUserManager().getUserById(userId);
                if (user != null) {
                    user.setOnline(false);
                    notifyFriendsStatusChange(userId); // 通知好友状态变化
                }
            }
        } finally {
            // 关闭资源
            try {
                if (ois != null) ois.close();
                if (oos != null) oos.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理用户登录请求
     * 验证用户凭据并建立会话
     * @param message 登录消息，内容格式：用户ID,密码
     */
    private void handleLogin(Message message) throws IOException {
        String[] credentials = message.getContent().split(",");
        String id = credentials[0];
        String password = credentials[1];
        User user = server.getUserManager().login(id, password);

        if (user != null) {
            // 检查用户是否已在其他地方登录
            if (server.isUserOnline(id)) {
                sendMessage(new Message(MessageType.LOGIN_FAIL, "Server", id, "User already online."));
                return;
            }
            
            // 登录成功，建立会话
            this.userId = id;
            server.addOnlineClient(id, this);
            user.setOnline(true);
            sendMessage(new Message(MessageType.LOGIN_SUCCESS, "Server", id, user.getUsername()));
            
            // 发送初始数据
            sendFriendList(id);
            sendGroupList(id);
            sendPendingRequests(id);
            
            // 通知好友用户上线
            notifyFriendsStatusChange(id);
        } else {
            sendMessage(new Message(MessageType.LOGIN_FAIL, "Server", id, "Invalid ID or password."));
        }
    }

    /**
     * 处理用户注册请求
     * 创建新用户账户
     * @param message 注册消息，内容格式：用户ID,用户名,密码
     */
    private void handleRegister(Message message) throws IOException {
        String[] userInfo = message.getContent().split(",");
        String id = userInfo[0];
        String username = userInfo[1];
        String password = userInfo[2];

        if (server.getUserManager().registerUser(id, username, password)) {
            sendMessage(new Message(MessageType.REGISTER_SUCCESS, "Server", id, "Registration successful."));
        } else {
            sendMessage(new Message(MessageType.REGISTER_FAIL, "Server", id, "ID already exists."));
        }
    }

    /**
     * 处理好友请求
     * 发送好友申请给目标用户
     * @param message 好友请求消息
     */
    private void handleFriendRequest(Message message) throws IOException {
        String senderId = message.getSenderId();
        String receiverId = message.getReceiverId();

        if (server.getUserManager().sendFriendRequest(senderId, receiverId)) {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", senderId, 
                "Friend request sent to " + receiverId + "."));
            
            // 如果接收者在线，立即通知
            ClientHandler receiverHandler = server.getOnlineClients().get(receiverId);
            if (receiverHandler != null) {
                receiverHandler.sendMessage(new Message(MessageType.FRIEND_REQUEST, senderId, receiverId, 
                    "You have a new friend request from " + senderId + "."));
            }
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", senderId, 
                "Failed to send friend request to " + receiverId + ". (Already friends or request pending)"));
        }
    }

    /**
     * 处理接受好友请求
     * 建立好友关系并通知相关用户
     * @param message 接受好友请求消息
     */
    private void handleFriendAccept(Message message) throws IOException {
        String acceptorId = message.getSenderId();
        String requesterId = message.getReceiverId(); // 原始请求发送者

        if (server.getUserManager().acceptFriendRequest(acceptorId, requesterId)) {
            sendMessage(new Message(MessageType.ADD_FRIEND_SUCCESS, "Server", acceptorId, 
                "You are now friends with " + requesterId + "."));
            
            // 通知请求发送者
            ClientHandler requesterHandler = server.getOnlineClients().get(requesterId);
            if (requesterHandler != null) {
                requesterHandler.sendMessage(new Message(MessageType.FRIEND_ACCEPT, acceptorId, requesterId, 
                    acceptorId + " accepted your friend request."));
                requesterHandler.sendFriendList(requesterId); // 立即更新好友列表
            }
            sendFriendList(acceptorId); // 更新接受者的好友列表
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", acceptorId, 
                "Failed to accept friend request from " + requesterId + "."));
        }
    }

    /**
     * 处理拒绝好友请求
     * 移除好友请求并通知发送者
     * @param message 拒绝好友请求消息
     */
    private void handleFriendReject(Message message) throws IOException {
        String rejectorId = message.getSenderId();
        String requesterId = message.getReceiverId(); // 原始请求发送者

        if (server.getUserManager().rejectFriendRequest(rejectorId, requesterId)) {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", rejectorId, 
                "You rejected friend request from " + requesterId + "."));
            
            // 通知请求发送者
            ClientHandler requesterHandler = server.getOnlineClients().get(requesterId);
            if (requesterHandler != null) {
                requesterHandler.sendMessage(new Message(MessageType.FRIEND_REJECT, rejectorId, requesterId, 
                    rejectorId + " rejected your friend request."));
            }
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", rejectorId, 
                "Failed to reject friend request from " + requesterId + "."));
        }
    }

    /**
     * 处理删除好友请求
     * 解除好友关系并通知相关用户
     * @param message 删除好友消息
     */
    private void handleDeleteFriend(Message message) throws IOException {
        String requesterId = message.getSenderId();
        String targetId = message.getReceiverId();

        if (server.getUserManager().deleteFriend(requesterId, targetId)) {
            sendMessage(new Message(MessageType.DELETE_FRIEND_SUCCESS, "Server", requesterId, 
                "Friend deleted: " + targetId));
            
            // 通知被删除方
            ClientHandler targetHandler = server.getOnlineClients().get(targetId);
            if (targetHandler != null) {
                targetHandler.sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", targetId, 
                    "You are no longer friends with: " + requesterId));
                targetHandler.sendFriendList(targetId); // 立即更新好友列表
            }
            sendFriendList(requesterId); // 更新删除方的好友列表
        } else {
            sendMessage(new Message(MessageType.DELETE_FRIEND_FAIL, "Server", requesterId, 
                "Failed to delete friend: " + targetId));
        }
    }

    /**
     * 处理私聊文本消息
     * 验证好友关系并转发消息
     * @param message 文本消息
     */
    private void handleTextMessage(Message message) throws IOException {
        // 验证发送者和接收者是否为好友关系
        if (!server.getUserManager().areFriends(message.getSenderId(), message.getReceiverId())) {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), 
                "You can only send messages to friends."));
            return;
        }

        // 转发消息给接收者
        ClientHandler receiverHandler = server.getOnlineClients().get(message.getReceiverId());
        if (receiverHandler != null) {
            receiverHandler.sendMessage(message);
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), 
                "User " + message.getReceiverId() + " is offline."));
        }
        
        // 保存聊天记录
        server.saveChatMessage(message);
    }

    /**
     * 处理群组消息
     * 验证群成员身份并转发给所有其他成员
     * @param message 群组消息
     */
    private void handleGroupMessage(Message message) throws IOException {
        List<String> groupMembers = server.getGroupManager().getGroupMembers(message.getReceiverId());
        if (groupMembers != null) {
            // 验证发送者是否为群成员
            if (!groupMembers.contains(message.getSenderId())) {
                sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), 
                    "You are not a member of group " + message.getReceiverId() + "."));
                return;
            }
            
            // 转发给群内其他成员（不包括发送者）
            for (String memberId : groupMembers) {
                if (!memberId.equals(message.getSenderId())) { // 不发送给自己
                    ClientHandler memberHandler = server.getOnlineClients().get(memberId);
                    if (memberHandler != null) {
                        // 创建新的消息对象，确保消息类型正确
                        Message groupMsg = new Message(MessageType.GROUP_MESSAGE, 
                            message.getSenderId(), message.getReceiverId(), message.getContent());
                        groupMsg.setTimestamp(message.getTimestamp());
                        memberHandler.sendMessage(groupMsg);
                    }
                }
            }
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), 
                "Group " + message.getReceiverId() + " does not exist."));
        }
        
        // 保存聊天记录
        server.saveChatMessage(message);
    }

    /**
     * 处理图片消息
     * 根据接收者类型（个人或群组）进行相应处理
     * @param message 图片消息
     */
    private void handleImageMessage(Message message) throws IOException {
        // 判断是群聊还是单聊
        List<String> groupMembers = server.getGroupManager().getGroupMembers(message.getReceiverId());
        if (groupMembers != null) {
            // 群聊图片消息处理
            if (!groupMembers.contains(message.getSenderId())) {
                sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), 
                    "You are not a member of group " + message.getReceiverId() + "."));
                return;
            }
            
            // 转发给所有其他成员
            for (String memberId : groupMembers) {
                if (!memberId.equals(message.getSenderId())) {
                    ClientHandler memberHandler = server.getOnlineClients().get(memberId);
                    if (memberHandler != null) {
                        memberHandler.sendMessage(message);
                    }
                }
            }
        } else {
            // 单聊图片消息处理
            if (!server.getUserManager().areFriends(message.getSenderId(), message.getReceiverId())) {
                sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), 
                    "You can only send images to friends."));
                return;
            }
            
            // 直接转发给接收者
            ClientHandler receiverHandler = server.getOnlineClients().get(message.getReceiverId());
            if (receiverHandler != null) {
                receiverHandler.sendMessage(message);
            } else {
                sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), 
                    "User " + message.getReceiverId() + " is offline."));
            }
        }
        
        // 保存聊天记录（只保存文件名）
        String content = message.getContent();
        String fileName = content.contains(":") ? content.split(":", 2)[0] : content;
        Message historyMessage = new Message(MessageType.IMAGE_MESSAGE, 
            message.getSenderId(), message.getReceiverId(), fileName);
        server.saveChatMessage(historyMessage);
    }

    /**
     * 处理群组邀请
     * 发送群组邀请给目标用户
     * @param message 群组邀请消息
     */
    private void handleGroupInvite(Message message) throws IOException {
        String inviterId = message.getSenderId();
        String invitedId = message.getReceiverId();
        String groupId = message.getContent();

        if (server.getGroupManager().sendGroupInvite(inviterId, invitedId, groupId)) {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", inviterId, 
                "Group invite sent to " + invitedId + "."));
            
            // 如果被邀请者在线，立即通知
            ClientHandler invitedHandler = server.getOnlineClients().get(invitedId);
            if (invitedHandler != null) {
                invitedHandler.sendMessage(new Message(MessageType.GROUP_INVITE, inviterId, invitedId, groupId));
            }
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", inviterId, 
                "Failed to send group invite to " + invitedId + "."));
        }
    }

    /**
     * 处理接受群组邀请
     * 将用户加入群组并通知所有成员
     * @param message 接受群组邀请消息
     */
    private void handleGroupAccept(Message message) throws IOException {
        String acceptorId = message.getSenderId();
        String groupId = message.getContent();

        if (server.getGroupManager().acceptGroupInvite(acceptorId, groupId)) {
            sendMessage(new Message(MessageType.GROUP_JOIN_SUCCESS, "Server", acceptorId, groupId));
            
            // 通知所有群成员有新成员加入
            List<String> groupMembers = server.getGroupManager().getGroupMembers(groupId);
            if (groupMembers != null) {
                for (String memberId : groupMembers) {
                    ClientHandler memberHandler = server.getOnlineClients().get(memberId);
                    if (memberHandler != null) {
                        memberHandler.sendGroupList(memberId); // 刷新群组列表
                        if (!memberId.equals(acceptorId)) {
                            memberHandler.sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", memberId, 
                                acceptorId + " has joined group " + groupId + "."));
                        }
                    }
                }
            }
        } else {
            sendMessage(new Message(MessageType.GROUP_JOIN_FAIL, "Server", acceptorId, 
                "Failed to join group " + groupId + "."));
        }
    }

    /**
     * 处理拒绝群组邀请
     * 移除群组邀请记录
     * @param message 拒绝群组邀请消息
     */
    private void handleGroupReject(Message message) throws IOException {
        String rejectorId = message.getSenderId();
        String groupId = message.getContent();

        if (server.getGroupManager().rejectGroupInvite(rejectorId, groupId)) {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", rejectorId, 
                "You rejected group invite to " + groupId + "."));
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", rejectorId, 
                "Failed to reject group invite to " + groupId + "."));
        }
    }

    /**
     * 处理创建群组请求
     * 创建新群组并通知相关用户
     * @param message 创建群组消息
     */
    private void handleCreateGroup(Message message) throws IOException {
        String creatorId = message.getSenderId();
        String groupId = message.getContent();
        
        if (server.createGroup(groupId, creatorId)) {
            sendMessage(new Message(MessageType.CREATE_GROUP_SUCCESS, "Server", creatorId, groupId));
            
            // 刷新所有在线用户的群组列表
            for (ClientHandler handler : server.getOnlineClients().values()) {
                handler.sendGroupList(handler.getUserId());
            }
        } else {
            sendMessage(new Message(MessageType.CREATE_GROUP_FAIL, "Server", creatorId, 
                "Group ID already exists or invalid."));
        }
    }

    /**
     * 发送消息给客户端
     * @param message 要发送的消息对象
     */
    public void sendMessage(Message message) throws IOException {
        oos.writeObject(message);
        oos.flush(); // 确保消息立即发送
    }

    /**
     * 发送好友列表给客户端
     * 包含好友的ID、用户名和在线状态
     * @param userId 请求用户的ID
     */
    public void sendFriendList(String userId) throws IOException {
        List<String> friendIds = server.getUserManager().getFriends(userId);
        
        StringBuilder sb = new StringBuilder();
        for (String friendId : friendIds) {
            User friendUser = server.getUserManager().getUserById(friendId);
            if (friendUser != null) {
                // 检查好友是否在线
                boolean isOnline = server.isUserOnline(friendId);
                String status = isOnline ? "online" : "offline";
                String friendInfo = friendUser.getId() + ":" + friendUser.getUsername() + ":" + status;
                sb.append(friendInfo).append(";");
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // 移除末尾的分号
        }
        
        sendMessage(new Message(MessageType.FRIEND_LIST, "Server", userId, sb.toString()));
    }

    /**
     * 发送群组列表给客户端
     * @param userId 请求用户的ID
     */
    public void sendGroupList(String userId) throws IOException {
        List<String> groupIds = server.getGroupManager().getUserGroups(userId);
        StringBuilder sb = new StringBuilder();
        for (String groupId : groupIds) {
            sb.append(groupId).append(";");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // 移除末尾的分号
        }
        sendMessage(new Message(MessageType.GET_GROUPS, "Server", userId, sb.toString()));
    }

    /**
     * 发送待处理请求列表给客户端
     * 包含好友请求和群组邀请
     * @param userId 请求用户的ID
     */
    public void sendPendingRequests(String userId) throws IOException {
        List<String> pendingFriendRequests = server.getUserManager().getPendingFriendRequests(userId);
        List<String> pendingGroupInvites = server.getGroupManager().getPendingGroupInvites(userId);

        // 构建好友请求字符串
        StringBuilder friendReqSb = new StringBuilder();
        for (String senderId : pendingFriendRequests) {
            friendReqSb.append(senderId).append(";");
        }
        if (friendReqSb.length() > 0) {
            friendReqSb.setLength(friendReqSb.length() - 1);
        }

        // 构建群组邀请字符串
        StringBuilder groupInvSb = new StringBuilder();
        for (String groupId : pendingGroupInvites) {
            groupInvSb.append(groupId).append(";");
        }
        if (groupInvSb.length() > 0) {
            groupInvSb.setLength(groupInvSb.length() - 1);
        }

        // 使用双竖线分隔好友请求和群组邀请
        String content = friendReqSb.toString() + "||" + groupInvSb.toString();
        sendMessage(new Message(MessageType.GET_PENDING_REQUESTS, "Server", userId, content));
    }

    /**
     * 发送群组成员列表给客户端
     * 包含成员的ID、用户名和在线状态
     * @param groupId 群组ID
     * @param requesterId 请求者ID
     */
    public void sendGroupMembers(String groupId, String requesterId) throws IOException {
        List<String> members = server.getGroupManager().getGroupMembers(groupId);
        StringBuilder sb = new StringBuilder();
        if (members != null) {
            for (String memberId : members) {
                User memberUser = server.getUserManager().getUserById(memberId);
                if (memberUser != null) {
                    // 检查成员是否在线
                    boolean isOnline = server.isUserOnline(memberId);
                    String status = isOnline ? "online" : "offline";
                    sb.append(memberUser.getId()).append(":").append(memberUser.getUsername())
                      .append(":").append(status).append(";");
                }
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // 移除末尾的分号
        }
        sendMessage(new Message(MessageType.GET_GROUP_MEMBERS, groupId, requesterId, sb.toString()));
    }

    /**
     * 通知好友状态变化
     * 当用户上线或下线时，通知其所有好友更新状态
     * @param userId 状态发生变化的用户ID
     */
    private void notifyFriendsStatusChange(String userId) {
        List<String> friends = server.getUserManager().getFriends(userId);
        for (String friendId : friends) {
            ClientHandler friendHandler = server.getOnlineClients().get(friendId);
            if (friendHandler != null) {
                try {
                    friendHandler.sendFriendList(friendId); // 发送更新的好友列表
                } catch (IOException e) {
                    System.err.println("Failed to notify friend " + friendId + " of status change: " + e.getMessage());
                }
            }
        }
    }
}