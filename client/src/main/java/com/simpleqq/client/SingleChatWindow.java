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

public class SingleChatWindow extends JFrame {
    private Client client;
    private String friendId;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton sendImageButton;
    private JButton saveHistoryButton; // New: Save chat history button
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public SingleChatWindow(Client client, String friendId) {
        this.client = client;
        this.friendId = friendId;

        setTitle("与 " + friendId + " 聊天 - " + client.getCurrentUser().getUsername());
        setSize(500, 400);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());
        add(panel);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("发送");
        sendImageButton = new JButton("发送图片");
        saveHistoryButton = new JButton("保存聊天记录"); // Initialize the new button

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3)); // Changed to 1,3 for new button
        buttonPanel.add(sendButton);
        buttonPanel.add(sendImageButton);
        buttonPanel.add(saveHistoryButton); // Add the new button

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        sendImageButton.addActionListener(e -> sendImage());
        saveHistoryButton.addActionListener(e -> saveChatHistory()); // Add action listener for save button

        loadChatHistory();

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
            Message message = new Message(MessageType.TEXT_MESSAGE, client.getCurrentUser().getId(), friendId, content);
            client.sendMessage(message);
            displayMessage(message); // Display own message immediately
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
                // 私聊中直接发送图片数据，类似群聊
                Message message = new Message(MessageType.IMAGE_MESSAGE, client.getCurrentUser().getId(), friendId, selectedFile.getName() + ":" + base64Image);
                client.sendMessage(message);
                // 立即显示自己发送的图片消息
                displayMessage(new Message(MessageType.IMAGE_MESSAGE, client.getCurrentUser().getId(), friendId, selectedFile.getName()));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "发送图片失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void displayMessage(Message message) {
        String senderName = message.getSenderId().equals(client.getCurrentUser().getId()) ? "我" : message.getSenderId();
        String time = dateFormat.format(new Date(message.getTimestamp()));
        String displayContent;

        if (message.getType() == MessageType.IMAGE_MESSAGE) {
            // 处理私聊图片消息
            String content = message.getContent();
            if (content.contains(":")) {
                // 如果包含图片数据，只显示文件名
                String fileName = content.split(":", 2)[0];
                displayContent = "[图片: " + fileName + "]";
                
                // 如果是接收到的图片消息，保存图片到以发送者用户名命名的文件夹
                if (!message.getSenderId().equals(client.getCurrentUser().getId())) {
                    try {
                        String[] parts = content.split(":", 2);
                        if (parts.length == 2) {
                            String base64Image = parts[1];
                            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                            
                            // 创建以发送者用户名命名的保存目录
                            File saveDir = new File("received_images_from_" + message.getSenderId());
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
        String currentUserId = client.getCurrentUser().getId();
        String fileName;
        if (currentUserId.compareTo(friendId) < 0) {
            fileName = "chat_history_" + currentUserId + "_" + friendId + ".txt";
        } else {
            fileName = "chat_history_" + friendId + "_" + currentUserId + ".txt";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                chatArea.append(line + "\n");
            }
        } catch (IOException e) {
            System.err.println("No chat history found for " + friendId + ": " + e.getMessage());
        }
    }

    private void saveChatHistory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存聊天记录");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File("chat_history_" + client.getCurrentUser().getId() + "_" + friendId + ".txt"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                Files.write(fileToSave.toPath(), chatArea.getText().getBytes());
                JOptionPane.showMessageDialog(this, "聊天记录已保存到: " + fileToSave.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "保存聊天记录失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // New method to handle incoming image related messages
    public void handleImageMessage(Message message) {
        switch (message.getType()) {
            case IMAGE_REQUEST:
                String senderId = message.getSenderId();
                String fileName = message.getContent();
                int choice = JOptionPane.showConfirmDialog(this, senderId + " 想向您发送图片 " + fileName + "，是否接受？", "接收图片", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("选择图片保存位置");
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int result = fileChooser.showSaveDialog(this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File saveDirectory = fileChooser.getSelectedFile();
                        String savePath = saveDirectory.getAbsolutePath() + File.separator + fileName;
                        client.sendMessage(new Message(MessageType.IMAGE_ACCEPT, client.getCurrentUser().getId(), senderId, savePath));
                    } else {
                        client.sendMessage(new Message(MessageType.IMAGE_REJECT, client.getCurrentUser().getId(), senderId, "用户取消了保存。"));
                    }
                } else {
                    client.sendMessage(new Message(MessageType.IMAGE_REJECT, client.getCurrentUser().getId(), senderId, "用户拒绝接收图片。"));
                }
                break;
            case IMAGE_ACCEPT:
                // 这些方法现在不再使用，因为我们采用了类似群聊的直接发送方式
                break;
            case IMAGE_REJECT:
                // 这些方法现在不再使用，因为我们采用了类似群聊的直接发送方式
                break;
            case IMAGE_DATA:
                // 这些方法现在不再使用，因为我们采用了类似群聊的直接发送方式
                break;
        }
    }
}