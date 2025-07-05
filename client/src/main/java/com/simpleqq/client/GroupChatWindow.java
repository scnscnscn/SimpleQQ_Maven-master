package com.simpleqq.client;

import com.simpleqq.common.Message;
import com.simpleqq.common.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * 群聊窗口类
 * 提供群组聊天功能，支持多人文本和图片消息交流
 * 包含群成员管理、邀请功能等
 */
public class GroupChatWindow extends JFrame {
    private Client client;                           // 客户端连接对象
    private String groupId;                          // 群组ID
    private JTextArea chatArea;                      // 聊天内容显示区域
    private JTextField messageField;                 // 消息输入框
    private JButton sendButton;                      // 发送文本消息按钮
    private JButton sendImageButton;                 // 发送图片按钮
    private JButton inviteMemberButton;              // 邀请成员按钮
    private JButton refreshMembersButton;            // 刷新成员列表按钮
    private JList<String> memberList;               // 群成员列表组件
    private DefaultListModel<String> memberListModel; // 群成员列表数据模型
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss"); // 时间格式化器

    /**
     * 构造函数
     * @param client 客户端对象
     * @param groupId 群组ID
     */
    public GroupChatWindow(Client client, String groupId) {
        this.client = client;
        this.groupId = groupId;
        
        initializeUI();
        setupEventHandlers();
        loadChatHistory();
        requestGroupMembers();
        setupWindowCloseHandler();
    }

    /**
     * 初始化用户界面
     * 设置窗口布局和组件
     */
    private void initializeUI() {
        setTitle("群聊: " + groupId + " - " + client.getCurrentUser().getUsername());
        setSize(700, 500); // 增加窗口大小以容纳成员列表
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        // 创建聊天区域面板
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);        // 设置为只读
        chatArea.setLineWrap(true);         // 启用自动换行
        chatArea.setWrapStyleWord(true);    // 按单词换行
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // 创建输入面板
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("发送");
        sendImageButton = new JButton("发送图片");

        JPanel sendButtonPanel = new JPanel(new GridLayout(1, 2));
        sendButtonPanel.add(sendButton);
        sendButtonPanel.add(sendImageButton);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButtonPanel, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        mainPanel.add(chatPanel, BorderLayout.CENTER);

