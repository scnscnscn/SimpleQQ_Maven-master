package com.simpleqq.server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 群组管理器类
 * 负责群组创建、成员管理、群组邀请等功能
 * 使用文件系统进行数据持久化存储
 */
public class GroupManager {
    private static final String GROUPS_FILE = "groups.txt";              // 群组信息文件
    private static final String GROUP_INVITES_FILE = "group_invites.txt"; // 群组邀请文件

    private Map<String, List<String>> groups;              // 群组映射表，key为群组ID，value为成员ID列表
    private Map<String, List<String>> pendingGroupInvites; // 待处理群组邀请，key为被邀请者ID，value为群组ID列表

    /**
     * 构造函数
     * 初始化数据结构并从文件加载数据
     */
    public GroupManager() {
        groups = new ConcurrentHashMap<>();
        pendingGroupInvites = new ConcurrentHashMap<>();
        loadGroups();
        loadGroupInvites();
    }

    /**
     * 从文件加载群组信息
     * 文件格式：群组ID|成员ID1|成员ID2|...
     */
    private void loadGroups() {
        try (BufferedReader reader = new BufferedReader(new FileReader(GROUPS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    String groupId = parts[0];
                    List<String> members = new ArrayList<>();
                    // 从第二个元素开始都是成员ID
                    for (int i = 1; i < parts.length; i++) {
                        members.add(parts[i]);
                    }
                    groups.put(groupId, members);
                }
            }
            System.out.println("Loaded " + groups.size() + " groups.");
        } catch (FileNotFoundException e) {
            System.out.println("Groups file not found. Creating a new one.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存群组信息到文件
     * 将内存中的群组数据写入文件进行持久化
     */
    private void saveGroups() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(GROUPS_FILE))) {
            for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
                StringBuilder sb = new StringBuilder(entry.getKey());
                for (String memberId : entry.getValue()) {
                    sb.append("|").append(memberId);
                }
                writer.write(sb.toString());
                writer.newLine();
            }
            System.out.println("Saved " + groups.size() + " groups.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文件加载群组邀请信息
     * 文件格式：被邀请者ID|群组ID
     */
    private void loadGroupInvites() {
        try (BufferedReader reader = new BufferedReader(new FileReader(GROUP_INVITES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    String invitedId = parts[0];
                    String groupId = parts[1];
                    pendingGroupInvites.computeIfAbsent(invitedId, k -> new ArrayList<>()).add(groupId);
                }
            }
            System.out.println("Loaded group invites.");
        } catch (FileNotFoundException e) {
            System.out.println("Group invites file not found. Creating a new one.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存群组邀请信息到文件
     */
    private void saveGroupInvites() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(GROUP_INVITES_FILE))) {
            for (Map.Entry<String, List<String>> entry : pendingGroupInvites.entrySet()) {
                String invitedId = entry.getKey();
                for (String groupId : entry.getValue()) {
                    writer.write(invitedId + "|" + groupId);
                    writer.newLine();
                }
            }
            System.out.println("Saved group invites.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建新群组
     * @param groupId 群组ID，必须唯一
     * @param creatorId 创建者用户ID，自动成为群组第一个成员
     * @return 创建成功返回true，群组ID已存在返回false
     */
    public synchronized boolean createGroup(String groupId, String creatorId) {
        if (groups.containsKey(groupId)) {
            return false; // 群组ID已存在
        }
        List<String> members = new ArrayList<>();
        members.add(creatorId); // 创建者自动成为第一个成员
        groups.put(groupId, members);
        saveGroups();
        return true;
    }

    /**
     * 发送群组邀请
     * @param inviterId 邀请者ID
     * @param invitedId 被邀请者ID
     * @param groupId 群组ID
     * @return 邀请发送成功返回true，失败返回false
     */
    public synchronized boolean sendGroupInvite(String inviterId, String invitedId, String groupId) {
        // 检查群组是否存在
        if (!groups.containsKey(groupId)) {
            return false; // 群组不存在
        }
        
        // 检查被邀请用户是否已经是群成员
        if (groups.get(groupId).contains(invitedId)) {
            return false; // 用户已经是群成员
        }

        // 允许重新发送邀请（简化处理，实际应用中可能需要更复杂的逻辑）
        if (pendingGroupInvites.containsKey(invitedId) && 
            pendingGroupInvites.get(invitedId).contains(groupId)) {
            return true; // 邀请已存在，但允许重新发送
        }

        // 添加到待处理邀请列表
        pendingGroupInvites.computeIfAbsent(invitedId, k -> new ArrayList<>()).add(groupId);
        saveGroupInvites();
        return true;
    }

    /**
     * 接受群组邀请
     * 将用户添加到群组成员列表并移除待处理邀请
     * @param invitedId 被邀请者ID
     * @param groupId 群组ID
     * @return 接受成功返回true，失败返回false
     */
    public synchronized boolean acceptGroupInvite(String invitedId, String groupId) {
        List<String> invites = pendingGroupInvites.get(invitedId);
        if (invites != null && invites.remove(groupId)) {
            // 将用户添加到群组成员列表
            groups.computeIfAbsent(groupId, k -> new ArrayList<>()).add(invitedId);
            saveGroups();
            saveGroupInvites();
            return true;
        }
        return false;
    }

    /**
     * 拒绝群组邀请
     * 从待处理邀请列表中移除邀请
     * @param invitedId 被邀请者ID
     * @param groupId 群组ID
     * @return 拒绝成功返回true，失败返回false
     */
    public synchronized boolean rejectGroupInvite(String invitedId, String groupId) {
        List<String> invites = pendingGroupInvites.get(invitedId);
        if (invites != null && invites.remove(groupId)) {
            saveGroupInvites();
            return true;
        }
        return false;
    }

    /**
     * 获取群组成员列表
     * @param groupId 群组ID
     * @return 成员ID列表，群组不存在返回null
     */
    public List<String> getGroupMembers(String groupId) {
        return groups.get(groupId);
    }

    /**
     * 获取用户的待处理群组邀请列表
     * @param userId 用户ID
     * @return 群组ID列表
     */
    public List<String> getPendingGroupInvites(String userId) {
        return pendingGroupInvites.getOrDefault(userId, new ArrayList<>());
    }

    /**
     * 获取用户加入的所有群组列表
     * 通过遍历所有群组找出包含指定用户的群组
     * @param userId 用户ID
     * @return 群组ID列表
     */
    public List<String> getUserGroups(String userId) {
        return groups.entrySet().stream()
                .filter(entry -> entry.getValue().contains(userId)) // 过滤包含该用户的群组
                .map(Map.Entry::getKey)                              // 提取群组ID
                .collect(Collectors.toList());                       // 收集为列表
    }

    /**
     * 获取所有群组映射表
     * @return 群组映射表
     */
    public Map<String, List<String>> getAllGroups() {
        return groups;
    }
}