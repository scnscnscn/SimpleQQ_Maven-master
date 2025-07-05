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

public class GroupChatWindow extends JFrame {
    private Client client;
    private String groupId;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton sendImageButton;
    private JButton inviteMemberButton; // New: Invite Member Button
    private JButton refreshMembersButton; // New: Refresh Members Button
    private JList<String> memberList; // New: Member List
    private DefaultListModel<String> memberListModel; // New: Member List Model
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public GroupChatWindow(Client client, String groupId) {
        this.client = client;
        this.groupId = groupId;

        setTitle("群聊: " + groupId + " - " + client.getCurrentUser().getUsername());
        setSize(700, 500); // Increased size to accommodate member list
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        // Chat Area Panel
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Input Panel
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

        // Member List Panel (Right side)
        JPanel memberListPanel = new JPanel(new BorderLayout());
        memberListPanel.setPreferredSize(new Dimension(150, 0));
        memberListPanel.setBorder(BorderFactory.createTitledBorder("群成员"));

        memberListModel = new DefaultListModel<>();
        memberList = new JList<>(memberListModel);
        memberList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        memberListPanel.add(new JScrollPane(memberList), BorderLayout.CENTER);

        // Button panel for member actions
        JPanel memberButtonPanel = new JPanel(new GridLayout(2, 1));
        inviteMemberButton = new JButton("邀请成员");
        refreshMembersButton = new JButton("刷新成员");
        
        memberButtonPanel.add(inviteMemberButton);
        memberButtonPanel.add(refreshMembersButton);
        memberListPanel.add(memberButtonPanel, BorderLayout.SOUTH);

        mainPanel.add(memberListPanel, BorderLayout.EAST);

        // Action Listeners
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        sendImageButton.addActionListener(e -> sendImage());
        inviteMemberButton.addActionListener(e -> inviteMember());
        refreshMembersButton.addActionListener(e -> refreshGroupMembers());

        loadChatHistory();
        requestGroupMembers(); // Request initial group members

        // Handle window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Optionally save chat history here, though server already saves
            }
        });
    }

    private void sendMessage() {
        String content = messageField.getText();
        if (!content.trim().isEmpty()) {
            Message message = new Message(MessageType.GROUP_MESSAGE, client.getCurrentUser().getId(), groupId, content);
            client.sendMessage(message);
            // 立即显示自己的消息
            displayMessage(message);
            messageField.setText("");
        }
    }

    private void sendImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择图片");
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                byte[] imageBytes = Files.readAllBytes(selectedFile.toPath());
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                // 群聊中直接发送图片数据，不需要确认
                Message message = new Message(MessageType.IMAGE_MESSAGE, client.getCurrentUser().getId(), groupId, selectedFile.getName() + ":" + base64Image);
                client.sendMessage(message);
                // 立即显示自己发送的图片消息
                displayMessage(new Message(MessageType.IMAGE_MESSAGE, client.getCurrentUser().getId(), groupId, selectedFile.getName()));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "发送图片失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void inviteMember() {
        String invitedId = JOptionPane.showInputDialog(this, "请输入要邀请的成员ID:");
        if (invitedId != null && !invitedId.trim().isEmpty()) {
            client.sendMessage(new Message(MessageType.GROUP_INVITE, client.getCurrentUser().getId(), invitedId, groupId));
            JOptionPane.showMessageDialog(this, "群成员邀请已发送给 " + invitedId + "。");
        }
    }

    private void refreshGroupMembers() {
        System.out.println("Refreshing group members for group: " + groupId);
        requestGroupMembers();
    }

    public void displayMessage(Message message) {
        String senderName = message.getSenderId().equals(client.getCurrentUser().getId()) ? "我" : message.getSenderId();
        String time = dateFormat.format(new Date(message.getTimestamp()));
        String displayContent;

        if (message.getType() == MessageType.IMAGE_MESSAGE) {
            // 处理群聊图片消息
            String content = message.getContent();
            if (content.contains(":")) {
                // 如果包含图片数据，只显示文件名
                String fileName = content.split(":", 2)[0];
                displayContent = "[图片: " + fileName + "]";
                
                // 如果是接收到的图片消息，保存图片
                if (!message.getSenderId().equals(client.getCurrentUser().getId())) {
                    try {
                        String[] parts = content.split(":", 2);
                        if (parts.length == 2) {
                            String base64Image = parts[1];
                            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                            
                            // 创建保存目录
                            File saveDir = new File("received_images");
                            saveDir.mkdirs();
                            
                            // 保存图片
                            File outputFile = new File(saveDir, fileName);
                            Files.write(outputFile.toPath(), imageBytes);
                            
                            displayContent += " (已保存到: " + outputFile.getAbsolutePath() + ")";
                        }
                    } catch (Exception ex) {
                        displayContent += " (保存失败: " + ex.getMessage() + ")";
                    }
                }
            } else {
                // 只有文件名
                displayContent = "[图片: " + content + "]";
            }
        } else {
            displayContent = message.getContent();
        }
        chatArea.append(time + " [" + senderName + "]: " + displayContent + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength()); // Scroll to bottom
    }

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

    private void requestGroupMembers() {
        client.sendMessage(new Message(MessageType.GET_GROUP_MEMBERS, client.getCurrentUser().getId(), "Server", groupId));
    }

    public void updateGroupMembers(List<String> members) {
        memberListModel.clear();
        for (String member : members) {
            memberListModel.addElement(member);
        }
        // 强制刷新UI
        memberList.revalidate();
        memberList.repaint();
        System.out.println("Updated group members list with " + members.size() + " members");
    }
}