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
    private JButton saveHistoryButton;
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
        saveHistoryButton = new JButton("保存聊天记录");

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
        buttonPanel.add(sendButton);
        buttonPanel.add(sendImageButton);
        buttonPanel.add(saveHistoryButton);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        sendImageButton.addActionListener(e -> sendImage());
        saveHistoryButton.addActionListener(e -> saveChatHistory());

        loadChatHistory();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
            }
        });
    }

    private void sendMessage() {
        String content = messageField.getText();
        if (!content.trim().isEmpty()) {
            Message message = new Message(MessageType.TEXT_MESSAGE, client.getCurrentUser().getId(), friendId, content);
            client.sendMessage(message);
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
                Message message = new Message(MessageType.IMAGE_MESSAGE, client.getCurrentUser().getId(), friendId, selectedFile.getName() + ":" + base64Image);
                client.sendMessage(message);
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
            String content = message.getContent();
            if (content.contains(":")) {
                String fileName = content.split(":", 2)[0];
                displayContent = "[图片: " + fileName + "]";
                
                if (!message.getSenderId().equals(client.getCurrentUser().getId())) {
                    try {
                        String[] parts = content.split(":", 2);
                        if (parts.length == 2) {
                            String base64Image = parts[1];
                            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                            
                            File saveDir = new File("received_images_from_" + message.getSenderId());
                            saveDir.mkdirs();
                            
                            File outputFile = new File(saveDir, fileName);
                            Files.write(outputFile.toPath(), imageBytes);
                            
                            displayContent += " (已保存到: " + outputFile.getAbsolutePath() + ")";
                        }
                    } catch (Exception ex) {
                        displayContent += " (保存失败: " + ex.getMessage() + ")";
                    }
                }
            } else {
                displayContent = "[图片: " + content + "]";
            }
        } else {
            displayContent = message.getContent();
        }
        chatArea.append(time + " [" + senderName + "]: " + displayContent + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
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
}