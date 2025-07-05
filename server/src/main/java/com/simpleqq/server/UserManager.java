package com.simpleqq.server;

import com.simpleqq.common.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理器类
 * 负责用户注册、登录、好友关系管理等功能
 * 使用文件系统进行数据持久化存储
 */
public class UserManager {
    private static final String USERS_FILE = "users.txt";                    // 用户信息文件
    private static final String FRIENDSHIPS_FILE = "friendships.txt";        // 好友关系文件
    private static final String FRIEND_REQUESTS_FILE = "friend_requests.txt"; // 好友请求文件

    private Map<String, User> users;                           // 用户信息映射表，key为用户ID
    private Map<String, List<String>> friendships;            // 好友关系映射表，key为用户ID，value为好友ID列表
    private Map<String, List<String>> pendingFriendRequests;  // 待处理好友请求，key为接收者ID，value为发送者ID列表

    /**
     * 构造函数
     * 初始化数据结构并从文件加载数据
     */
    public UserManager() {
        users = new ConcurrentHashMap<>();
        friendships = new ConcurrentHashMap<>();
        pendingFriendRequests = new ConcurrentHashMap<>();
        
        // 从文件加载数据
        loadUsers();
        loadFriendships();
        loadFriendRequests();
    }

    /**
     * 从文件加载用户信息
     * 文件格式：用户ID|用户名|密码
     */
    private void loadUsers() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    User user = new User(parts[0], parts[1], parts[2]);
                    users.put(user.getId(), user);
                }
            }
            System.out.println("Loaded " + users.size() + " users.");
        } catch (FileNotFoundException e) {
            System.out.println("Users file not found. Creating a new one.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存用户信息到文件
     * 将内存中的用户数据写入文件进行持久化
     */
    private void saveUsers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (User user : users.values()) {
                writer.write(user.getId() + "|" + user.getUsername() + "|" + user.getPassword());
                writer.newLine();
            }
            System.out.println("Saved " + users.size() + " users.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文件加载好友关系
     * 文件格式：用户ID1|用户ID2（表示双向好友关系）
     */
    private void loadFriendships() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FRIENDSHIPS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    String userId1 = parts[0];
                    String userId2 = parts[1];
                    // 建立双向好友关系
                    friendships.computeIfAbsent(userId1, k -> new ArrayList<>()).add(userId2);
                    friendships.computeIfAbsent(userId2, k -> new ArrayList<>()).add(userId1);
                    System.out.println("Loaded friendship: " + userId1 + " <-> " + userId2);
                }
            }
            System.out.println("Loaded friendships. Total friendship entries: " + friendships.size());
        } catch (FileNotFoundException e) {
            System.out.println("Friendships file not found. Creating a new one.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存好友关系到文件
     * 避免重复保存双向关系，只保存字典序较小的用户ID在前的关系
     */
    private void saveFriendships() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FRIENDSHIPS_FILE))) {
            for (Map.Entry<String, List<String>> entry : friendships.entrySet()) {
                String userId1 = entry.getKey();
                for (String userId2 : entry.getValue()) {
                    // 避免重复保存，只保存一次 (userId1, userId2) 或 (userId2, userId1)
                    if (userId1.compareTo(userId2) < 0) {
                        writer.write(userId1 + "|" + userId2);
                        writer.newLine();
                        System.out.println("Saved friendship: " + userId1 + " <-> " + userId2);
                    }
                }
            }
            System.out.println("Saved friendships.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文件加载好友请求
     * 文件格式：发送者ID|接收者ID
     */
    private void loadFriendRequests() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FRIEND_REQUESTS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    // 将请求添加到接收者的待处理列表中
                    pendingFriendRequests.computeIfAbsent(parts[1], k -> new ArrayList<>()).add(parts[0]);
                }
            }
            System.out.println("Loaded friend requests.");
        } catch (FileNotFoundException e) {
            System.out.println("Friend requests file not found. Creating a new one.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存好友请求到文件
     */
    private void saveFriendRequests() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FRIEND_REQUESTS_FILE))) {
            for (Map.Entry<String, List<String>> entry : pendingFriendRequests.entrySet()) {
                String receiverId = entry.getKey();
                for (String senderId : entry.getValue()) {
                    writer.write(senderId + "|" + receiverId);
                    writer.newLine();
                }
            }
            System.out.println("Saved friend requests.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 注册新用户
     * @param id 用户ID，必须唯一
     * @param username 用户名
     * @param password 密码
     * @return 注册成功返回true，ID已存在返回false
     */
    public synchronized boolean registerUser(String id, String username, String password) {
        if (users.containsKey(id)) {
            return false; // ID已存在
        }
        User newUser = new User(id, username, password);
        users.put(id, newUser); // 立即添加到内存映射表
        saveUsers(); // 持久化到文件
        return true;
    }

    /**
     * 用户登录验证
     * @param id 用户ID
     * @param password 密码
     * @return 登录成功返回用户对象，失败返回null
     */
    public synchronized User login(String id, String password) {
        User user = users.get(id);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    /**
     * 发送好友请求
     * 将好友请求添加到待处理列表，而不是直接建立好友关系
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @return 发送成功返回true，失败返回false
     */
    public synchronized boolean sendFriendRequest(String senderId, String receiverId) {
        System.out.println("Attempting to send friend request from " + senderId + " to " + receiverId);
        
        // 验证用户存在性和有效性
        if (!users.containsKey(senderId) || !users.containsKey(receiverId) || senderId.equals(receiverId)) {
            System.out.println("Invalid users or self-request");
            return false;
        }
        
        // 检查是否已经是好友
        if (areFriends(senderId, receiverId)) {
            System.out.println("Users are already friends");
            return false;
        }
        
        // 检查是否已有待处理的请求
        if (pendingFriendRequests.containsKey(receiverId) && 
            pendingFriendRequests.get(receiverId).contains(senderId)) {
            System.out.println("Friend request already pending");
            return false;
        }

        // 添加到待处理请求列表
        pendingFriendRequests.computeIfAbsent(receiverId, k -> new ArrayList<>()).add(senderId);
        saveFriendRequests();
        System.out.println("Friend request sent successfully");
        return true;
    }

    /**
     * 接受好友请求
     * 将待处理的好友请求转换为正式的好友关系
     * @param receiverId 接收者ID（接受请求的人）
     * @param senderId 发送者ID（发送请求的人）
     * @return 接受成功返回true，失败返回false
     */
    public synchronized boolean acceptFriendRequest(String receiverId, String senderId) {
        System.out.println("Attempting to accept friend request from " + senderId + " by " + receiverId);
        
        List<String> requests = pendingFriendRequests.get(receiverId);
        if (requests != null && requests.remove(senderId)) {
            // 建立双向好友关系
            friendships.computeIfAbsent(receiverId, k -> new ArrayList<>()).add(senderId);
            friendships.computeIfAbsent(senderId, k -> new ArrayList<>()).add(receiverId);
            
            System.out.println("Added friendship: " + receiverId + " <-> " + senderId);
            System.out.println("Current friendships for " + receiverId + ": " + friendships.get(receiverId));
            System.out.println("Current friendships for " + senderId + ": " + friendships.get(senderId));
            
            saveFriendships();
            saveFriendRequests(); // 更新请求文件
            return true;
        }
        System.out.println("Failed to accept friend request - request not found");
        return false;
    }

    /**
     * 拒绝好友请求
     * 从待处理列表中移除好友请求
     * @param receiverId 接收者ID
     * @param senderId 发送者ID
     * @return 拒绝成功返回true，失败返回false
     */
    public synchronized boolean rejectFriendRequest(String receiverId, String senderId) {
        List<String> requests = pendingFriendRequests.get(receiverId);
        if (requests != null && requests.remove(senderId)) {
            saveFriendRequests(); // 更新请求文件
            return true;
        }
        return false;
    }

    /**
     * 删除好友关系
     * 从双方的好友列表中移除对方
     * @param userId1 用户1的ID
     * @param userId2 用户2的ID
     * @return 删除成功返回true，失败返回false
     */
    public synchronized boolean deleteFriend(String userId1, String userId2) {
        System.out.println("Attempting to delete friendship between " + userId1 + " and " + userId2);
        
        List<String> user1Friends = friendships.get(userId1);
        List<String> user2Friends = friendships.get(userId2);

        if (user1Friends == null || user2Friends == null) {
            System.out.println("One or both users have no friends list");
            return false;
        }

        // 从双方的好友列表中移除对方
        boolean removed1 = user1Friends.remove(userId2);
        boolean removed2 = user2Friends.remove(userId1);

        System.out.println("Removed " + userId2 + " from " + userId1 + "'s list: " + removed1);
        System.out.println("Removed " + userId1 + " from " + userId2 + "'s list: " + removed2);

        if (removed1 && removed2) {
            // 清理空的好友列表
            if (user1Friends.isEmpty()) {
                friendships.remove(userId1);
                System.out.println("Removed empty friends list for " + userId1);
            }
            if (user2Friends.isEmpty()) {
                friendships.remove(userId2);
                System.out.println("Removed empty friends list for " + userId2);
            }
            saveFriendships();
            System.out.println("Successfully deleted friendship");
            return true;
        }
        System.out.println("Failed to delete friendship");
        return false;
    }

    /**
     * 检查两个用户是否为好友关系
     * @param userId1 用户1的ID
     * @param userId2 用户2的ID
     * @return 是好友返回true，否则返回false
     */
    public boolean areFriends(String userId1, String userId2) {
        List<String> user1Friends = friendships.get(userId1);
        boolean result = user1Friends != null && user1Friends.contains(userId2);
        System.out.println("Checking if " + userId1 + " and " + userId2 + " are friends: " + result);
        if (user1Friends != null) {
            System.out.println(userId1 + "'s friends: " + user1Friends);
        }
        return result;
    }

    /**
     * 获取用户的好友列表
     * @param userId 用户ID
     * @return 好友ID列表
     */
    public List<String> getFriends(String userId) {
        List<String> friends = friendships.getOrDefault(userId, new ArrayList<>());
        System.out.println("Getting friends for " + userId + ": " + friends);
        return friends;
    }

    /**
     * 获取用户的待处理好友请求列表
     * @param userId 用户ID
     * @return 发送者ID列表
     */
    public List<String> getPendingFriendRequests(String userId) {
        return pendingFriendRequests.getOrDefault(userId, new ArrayList<>());
    }

    /**
     * 根据ID获取用户对象
     * @param id 用户ID
     * @return 用户对象，不存在返回null
     */
    public User getUserById(String id) {
        return users.get(id);
    }

    /**
     * 获取所有用户映射表
     * @return 用户映射表
     */
    public Map<String, User> getAllUsers() {
        return users;
    }
}