package com.simpleqq.client;

import com.simpleqq.common.Message;
import com.simpleqq.common.MessageType;
import com.simpleqq.common.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主聊天窗口类
 * 应用程序的主界面，管理好友列表、群组列表和各种聊天功能
 * 提供好友管理、群组管理、消息处理等核心功能
 */
public class ChatWindow extends JFrame {
    private Client client;                                    // 客户端连接对象
    private JTabbedPane tabbedPane;                          // 主标签页容器
    private JList<String> friendList;                       // 好友列表组件
    private JList<String> groupList;                        // 群组列表组件
    private DefaultListModel<String> friendListModel;       // 好友列表数据模型
    private DefaultListModel<String> groupListModel;        // 群组列表数据模型
    private Map<String, SingleChatWindow> singleChatWindows; // 私聊窗口管理器
    private Map<String, GroupChatWindow> groupChatWindows;   // 群聊窗口管理器

    // 请求处理相关组件
    private JPanel requestPanel;                             // 请求处理面板
    private DefaultListModel<String> friendRequestListModel; // 好友请求列表模型
    private JList<String> friendRequestList;                // 好友请求列表
    private DefaultListModel<String> groupInviteListModel;  // 群组邀请列表模型
    private JList<String> groupInviteList;                  // 群组邀请列表

    // 刷新按钮
    private JButton refreshFriendsButton;                    // 刷新好友列表按钮
    private JButton refreshGroupsButton;                     // 刷新群组列表按钮

    /**
     * 构造函数
     * @param client 客户端对象，用于网络通信
     */
    public ChatWindow(Client client) {
        this.client = client;
        this.singleChatWindows = new HashMap<>();
        this.groupChatWindows = new HashMap<>();

        initializeUI();
        setupMessageListener();
        requestInitialData();
        setupWindowCloseHandler();
    }

