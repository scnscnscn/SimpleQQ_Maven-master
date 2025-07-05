package com.simpleqq.server;

import com.simpleqq.common.Message;
import com.simpleqq.common.MessageType;
import com.simpleqq.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket; // Import Socket class
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler extends Thread {
    private Socket socket;
    private Server server;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String userId;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUserId() {
        return userId;
    }

    public ObjectOutputStream getOos() {
        return oos;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message message = (Message) ois.readObject();
                System.out.println("Received message from client: " + message);

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
                    case IMAGE_REQUEST:
                        handleImageRequest(message);
                        break;
                    case IMAGE_ACCEPT:
                        handleImageAccept(message);
                        break;
                    case IMAGE_REJECT:
                        handleImageReject(message);
                        break;
                    case IMAGE_DATA:
                        handleImageData(message);
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
            server.removeClient(userId);
            if (userId != null) {
                User user = server.getUserManager().getUserById(userId);
                if (user != null) {
                    user.setOnline(false);
                    // 通知所有在线用户更新好友列表（更新在线状态）
                    notifyFriendsStatusChange(userId);
                }
            }
        } finally {
            try {
                if (ois != null) ois.close();
                if (oos != null) oos.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleLogin(Message message) throws IOException {
        String[] credentials = message.getContent().split(",");
        String id = credentials[0];
        String password = credentials[1];
        User user = server.getUserManager().login(id, password);

        if (user != null) {
            if (server.isUserOnline(id)) {
                sendMessage(new Message(MessageType.LOGIN_FAIL, "Server", id, "User already online."));
                return;
            }
            this.userId = id;
            server.addOnlineClient(id, this);
            user.setOnline(true);
            sendMessage(new Message(MessageType.LOGIN_SUCCESS, "Server", id, user.getUsername()));
            sendFriendList(id);
            sendPendingFriendRequests(id); // Send pending friend requests on login
            sendGroupList(id); // Send group list on login
            sendPendingRequests(id); // Send all pending requests on login
            
            // 通知所有好友用户上线了
            notifyFriendsStatusChange(id);
        } else {
            sendMessage(new Message(MessageType.LOGIN_FAIL, "Server", id, "Invalid ID or password."));
        }
    }

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

    private void handleFriendRequest(Message message) throws IOException {
        String senderId = message.getSenderId();
        String receiverId = message.getReceiverId();

        System.out.println("Processing friend request from " + senderId + " to " + receiverId);

        if (server.getUserManager().sendFriendRequest(senderId, receiverId)) {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", senderId, "Friend request sent to " + receiverId + "."));
            ClientHandler receiverHandler = server.getOnlineClients().get(receiverId);
            if (receiverHandler != null) {
                receiverHandler.sendMessage(new Message(MessageType.FRIEND_REQUEST, senderId, receiverId, "You have a new friend request from " + senderId + "."));
            }
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", senderId, "Failed to send friend request to " + receiverId + ". (Already friends or request pending)"));
        }
    }

    private void handleFriendAccept(Message message) throws IOException {
        String acceptorId = message.getSenderId();
        String requesterId = message.getReceiverId(); // Original sender of the request

        System.out.println("Processing friend accept from " + acceptorId + " for request from " + requesterId);

        if (server.getUserManager().acceptFriendRequest(acceptorId, requesterId)) {
            sendMessage(new Message(MessageType.ADD_FRIEND_SUCCESS, "Server", acceptorId, "You are now friends with " + requesterId + "."));
            ClientHandler requesterHandler = server.getOnlineClients().get(requesterId);
            if (requesterHandler != null) {
                requesterHandler.sendMessage(new Message(MessageType.FRIEND_ACCEPT, acceptorId, requesterId, acceptorId + " accepted your friend request."));
                // 立即发送更新的好友列表给请求者
                requesterHandler.sendFriendList(requesterId);
            }
            // 立即发送更新的好友列表给接受者
            sendFriendList(acceptorId);
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", acceptorId, "Failed to accept friend request from " + requesterId + "."));
        }
    }

    private void handleFriendReject(Message message) throws IOException {
        String rejectorId = message.getSenderId();
        String requesterId = message.getReceiverId(); // Original sender of the request

        if (server.getUserManager().rejectFriendRequest(rejectorId, requesterId)) {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", rejectorId, "You rejected friend request from " + requesterId + "."));
            ClientHandler requesterHandler = server.getOnlineClients().get(requesterId);
            if (requesterHandler != null) {
                requesterHandler.sendMessage(new Message(MessageType.FRIEND_REJECT, rejectorId, requesterId, rejectorId + " rejected your friend request."));
            }
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", rejectorId, "Failed to reject friend request from " + requesterId + "."));
        }
    }

    private void handleDeleteFriend(Message message) throws IOException {
        String requesterId = message.getSenderId();
        String targetId = message.getReceiverId();

        System.out.println("Processing delete friend request from " + requesterId + " to delete " + targetId);

        if (server.getUserManager().deleteFriend(requesterId, targetId)) {
            sendMessage(new Message(MessageType.DELETE_FRIEND_SUCCESS, "Server", requesterId, "Friend deleted: " + targetId));
            // 通知被删除方
            ClientHandler targetHandler = server.getOnlineClients().get(targetId);
            if (targetHandler != null) {
                targetHandler.sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", targetId, "You are no longer friends with: " + requesterId));
                // 立即发送更新的好友列表给被删除方
                targetHandler.sendFriendList(targetId);
            }
            // 立即发送更新的好友列表给删除方
            sendFriendList(requesterId);
        } else {
            sendMessage(new Message(MessageType.DELETE_FRIEND_FAIL, "Server", requesterId, "Failed to delete friend: " + targetId));
        }
    }

    private void handleTextMessage(Message message) throws IOException {
        System.out.println("Processing text message from " + message.getSenderId() + " to " + message.getReceiverId());
        
        // 检查发送者和接收者是否为好友关系
        if (!server.getUserManager().areFriends(message.getSenderId(), message.getReceiverId())) {
            System.out.println("Users " + message.getSenderId() + " and " + message.getReceiverId() + " are not friends");
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), "You can only send messages to friends."));
            return;
        }

        ClientHandler receiverHandler = server.getOnlineClients().get(message.getReceiverId());
        if (receiverHandler != null) {
            receiverHandler.sendMessage(message);
            System.out.println("Message forwarded to " + message.getReceiverId());
        } else {
            // 用户不在线，可以考虑离线消息存储
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), "User " + message.getReceiverId() + " is offline."));
        }
        // 保存聊天记录
        server.saveChatMessage(message);
    }

    private void handleGroupMessage(Message message) throws IOException {
        List<String> groupMembers = server.getGroupManager().getGroupMembers(message.getReceiverId());
        if (groupMembers != null) {
            // 检查发送者是否为群成员
            if (!groupMembers.contains(message.getSenderId())) {
                sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), "You are not a member of group " + message.getReceiverId() + "."));
                return;
            }
            
            // 只转发给群内其他成员（不包括发送者）
            for (String memberId : groupMembers) {
                if (!memberId.equals(message.getSenderId())) { // 不发送给自己
                    ClientHandler memberHandler = server.getOnlineClients().get(memberId);
                    if (memberHandler != null) {
                        // 创建新的消息对象，确保消息类型正确
                        Message groupMsg = new Message(MessageType.GROUP_MESSAGE, message.getSenderId(), message.getReceiverId(), message.getContent());
                        groupMsg.setTimestamp(message.getTimestamp());
                        memberHandler.sendMessage(groupMsg);
                    }
                }
            }
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), "Group " + message.getReceiverId() + " does not exist."));
        }
        // 保存聊天记录
        server.saveChatMessage(message);
    }

    private void handleImageMessage(Message message) throws IOException {
        // 判断是群聊还是单聊
        List<String> groupMembers = server.getGroupManager().getGroupMembers(message.getReceiverId());
        if (groupMembers != null) {
            // 群聊图片消息 - 检查发送者是否为群成员
            if (!groupMembers.contains(message.getSenderId())) {
                sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), "You are not a member of group " + message.getReceiverId() + "."));
                return;
            }
            
            // 转发给所有其他成员
            for (String memberId : groupMembers) {
                if (!memberId.equals(message.getSenderId())) { // 不发送给自己
                    ClientHandler memberHandler = server.getOnlineClients().get(memberId);
                    if (memberHandler != null) {
                        memberHandler.sendMessage(message);
                    }
                }
            }
        } else {
            // 单聊图片消息 - 检查是否为好友关系
            if (!server.getUserManager().areFriends(message.getSenderId(), message.getReceiverId())) {
                sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), "You can only send images to friends."));
                return;
            }
            
            // 直接转发给接收者
            ClientHandler receiverHandler = server.getOnlineClients().get(message.getReceiverId());
            if (receiverHandler != null) {
                receiverHandler.sendMessage(message);
            } else {
                sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), "User " + message.getReceiverId() + " is offline."));
            }
        }
        // 保存聊天记录 - 只保存文件名
        String content = message.getContent();
        String fileName = content.contains(":") ? content.split(":", 2)[0] : content;
        Message historyMessage = new Message(MessageType.IMAGE_MESSAGE, message.getSenderId(), message.getReceiverId(), fileName);
        server.saveChatMessage(historyMessage);
    }

    private void handleImageRequest(Message message) throws IOException {
        // Forward the image request to the receiver
        ClientHandler receiverHandler = server.getOnlineClients().get(message.getReceiverId());
        if (receiverHandler != null) {
            receiverHandler.sendMessage(message);
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), "User " + message.getReceiverId() + " is offline. Cannot send image request."));
        }
    }

    private void handleImageAccept(Message message) throws IOException {
        // Forward the image accept message back to the sender of the image request
        ClientHandler senderHandler = server.getOnlineClients().get(message.getReceiverId()); // Receiver of IMAGE_ACCEPT is original sender of IMAGE_REQUEST
        if (senderHandler != null) {
            senderHandler.sendMessage(message);
        } else {
            System.out.println("Original sender " + message.getReceiverId() + " is offline. Cannot forward IMAGE_ACCEPT.");
        }
    }

    private void handleImageReject(Message message) throws IOException {
        // Forward the image reject message back to the sender of the image request
        ClientHandler senderHandler = server.getOnlineClients().get(message.getReceiverId()); // Receiver of IMAGE_REJECT is original sender of IMAGE_REQUEST
        if (senderHandler != null) {
            senderHandler.sendMessage(message);
        }
    }

    private void handleImageData(Message message) throws IOException {
        // Forward the actual image data to the receiver
        ClientHandler receiverHandler = server.getOnlineClients().get(message.getReceiverId());
        if (receiverHandler != null) {
            receiverHandler.sendMessage(message);
            // 提取文件名用于聊天记录
            String[] parts = message.getContent().split(":", 2);
            if (parts.length >= 1) {
                String savePathAndFileName = parts[0];
                String fileName = savePathAndFileName.substring(Math.max(savePathAndFileName.lastIndexOf("/"), savePathAndFileName.lastIndexOf("\\")) + 1);
                Message historyMessage = new Message(MessageType.IMAGE_MESSAGE, message.getSenderId(), message.getReceiverId(), fileName);
                server.saveChatMessage(historyMessage);
            }
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", message.getSenderId(), "User " + message.getReceiverId() + " is offline. Image data not sent."));
        }
    }

    private void handleGroupInvite(Message message) throws IOException {
        String inviterId = message.getSenderId();
        String invitedId = message.getReceiverId();
        String groupId = message.getContent();

        if (server.getGroupManager().sendGroupInvite(inviterId, invitedId, groupId)) {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", inviterId, "Group invite sent to " + invitedId + "."));
            ClientHandler invitedHandler = server.getOnlineClients().get(invitedId);
            if (invitedHandler != null) {
                invitedHandler.sendMessage(new Message(MessageType.GROUP_INVITE, inviterId, invitedId, groupId));
            }
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", inviterId, "Failed to send group invite to " + invitedId + "."));
        }
    }

    private void handleGroupAccept(Message message) throws IOException {
        String acceptorId = message.getSenderId();
        String groupId = message.getContent();

        // The server should add the user to the group and then notify clients.
        // The `acceptGroupInvite` method in GroupManager already handles adding the user to the group.
        if (server.getGroupManager().acceptGroupInvite(acceptorId, groupId)) {
            sendMessage(new Message(MessageType.GROUP_JOIN_SUCCESS, "Server", acceptorId, groupId));
            // Notify all group members that a new member joined
            List<String> groupMembers = server.getGroupManager().getGroupMembers(groupId);
            if (groupMembers != null) {
                for (String memberId : groupMembers) {
                    ClientHandler memberHandler = server.getOnlineClients().get(memberId);
                    if (memberHandler != null) {
                        // Send a message to all members to refresh their group list
                        memberHandler.sendGroupList(memberId);
                        if (!memberId.equals(acceptorId)) {
                            memberHandler.sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", memberId, acceptorId + " has joined group " + groupId + "."));
                        }
                    }
                }
            }
        } else {
            sendMessage(new Message(MessageType.GROUP_JOIN_FAIL, "Server", acceptorId, "Failed to join group " + groupId + "."));
        }
    }

    private void handleGroupReject(Message message) throws IOException {
        String rejectorId = message.getSenderId();
        String groupId = message.getContent();

        if (server.getGroupManager().rejectGroupInvite(rejectorId, groupId)) {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", rejectorId, "You rejected group invite to " + groupId + "."));
        } else {
            sendMessage(new Message(MessageType.SERVER_MESSAGE, "Server", rejectorId, "Failed to reject group invite to " + groupId + "."));
        }
    }

    private void handleCreateGroup(Message message) throws IOException {
        String creatorId = message.getSenderId();
        String groupId = message.getContent();
        if (server.createGroup(groupId, creatorId)) {
            sendMessage(new Message(MessageType.CREATE_GROUP_SUCCESS, "Server", creatorId, groupId));
            // Notify all online users about the new group (or just the creator)
            for (ClientHandler handler : server.getOnlineClients().values()) {
                handler.sendGroupList(handler.getUserId()); // Refresh group list for all online users
            }
        } else {
            sendMessage(new Message(MessageType.CREATE_GROUP_FAIL, "Server", creatorId, "Group ID already exists or invalid."));
        }
    }

    public void sendMessage(Message message) throws IOException {
        oos.writeObject(message);
        oos.flush();
    }

    public void sendFriendList(String userId) throws IOException {
        System.out.println("Sending friend list to user: " + userId);
        List<String> friendIds = server.getUserManager().getFriends(userId);
        System.out.println("Friend IDs for " + userId + ": " + friendIds);
        
        StringBuilder sb = new StringBuilder();
        for (String friendId : friendIds) {
            User friendUser = server.getUserManager().getUserById(friendId);
            if (friendUser != null) {
                // 检查好友是否在线
                boolean isOnline = server.isUserOnline(friendId);
                String status = isOnline ? "online" : "offline";
                String friendInfo = friendUser.getId() + ":" + friendUser.getUsername() + ":" + status;
                sb.append(friendInfo).append(";");
                System.out.println("Added friend info: " + friendInfo);
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // Remove trailing semicolon
        }
        
        String friendListContent = sb.toString();
        System.out.println("Final friend list content: " + friendListContent);
        sendMessage(new Message(MessageType.FRIEND_LIST, "Server", userId, friendListContent));
    }

    public void sendGroupList(String userId) throws IOException {
        List<String> groupIds = server.getGroupManager().getUserGroups(userId);
        StringBuilder sb = new StringBuilder();
        for (String groupId : groupIds) {
            sb.append(groupId).append(";");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        sendMessage(new Message(MessageType.GET_GROUPS, "Server", userId, sb.toString()));
    }

    public void sendPendingFriendRequests(String userId) throws IOException {
        List<String> pendingRequests = server.getUserManager().getPendingFriendRequests(userId);
        StringBuilder sb = new StringBuilder();
        for (String senderId : pendingRequests) {
            sb.append(senderId).append(";");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        // This is sent as part of GET_PENDING_REQUESTS now
    }

    public void sendPendingRequests(String userId) throws IOException {
        List<String> pendingFriendRequests = server.getUserManager().getPendingFriendRequests(userId);
        List<String> pendingGroupInvites = server.getGroupManager().getPendingGroupInvites(userId);

        StringBuilder friendReqSb = new StringBuilder();
        for (String senderId : pendingFriendRequests) {
            friendReqSb.append(senderId).append(";");
        }
        if (friendReqSb.length() > 0) {
            friendReqSb.setLength(friendReqSb.length() - 1);
        }

        StringBuilder groupInvSb = new StringBuilder();
        for (String groupId : pendingGroupInvites) {
            groupInvSb.append(groupId).append(";");
        }
        if (groupInvSb.length() > 0) {
            groupInvSb.setLength(groupInvSb.length() - 1);
        }

        String content = friendReqSb.toString() + "||" + groupInvSb.toString();
        sendMessage(new Message(MessageType.GET_PENDING_REQUESTS, "Server", userId, content));
    }

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
                    sb.append(memberUser.getId()).append(":").append(memberUser.getUsername()).append(":").append(status).append(";");
                }
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        sendMessage(new Message(MessageType.GET_GROUP_MEMBERS, groupId, requesterId, sb.toString()));
    }

    // 通知好友状态变化
    private void notifyFriendsStatusChange(String userId) {
        List<String> friends = server.getUserManager().getFriends(userId);
        for (String friendId : friends) {
            ClientHandler friendHandler = server.getOnlineClients().get(friendId);
            if (friendHandler != null) {
                try {
                    friendHandler.sendFriendList(friendId);
                } catch (IOException e) {
                    System.err.println("Failed to notify friend " + friendId + " of status change: " + e.getMessage());
                }
            }
        }
    }
}