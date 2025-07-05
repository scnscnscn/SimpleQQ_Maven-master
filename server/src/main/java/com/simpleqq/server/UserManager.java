package com.simpleqq.server;

import com.simpleqq.common.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private static final String USERS_FILE = "users.txt";
    private static final String FRIENDSHIPS_FILE = "friendships.txt";
    private static final String FRIEND_REQUESTS_FILE = "friend_requests.txt";

    private Map<String, User> users;
    private Map<String, List<String>> friendships;
    private Map<String, List<String>> pendingFriendRequests; // receiverId -> list of senderIds

    public UserManager() {
        users = new ConcurrentHashMap<>();
        friendships = new ConcurrentHashMap<>();
        pendingFriendRequests = new ConcurrentHashMap<>();
        loadUsers();
        loadFriendships();
        loadFriendRequests();
    }

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

    private void loadFriendships() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FRIENDSHIPS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    String userId1 = parts[0];
                    String userId2 = parts[1];
                    friendships.computeIfAbsent(userId1, k -> new ArrayList<>()).add(userId2);
                    friendships.computeIfAbsent(userId2, k -> new ArrayList<>()).add(userId1); // 双向好友
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

    private void loadFriendRequests() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FRIEND_REQUESTS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
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

    public synchronized boolean registerUser(String id, String username, String password) {
        if (users.containsKey(id)) {
            return false; // ID already exists
        }
        User newUser = new User(id, username, password);
        users.put(id, newUser); // Add to in-memory map immediately
        saveUsers();
        return true;
    }

    public synchronized User login(String id, String password) {
        User user = users.get(id);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    // Modified addFriend to send a request instead of direct add
    public synchronized boolean sendFriendRequest(String senderId, String receiverId) {
        System.out.println("Attempting to send friend request from " + senderId + " to " + receiverId);
        
        if (!users.containsKey(senderId) || !users.containsKey(receiverId) || senderId.equals(receiverId)) {
            System.out.println("Invalid users or self-request");
            return false; // User(s) not found or cannot add self
        }
        // Check if already friends
        if (areFriends(senderId, receiverId)) {
            System.out.println("Users are already friends");
            return false; // Already friends
        }
        // Check if request already sent
        if (pendingFriendRequests.containsKey(receiverId) && pendingFriendRequests.get(receiverId).contains(senderId)) {
            System.out.println("Friend request already pending");
            return false; // Request already pending
        }

        pendingFriendRequests.computeIfAbsent(receiverId, k -> new ArrayList<>()).add(senderId);
        saveFriendRequests();
        System.out.println("Friend request sent successfully");
        return true;
    }

    public synchronized boolean acceptFriendRequest(String receiverId, String senderId) {
        System.out.println("Attempting to accept friend request from " + senderId + " by " + receiverId);
        
        List<String> requests = pendingFriendRequests.get(receiverId);
        if (requests != null && requests.remove(senderId)) {
            // Add to friendships - 确保双向添加
            friendships.computeIfAbsent(receiverId, k -> new ArrayList<>()).add(senderId);
            friendships.computeIfAbsent(senderId, k -> new ArrayList<>()).add(receiverId);
            
            System.out.println("Added friendship: " + receiverId + " <-> " + senderId);
            System.out.println("Current friendships for " + receiverId + ": " + friendships.get(receiverId));
            System.out.println("Current friendships for " + senderId + ": " + friendships.get(senderId));
            
            saveFriendships();
            saveFriendRequests(); // Update requests file
            return true;
        }
        System.out.println("Failed to accept friend request - request not found");
        return false;
    }

    public synchronized boolean rejectFriendRequest(String receiverId, String senderId) {
        List<String> requests = pendingFriendRequests.get(receiverId);
        if (requests != null && requests.remove(senderId)) {
            saveFriendRequests(); // Update requests file
            return true;
        }
        return false;
    }

    public synchronized boolean deleteFriend(String userId1, String userId2) {
        System.out.println("Attempting to delete friendship between " + userId1 + " and " + userId2);
        
        List<String> user1Friends = friendships.get(userId1);
        List<String> user2Friends = friendships.get(userId2);

        if (user1Friends == null || user2Friends == null) {
            System.out.println("One or both users have no friends list");
            return false; // Not friends or user(s) not found
        }

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

    public boolean areFriends(String userId1, String userId2) {
        List<String> user1Friends = friendships.get(userId1);
        boolean result = user1Friends != null && user1Friends.contains(userId2);
        System.out.println("Checking if " + userId1 + " and " + userId2 + " are friends: " + result);
        if (user1Friends != null) {
            System.out.println(userId1 + "'s friends: " + user1Friends);
        }
        return result;
    }

    public List<String> getFriends(String userId) {
        List<String> friends = friendships.getOrDefault(userId, new ArrayList<>());
        System.out.println("Getting friends for " + userId + ": " + friends);
        return friends;
    }

    public List<String> getPendingFriendRequests(String userId) {
        return pendingFriendRequests.getOrDefault(userId, new ArrayList<>());
    }

    public User getUserById(String id) {
        return users.get(id);
    }

    public Map<String, User> getAllUsers() {
        return users;
    }
}