    /**
     * 初始化用户界面
     * 设置窗口基本属性和布局结构
     */
    private void initializeUI() {
        setTitle("QQ - " + client.getCurrentUser().getUsername() + " (" + client.getCurrentUser().getId() + ")");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        // 创建左侧面板，包含好友列表、群组列表和请求处理
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 0));
        tabbedPane = new JTabbedPane();

        setupFriendsTab();    // 设置好友标签页
        setupGroupsTab();     // 设置群组标签页
        setupRequestsTab();   // 设置请求处理标签页

        leftPanel.add(tabbedPane, BorderLayout.CENTER);

        // 创建功能按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1));
        JButton addFriendButton = new JButton("添加好友");
        JButton deleteFriendButton = new JButton("删除好友");
        JButton createGroupButton = new JButton("创建群聊");

        addFriendButton.addActionListener(e -> addFriend());
        deleteFriendButton.addActionListener(e -> deleteFriend());
        createGroupButton.addActionListener(e -> createGroup());

        buttonPanel.add(addFriendButton);
        buttonPanel.add(deleteFriendButton);
        buttonPanel.add(createGroupButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(leftPanel, BorderLayout.WEST);

        // 创建右侧占位面板
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("请选择一个好友或群组开始聊天", SwingConstants.CENTER), BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.CENTER);
    }

    /**
     * 设置好友标签页
     * 创建好友列表和相关控件
     */
    private void setupFriendsTab() {
        JPanel friendsPanel = new JPanel(new BorderLayout());
        friendListModel = new DefaultListModel<>();
        friendList = new JList<>(friendListModel);
        friendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 好友列表选择事件：双击打开私聊窗口
        friendList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedFriend = friendList.getSelectedValue();
                if (selectedFriend != null && !selectedFriend.trim().isEmpty()) {
                    // 从显示文本中提取好友ID（格式：ID username (status)）
                    String friendId = selectedFriend.split(" ")[0];
                    openSingleChatWindow(friendId);
                }
            }
        });
        
        friendsPanel.add(new JScrollPane(friendList), BorderLayout.CENTER);
        
        // 添加刷新好友列表按钮
        refreshFriendsButton = new JButton("刷新好友");
        refreshFriendsButton.addActionListener(e -> refreshFriendList());
        friendsPanel.add(refreshFriendsButton, BorderLayout.SOUTH);
        
        tabbedPane.addTab("好友", friendsPanel);
    }

    /**
     * 设置群组标签页
     * 创建群组列表和相关控件
     */
    private void setupGroupsTab() {
        JPanel groupsPanel = new JPanel(new BorderLayout());
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 群组列表选择事件：双击打开群聊窗口
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedGroup = groupList.getSelectedValue();
                if (selectedGroup != null && !selectedGroup.trim().isEmpty()) {
                    String groupId = selectedGroup.split(" ")[0];
                    openGroupChatWindow(groupId);
                }
            }
        });
        
        groupsPanel.add(new JScrollPane(groupList), BorderLayout.CENTER);
        
        // 添加刷新群组列表按钮
        refreshGroupsButton = new JButton("刷新群聊");
        refreshGroupsButton.addActionListener(e -> refreshGroupList());
        groupsPanel.add(refreshGroupsButton, BorderLayout.SOUTH);
        
        tabbedPane.addTab("群聊", groupsPanel);
    }

    /**
     * 设置请求处理标签页
     * 创建好友请求和群组邀请的处理界面
     */
    private void setupRequestsTab() {
        requestPanel = new JPanel(new BorderLayout());
        JTabbedPane requestTabbedPane = new JTabbedPane();

        // 好友请求子标签页
        friendRequestListModel = new DefaultListModel<>();
        friendRequestList = new JList<>(friendRequestListModel);
        friendRequestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTabbedPane.addTab("好友请求", new JScrollPane(friendRequestList));

        // 群组邀请子标签页
        groupInviteListModel = new DefaultListModel<>();
        groupInviteList = new JList<>(groupInviteListModel);
        groupInviteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTabbedPane.addTab("群聊邀请", new JScrollPane(groupInviteList));

        requestPanel.add(requestTabbedPane, BorderLayout.CENTER);

        // 请求处理按钮面板
        JPanel requestButtonPanel = new JPanel(new GridLayout(2, 2));
        JButton acceptFriendRequestButton = new JButton("接受好友");
        JButton rejectFriendRequestButton = new JButton("拒绝好友");
        JButton acceptGroupInviteButton = new JButton("接受群聊");
        JButton rejectGroupInviteButton = new JButton("拒绝群聊");

        acceptFriendRequestButton.addActionListener(e -> handleFriendRequestAction(true));
        rejectFriendRequestButton.addActionListener(e -> handleFriendRequestAction(false));
        acceptGroupInviteButton.addActionListener(e -> handleGroupInviteAction(true));
        rejectGroupInviteButton.addActionListener(e -> handleGroupInviteAction(false));

        requestButtonPanel.add(acceptFriendRequestButton);
        requestButtonPanel.add(rejectFriendRequestButton);
        requestButtonPanel.add(acceptGroupInviteButton);
        requestButtonPanel.add(rejectGroupInviteButton);
        requestPanel.add(requestButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("请求", requestPanel);
    }

    /**
     * 添加好友功能
     * 弹出输入框让用户输入好友ID并发送好友请求
     */
    private void addFriend() {
        String friendId = JOptionPane.showInputDialog(this, "请输入好友ID:");
        if (friendId != null && !friendId.trim().isEmpty()) {
            // 防止添加自己为好友
            if (friendId.equals(client.getCurrentUser().getId())) {
                JOptionPane.showMessageDialog(this, "不能添加自己为好友！");
                return;
            }
            // 发送好友请求
            client.sendMessage(new Message(MessageType.FRIEND_REQUEST, client.getCurrentUser().getId(), friendId, ""));
            JOptionPane.showMessageDialog(this, "好友请求已发送！");
        }
    }

    /**
     * 删除好友功能
     * 删除当前选中的好友并关闭相关聊天窗口
     */
    private void deleteFriend() {
        String selectedFriend = friendList.getSelectedValue();
        if (selectedFriend != null && !selectedFriend.trim().isEmpty()) {
            String friendId = selectedFriend.split(" ")[0];
            int confirm = JOptionPane.showConfirmDialog(this, 
                "确定要删除好友 " + friendId + " 吗？", "删除好友", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                // 发送删除好友请求
                client.sendMessage(new Message(MessageType.DELETE_FRIEND, client.getCurrentUser().getId(), friendId, ""));
                // 关闭与该好友的聊天窗口
                closeSingleChatWindow(friendId);
            }
        } else {
            JOptionPane.showMessageDialog(this, "请选择要删除的好友。");
        }
    }

    /**
     * 创建群组功能
     * 弹出输入框让用户输入群组ID并创建新群组
     */
    private void createGroup() {
        String groupId = JOptionPane.showInputDialog(this, "请输入群聊ID:");
        if (groupId != null && !groupId.trim().isEmpty()) {
            // 发送创建群组请求
            client.sendMessage(new Message(MessageType.CREATE_GROUP, client.getCurrentUser().getId(), "Server", groupId));
        }
    }

    /**
     * 请求初始数据
     * 登录后立即获取好友列表、群组列表和待处理请求
     */
    private void requestInitialData() {
        client.sendMessage(new Message(MessageType.FRIEND_LIST, client.getCurrentUser().getId(), "Server", ""));
        client.sendMessage(new Message(MessageType.GET_GROUPS, client.getCurrentUser().getId(), "Server", ""));
        client.sendMessage(new Message(MessageType.GET_PENDING_REQUESTS, client.getCurrentUser().getId(), "Server", ""));
    }

    /**
     * 设置消息监听器
     * 处理从服务器接收到的各种消息类型
     */
    private void setupMessageListener() {
        client.setMessageListener(message -> {
            SwingUtilities.invokeLater(() -> {
                handleIncomingMessage(message);
            });
        });
    }

    /**
     * 处理接收到的消息
     * 根据消息类型分发到相应的处理方法
     * @param message 接收到的消息对象
     */
    private void handleIncomingMessage(Message message) {
        switch (message.getType()) {
            case FRIEND_LIST:
                updateFriendList(message.getContent());
                break;
            case TEXT_MESSAGE:
            case IMAGE_MESSAGE:
                handleChatMessage(message);
                break;
            case GROUP_MESSAGE:
                openGroupChatWindow(message.getReceiverId()).displayMessage(message);
                break;
            case ADD_FRIEND_SUCCESS:
                JOptionPane.showMessageDialog(this, "添加好友成功: " + message.getContent());
                refreshFriendList();
                break;
            case ADD_FRIEND_FAIL:
                JOptionPane.showMessageDialog(this, "添加好友失败: " + message.getContent());
                break;
            case DELETE_FRIEND_SUCCESS:
                JOptionPane.showMessageDialog(this, "删除好友成功: " + message.getContent());
                refreshFriendList();
                break;
            case DELETE_FRIEND_FAIL:
                JOptionPane.showMessageDialog(this, "删除好友失败: " + message.getContent());
                break;
            case SERVER_MESSAGE:
                JOptionPane.showMessageDialog(this, "服务器消息: " + message.getContent());
                break;
            case FRIEND_REQUEST:
                handleFriendRequest(message);
                break;
            case FRIEND_ACCEPT:
                JOptionPane.showMessageDialog(this, message.getSenderId() + " 接受了您的好友请求。");
                refreshFriendList();
                break;
            case FRIEND_REJECT:
                JOptionPane.showMessageDialog(this, message.getSenderId() + " 拒绝了您的好友请求。");
                break;
            case GROUP_INVITE:
                handleGroupInvite(message);
                break;
            case GET_GROUPS:
                updateGroupList(message.getContent());
                break;
            case GET_PENDING_REQUESTS:
                updatePendingRequests(message.getContent());
                break;
            case CREATE_GROUP_SUCCESS:
                JOptionPane.showMessageDialog(this, "群聊创建成功: " + message.getContent());
                refreshGroupList();
                break;
            case CREATE_GROUP_FAIL:
                JOptionPane.showMessageDialog(this, "群聊创建失败: " + message.getContent());
                break;
            case GROUP_JOIN_SUCCESS:
                JOptionPane.showMessageDialog(this, "成功加入群聊: " + message.getContent());
                refreshGroupList();
                break;
            case GROUP_JOIN_FAIL:
                JOptionPane.showMessageDialog(this, "加入群聊失败: " + message.getContent());
                break;
            case GET_GROUP_MEMBERS:
                updateGroupMembers(message);
                break;
            default:
                System.out.println("Unhandled message type in ChatWindow: " + message.getType());
        }
    }

    /**
     * 处理聊天消息
     * 根据发送者和接收者确定是否打开相应的聊天窗口
     * @param message 聊天消息对象
     */
    private void handleChatMessage(Message message) {
        String sender = message.getSenderId();
        String receiver = message.getReceiverId();
        String currentUserId = client.getCurrentUser().getId();

        if (receiver.equals(currentUserId)) {
            // 接收到的消息，打开与发送者的聊天窗口
            openSingleChatWindow(sender).displayMessage(message);
        } else if (sender.equals(currentUserId)) {
            // 自己发送的消息，打开与接收者的聊天窗口
            openSingleChatWindow(receiver).displayMessage(message);
        }
    }

    /**
     * 处理好友请求
     * 将新的好友请求添加到请求列表并显示通知
     * @param message 好友请求消息
     */
    private void handleFriendRequest(Message message) {
        String friendRequestSenderId = message.getSenderId();
        if (!friendRequestListModel.contains(friendRequestSenderId)) {
            friendRequestListModel.addElement(friendRequestSenderId);
            JOptionPane.showMessageDialog(this, "您收到一条好友请求来自: " + friendRequestSenderId);
        }
    }

    /**
     * 处理群组邀请
     * 将新的群组邀请添加到邀请列表并显示通知
     * @param message 群组邀请消息
     */
    private void handleGroupInvite(Message message) {
        String groupInviteGroupId = message.getContent();
        String inviterId = message.getSenderId();
        String inviteDisplay = groupInviteGroupId + " (来自 " + inviterId + ")";
        if (!groupInviteListModel.contains(inviteDisplay)) {
            groupInviteListModel.addElement(inviteDisplay);
            JOptionPane.showMessageDialog(this, "您收到一条群聊邀请来自 " + inviterId + " 加入群聊: " + groupInviteGroupId);
        }
    }

    /**
     * 更新群组成员信息
     * 将服务器返回的群组成员列表更新到相应的群聊窗口
     * @param message 包含群组成员信息的消息
     */
    private void updateGroupMembers(Message message) {
        String groupId = message.getSenderId();
        GroupChatWindow groupWindow = groupChatWindows.get(groupId);
        if (groupWindow != null) {
            String membersStr = message.getContent();
            java.util.List<String> members = new java.util.ArrayList<>();
            if (membersStr != null && !membersStr.isEmpty()) {
                String[] memberInfos = membersStr.split(";");
                for (String memberInfo : memberInfos) {
                    String[] parts = memberInfo.split(":");
                    if (parts.length >= 2) {
                        members.add(parts[0] + " " + parts[1]); // ID + username
                    }
                }
            }
            groupWindow.updateGroupMembers(members);
        }
    }

    /**
     * 更新好友列表
     * 解析服务器返回的好友列表数据并更新界面
     * @param friendListStr 好友列表字符串，格式：id:username:status;id:username:status;...
     */
    private void updateFriendList(String friendListStr) {
        friendListModel.clear();
        if (friendListStr != null && !friendListStr.isEmpty()) {
            String[] friends = friendListStr.split(";");
            for (String friendInfo : friends) {
                String[] parts = friendInfo.split(":");
                if (parts.length == 3) {
                    String id = parts[0];
                    String username = parts[1];
                    String status = parts[2];
                    String displayText = id + " " + username + " (" + status + ")";
                    friendListModel.addElement(displayText);
                }
            }
        }
        // 强制刷新界面
        friendList.revalidate();
        friendList.repaint();
    }

    /**
     * 更新群组列表
     * 解析服务器返回的群组列表数据并更新界面
     * @param groupListStr 群组列表字符串，格式：groupId;groupId;...
     */
    private void updateGroupList(String groupListStr) {
        groupListModel.clear();
        if (groupListStr != null && !groupListStr.isEmpty()) {
            String[] groups = groupListStr.split(";");
            for (String groupId : groups) {
                groupListModel.addElement(groupId);
            }
        }
        // 强制刷新界面
        groupList.revalidate();
        groupList.repaint();
    }

    /**
     * 更新待处理请求列表
     * 解析服务器返回的待处理请求数据并更新界面
     * @param pendingRequestsStr 待处理请求字符串，格式：friendRequests||groupInvites
     */
    private void updatePendingRequests(String pendingRequestsStr) {
        friendRequestListModel.clear();
        groupInviteListModel.clear();

        if (pendingRequestsStr != null && !pendingRequestsStr.isEmpty()) {
            String[] parts = pendingRequestsStr.split("\\|\\|");
            String friendRequests = parts.length > 0 ? parts[0] : "";
            String groupInvites = parts.length > 1 ? parts[1] : "";

            // 处理好友请求
            if (!friendRequests.isEmpty()) {
                String[] requests = friendRequests.split(";");
                for (String senderId : requests) {
                    friendRequestListModel.addElement(senderId);
                }
            }

            // 处理群组邀请
            if (!groupInvites.isEmpty()) {
                String[] invites = groupInvites.split(";");
                for (String inviteInfo : invites) {
                    groupInviteListModel.addElement(inviteInfo);
                }
            }
        }
    }

    /**
     * 打开私聊窗口
     * 如果窗口不存在则创建新窗口，否则将现有窗口置于前台
     * @param friendId 好友ID
     * @return 私聊窗口对象
     */
    private SingleChatWindow openSingleChatWindow(String friendId) {
        if (friendId == null || friendId.trim().isEmpty()) {
            return null;
        }
        
        SingleChatWindow chatWindow = singleChatWindows.get(friendId);
        if (chatWindow == null) {
            // 创建新的私聊窗口
            chatWindow = new SingleChatWindow(client, friendId);
            singleChatWindows.put(friendId, chatWindow);
            chatWindow.setVisible(true);
            
            // 设置窗口关闭监听器，窗口关闭时从管理器中移除
            chatWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    singleChatWindows.remove(friendId);
                }
            });
        } else {
            // 将现有窗口置于前台
            chatWindow.toFront();
            chatWindow.setVisible(true);
        }
        return chatWindow;
    }

    /**
     * 关闭私聊窗口
     * 强制关闭指定好友的私聊窗口
     * @param friendId 好友ID
     */
    private void closeSingleChatWindow(String friendId) {
        SingleChatWindow chatWindow = singleChatWindows.get(friendId);
        if (chatWindow != null) {
            chatWindow.dispose();
            singleChatWindows.remove(friendId);
        }
    }

    /**
     * 打开群聊窗口
     * 如果窗口不存在则创建新窗口，否则将现有窗口置于前台
     * @param groupId 群组ID
     * @return 群聊窗口对象
     */
    private GroupChatWindow openGroupChatWindow(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return null;
        }
        
        GroupChatWindow chatWindow = groupChatWindows.get(groupId);
        if (chatWindow == null) {
            // 创建新的群聊窗口
            chatWindow = new GroupChatWindow(client, groupId);
            groupChatWindows.put(groupId, chatWindow);
            chatWindow.setVisible(true);
            
            // 设置窗口关闭监听器，窗口关闭时从管理器中移除
            chatWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    groupChatWindows.remove(groupId);
                }
            });
        } else {
            // 将现有窗口置于前台
            chatWindow.toFront();
            chatWindow.setVisible(true);
        }
        return chatWindow;
    }

    /**
     * 处理好友请求操作
     * 接受或拒绝选中的好友请求
     * @param accept true表示接受，false表示拒绝
     */
    private void handleFriendRequestAction(boolean accept) {
        String selectedRequest = friendRequestList.getSelectedValue();
        if (selectedRequest != null) {
            String senderId = selectedRequest;
            if (accept) {
                client.sendMessage(new Message(MessageType.FRIEND_ACCEPT, client.getCurrentUser().getId(), senderId, ""));
            } else {
                client.sendMessage(new Message(MessageType.FRIEND_REJECT, client.getCurrentUser().getId(), senderId, ""));
            }
            // 从列表中移除已处理的请求
            friendRequestListModel.removeElement(selectedRequest);
        } else {
            JOptionPane.showMessageDialog(this, "请选择一个好友请求。");
        }
    }

    /**
     * 处理群组邀请操作
     * 接受或拒绝选中的群组邀请
     * @param accept true表示接受，false表示拒绝
     */
    private void handleGroupInviteAction(boolean accept) {
        String selectedInvite = groupInviteList.getSelectedValue();
        if (selectedInvite != null) {
            // 从显示文本中提取群组ID和邀请者ID
            String groupId = selectedInvite.split(" \\(")[0];
            String inviterId = selectedInvite.substring(selectedInvite.indexOf("来自 ") + 3, selectedInvite.indexOf(")"));

            if (accept) {
                client.sendMessage(new Message(MessageType.GROUP_ACCEPT, client.getCurrentUser().getId(), inviterId, groupId));
            } else {
                client.sendMessage(new Message(MessageType.GROUP_REJECT, client.getCurrentUser().getId(), inviterId, groupId));
            }
            // 从列表中移除已处理的邀请
            groupInviteListModel.removeElement(selectedInvite);
        } else {
            JOptionPane.showMessageDialog(this, "请选择一个群聊邀请。");
        }
    }

    /**
     * 刷新好友列表
     * 向服务器请求最新的好友列表数据
     */
    private void refreshFriendList() {
        client.sendMessage(new Message(MessageType.FRIEND_LIST, client.getCurrentUser().getId(), "Server", ""));
    }

    /**
     * 刷新群组列表
     * 向服务器请求最新的群组列表数据
     */
    private void refreshGroupList() {
        client.sendMessage(new Message(MessageType.GET_GROUPS, client.getCurrentUser().getId(), "Server", ""));
    }

    /**
     * 设置窗口关闭处理器
     * 窗口关闭时断开与服务器的连接
     */
    private void setupWindowCloseHandler() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.disconnect(); // 断开服务器连接
            }
        });
    }
}