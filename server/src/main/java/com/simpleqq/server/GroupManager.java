package com.simpleqq.server;

import com.simpleqq.common.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GroupManager {
    private static final String GROUPS_FILE = "groups.txt";
    private static final String GROUP_INVITES_FILE = "group_invites.txt";

    // groupId -> List of memberIds
    private Map<String, List<String>> groups;
    // invitedId -> List of groupId
    private Map<String, List<String>> pendingGroupInvites;

    public GroupManager() {
        groups = new ConcurrentHashMap<>();
        pendingGroupInvites = new ConcurrentHashMap<>();
        loadGroups();
        loadGroupInvites();
    }

    private void loadGroups() {
        try (BufferedReader reader = new BufferedReader(new FileReader(GROUPS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    String groupId = parts[0];
                    List<String> members = new ArrayList<>();
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

    public synchronized boolean createGroup(String groupId, String creatorId) {
        if (groups.containsKey(groupId)) {
            return false; // Group ID already exists
        }
        List<String> members = new ArrayList<>();
        members.add(creatorId);
        groups.put(groupId, members);
        saveGroups();
        return true;
    }

    public synchronized boolean sendGroupInvite(String inviterId, String invitedId, String groupId) {
        // Check if group exists
        if (!groups.containsKey(groupId)) {
            return false; // Group does not exist
        }
        // Check if invited user is already a member
        if (groups.get(groupId).contains(invitedId)) {
            return false; // User is already a member
        }
        // Check if invite already pending - REMOVED TO ALLOW RESENDING INVITES
        // if (pendingGroupInvites.containsKey(invitedId) && pendingGroupInvites.get(invitedId).contains(groupId)) {
        //     return false; // Invite already pending
        // }

        // Allow resending invite if not already accepted
        if (pendingGroupInvites.containsKey(invitedId) && pendingGroupInvites.get(invitedId).contains(groupId)) {
            return true; // Invite already pending, but allow resend for simplicity
        }

        pendingGroupInvites.computeIfAbsent(invitedId, k -> new ArrayList<>()).add(groupId);
        saveGroupInvites();
        return true;
    }

    public synchronized boolean acceptGroupInvite(String invitedId, String groupId) {
        List<String> invites = pendingGroupInvites.get(invitedId);
        if (invites != null && invites.remove(groupId)) {
            groups.computeIfAbsent(groupId, k -> new ArrayList<>()).add(invitedId);
            saveGroups();
            saveGroupInvites();
            return true;
        }
        return false;
    }

    public synchronized boolean rejectGroupInvite(String invitedId, String groupId) {
        List<String> invites = pendingGroupInvites.get(invitedId);
        if (invites != null && invites.remove(groupId)) {
            saveGroupInvites();
            return true;
        }
        return false;
    }

    public List<String> getGroupMembers(String groupId) {
        return groups.get(groupId);
    }

    public List<String> getPendingGroupInvites(String userId) {
        return pendingGroupInvites.getOrDefault(userId, new ArrayList<>());
    }

    public List<String> getUserGroups(String userId) {
        return groups.entrySet().stream()
                .filter(entry -> entry.getValue().contains(userId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Map<String, List<String>> getAllGroups() {
        return groups;
    }
}