        // 创建成员列表面板（右侧）
        setupMemberListPanel(mainPanel);
    }

    /**
     * 设置成员列表面板
     * 创建群成员列表和相关操作按钮
     * @param mainPanel 主面板容器
     */
    private void setupMemberListPanel(JPanel mainPanel) {
        JPanel memberListPanel = new JPanel(new BorderLayout());
        memberListPanel.setPreferredSize(new Dimension(150, 0));
        memberListPanel.setBorder(BorderFactory.createTitledBorder("群成员"));

        // 创建成员列表
        memberListModel = new DefaultListModel<>();
        memberList = new JList<>(memberListModel);
        memberList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        memberListPanel.add(new JScrollPane(memberList), BorderLayout.CENTER);

        // 创建成员操作按钮面板
        JPanel memberButtonPanel = new JPanel(new GridLayout(2, 1));
        inviteMemberButton = new JButton("邀请成员");
        refreshMembersButton = new JButton("刷新成员");
        
        memberButtonPanel.add(inviteMemberButton);
        memberButtonPanel.add(refreshMembersButton);
        memberListPanel.add(memberButtonPanel, BorderLayout.SOUTH);

        mainPanel.add(memberListPanel, BorderLayout.EAST);
    }

    /**
     * 设置事件处理器
     * 绑定按钮点击和键盘事件
     */
    private void setupEventHandlers() {
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage()); // 回车键发送消息
        sendImageButton.addActionListener(e -> sendImage());
        inviteMemberButton.addActionListener(e -> inviteMember());
        refreshMembersButton.addActionListener(e -> refreshGroupMembers());
    }

    /**
     * 发送群组文本消息
     * 获取输入框内容并发送给群组所有成员
     */
    private void sendMessage() {
        String content = messageField.getText();
        if (!content.trim().isEmpty()) {
            // 创建群组消息对象
            Message message = new Message(MessageType.GROUP_MESSAGE, client.getCurrentUser().getId(), groupId, content);
            client.sendMessage(message);
            
            // 立即显示自己的消息
            displayMessage(message);
            messageField.setText(""); // 清空输入框
        }
    }

    /**
     * 发送群组图片消息
     * 弹出文件选择对话框，选择图片并发送给群组
     */
    private void sendImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择图片");
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // 读取图片文件并转换为Base64编码
                byte[] imageBytes = Files.readAllBytes(selectedFile.toPath());
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                
                // 群聊中直接发送图片数据，不需要确认
                String imageContent = selectedFile.getName() + ":" + base64Image;
                Message message = new Message(MessageType.IMAGE_MESSAGE, client.getCurrentUser().getId(), groupId, imageContent);
                client.sendMessage(message);
                
                // 立即显示自己发送的图片消息
                Message displayMessage = new Message(MessageType.IMAGE_MESSAGE, client.getCurrentUser().getId(), groupId, selectedFile.getName());
                displayMessage(displayMessage);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "发送图片失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 邀请成员加入群组
     * 弹出输入框让用户输入要邀请的成员ID
     */
    private void inviteMember() {
        String invitedId = JOptionPane.showInputDialog(this, "请输入要邀请的成员ID:");
        if (invitedId != null && !invitedId.trim().isEmpty()) {
            // 发送群组邀请消息
            client.sendMessage(new Message(MessageType.GROUP_INVITE, client.getCurrentUser().getId(), invitedId, groupId));
            JOptionPane.showMessageDialog(this, "群成员邀请已发送给 " + invitedId + "。");
        }
    }

    /**
     * 刷新群组成员列表
     * 向服务器请求最新的群组成员信息
     */
    private void refreshGroupMembers() {
        System.out.println("Refreshing group members for group: " + groupId);
        requestGroupMembers();
    }

    /**
     * 显示消息到聊天区域
     * 处理群组文本消息和图片消息的显示
     * @param message 要显示的消息对象
     */
    public void displayMessage(Message message) {
        // 确定发送者显示名称
        String senderName = message.getSenderId().equals(client.getCurrentUser().getId()) ? "我" : message.getSenderId();
        String time = dateFormat.format(new Date(message.getTimestamp()));
        String displayContent;

        if (message.getType() == MessageType.IMAGE_MESSAGE) {
            // 处理群聊图片消息
            String content = message.getContent();
            if (content.contains(":")) {
                // 包含图片数据的消息，提取文件名
                String fileName = content.split(":", 2)[0];
                displayContent = "[图片: " + fileName + "]";
                
                // 如果是接收到的图片消息，自动保存图片
                if (!message.getSenderId().equals(client.getCurrentUser().getId())) {
                    try {
                        String[] parts = content.split(":", 2);
                        if (parts.length == 2) {
                            String base64Image = parts[1];
                            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                            
                            // 创建群组图片保存目录
                            File saveDir = new File("received_images");
                            saveDir.mkdirs();
                            
                            // 保存图片文件
                            File outputFile = new File(saveDir, fileName);
                            Files.write(outputFile.toPath(), imageBytes);
                            
                            displayContent += " (已保存到: " + outputFile.getAbsolutePath() + ")";
                        }
                    } catch (Exception ex) {
                        displayContent += " (保存失败: " + ex.getMessage() + ")";
                    }
                }
            } else {
                // 只有文件名的消息
                displayContent = "[图片: " + content + "]";
            }
        } else {
            // 普通文本消息
            displayContent = message.getContent();
        }
        
        // 将消息添加到聊天区域
        chatArea.append(time + " [" + senderName + "]: " + displayContent + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength()); // 滚动到底部
    }

    /**
     * 加载群聊历史记录
     * 从本地文件读取之前的群聊记录并显示
     */
    private void loadChatHistory() {
        String fileName = "chat_history_group_" + groupId + ".txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                chatArea.append(line + "\n");
            }
        } catch (IOException e) {
            System.err.println("No chat history found for group " + groupId + ": " + e.getMessage());
        }
    }

    /**
     * 请求群组成员列表
     * 向服务器发送获取群组成员的请求
     */
    private void requestGroupMembers() {
        client.sendMessage(new Message(MessageType.GET_GROUP_MEMBERS, client.getCurrentUser().getId(), "Server", groupId));
    }

    /**
     * 更新群组成员列表
     * 根据服务器返回的成员信息更新界面显示
     * @param members 成员列表，每个元素格式为"ID username"
     */
    public void updateGroupMembers(List<String> members) {
        memberListModel.clear();
        for (String member : members) {
            memberListModel.addElement(member);
        }
        // 强制刷新界面
        memberList.revalidate();
        memberList.repaint();
        System.out.println("Updated group members list with " + members.size() + " members");
    }

    /**
     * 设置窗口关闭处理器
     * 窗口关闭时的清理工作
     */
    private void setupWindowCloseHandler() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 窗口关闭时可以进行一些清理工作
                // 目前聊天记录由服务器自动保存，这里暂时不需要额外操作
            }
        });
    }
}