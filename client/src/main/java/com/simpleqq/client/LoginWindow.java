package com.simpleqq.client;

import com.simpleqq.common.Message;
import com.simpleqq.common.MessageType;
import com.simpleqq.common.User;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LoginWindow extends JFrame {
    private JTextField idField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private Client client;

    public LoginWindow(Client client) {
        this.client = client;
        setTitle("QQ登录");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 居中显示

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1));
        add(panel);

        panel.add(new JLabel("ID:"));
        idField = new JTextField(15);
        panel.add(idField);

        panel.add(new JLabel("密码:"));
        passwordField = new JPasswordField(15);
        panel.add(passwordField);

        loginButton = new JButton("登录");
        panel.add(loginButton);

        registerButton = new JButton("注册");
        panel.add(registerButton);

        loginButton.addActionListener(e -> {
            String id = idField.getText();
            String password = new String(passwordField.getPassword());
            if (id.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "ID和密码不能为空！");
                return;
            }
            try {
                if (client.socket == null || client.socket.isClosed()) {
                    client.connect();
                }
                client.sendMessage(new Message(MessageType.LOGIN, id, "Server", id + "," + password));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "连接服务器失败: " + ex.getMessage());
            }
        });

        registerButton.addActionListener(e -> {
            RegisterWindow registerWindow = new RegisterWindow(client, this);
            registerWindow.setVisible(true);
            this.setVisible(false);
        });

        // 设置初始消息监听器
        setupMessageListener();
    }

    public void setupMessageListener() {
        client.setMessageListener(message -> {
            if (message.getType() == MessageType.LOGIN_SUCCESS) {
                User loggedInUser = new User(message.getReceiverId(), message.getContent(), ""); // content is username
                loggedInUser.setOnline(true);
                client.setCurrentUser(loggedInUser);
                SwingUtilities.invokeLater(() -> {
                    ChatWindow chatWindow = new ChatWindow(client);
                    chatWindow.setVisible(true);
                    this.dispose(); // 关闭登录窗口
                });
            } else if (message.getType() == MessageType.LOGIN_FAIL) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "登录失败: " + message.getContent());
                });
            }
        });
    }

    public static void main(String[] args) {
        Client client = new Client();
        SwingUtilities.invokeLater(() -> {
            new LoginWindow(client).setVisible(true);
        });
    }
}