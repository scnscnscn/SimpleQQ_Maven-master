package com.simpleqq.client;

import com.simpleqq.common.Message;
import com.simpleqq.common.MessageType;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class RegisterWindow extends JFrame {
    private JTextField idField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton registerButton;
    private JButton backButton;
    private Client client;
    private LoginWindow loginWindow;

    public RegisterWindow(Client client, LoginWindow loginWindow) {
        this.client = client;
        this.loginWindow = loginWindow;
        setTitle("QQ注册");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null); // 居中显示

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 1));
        add(panel);

        panel.add(new JLabel("ID (唯一标识):"));
        idField = new JTextField(15);
        panel.add(idField);

        panel.add(new JLabel("用户名:"));
        usernameField = new JTextField(15);
        panel.add(usernameField);

        panel.add(new JLabel("密码:"));
        passwordField = new JPasswordField(15);
        panel.add(passwordField);

        panel.add(new JLabel("确认密码:"));
        confirmPasswordField = new JPasswordField(15);
        panel.add(confirmPasswordField);

        registerButton = new JButton("注册");
        panel.add(registerButton);

        backButton = new JButton("返回登录");
        panel.add(backButton);

        registerButton.addActionListener(e -> {
            String id = idField.getText();
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (id.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "所有字段都不能为空！");
                return;
            }
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "两次输入的密码不一致！");
                return;
            }

            try {
                if (client.socket == null || client.socket.isClosed()) {
                    client.connect();
                }
                client.sendMessage(new Message(MessageType.REGISTER, id, "Server", id + "," + username + "," + password));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "连接服务器失败: " + ex.getMessage());
            }
        });

        backButton.addActionListener(e -> {
            // 恢复登录窗口的消息监听器
            loginWindow.setupMessageListener();
            loginWindow.setVisible(true);
            this.dispose();
        });

        // 设置注册窗口的消息监听器
        setupMessageListener();
    }

    private void setupMessageListener() {
        client.setMessageListener(message -> {
            if (message.getType() == MessageType.REGISTER_SUCCESS) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "注册成功！请登录。");
                    // 恢复登录窗口的消息监听器
                    loginWindow.setupMessageListener();
                    loginWindow.setVisible(true);
                    this.dispose();
                });
            } else if (message.getType() == MessageType.REGISTER_FAIL) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "注册失败: " + message.getContent());
                });
            }
        });
    }
}