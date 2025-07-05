package com.simpleqq.client;

import com.simpleqq.common.Message;
import com.simpleqq.common.MessageType;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * 注册窗口类
 * 提供用户注册功能界面
 * 处理新用户账户创建流程
 */
public class RegisterWindow extends JFrame {
    private JTextField idField;                    // 用户ID输入框
    private JTextField usernameField;              // 用户名输入框
    private JPasswordField passwordField;          // 密码输入框
    private JPasswordField confirmPasswordField;   // 确认密码输入框
    private JButton registerButton;                // 注册按钮
    private JButton backButton;                    // 返回登录按钮
    private Client client;                         // 客户端连接对象
    private LoginWindow loginWindow;               // 父登录窗口引用

    /**
     * 构造函数
     * @param client 客户端对象
     * @param loginWindow 父登录窗口，用于返回
     */
    public RegisterWindow(Client client, LoginWindow loginWindow) {
        this.client = client;
        this.loginWindow = loginWindow;
        initializeUI();
        setupEventHandlers();
        setupMessageListener();
    }

    /**
     * 初始化用户界面
     * 设置窗口布局和输入组件
     */
    private void initializeUI() {
        setTitle("QQ注册");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null); // 窗口居中显示

        // 创建主面板，使用网格布局
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 1));
        add(panel);

        // 添加用户ID输入组件
        panel.add(new JLabel("ID (唯一标识):"));
        idField = new JTextField(15);
        panel.add(idField);

        // 添加用户名输入组件
        panel.add(new JLabel("用户名:"));
        usernameField = new JTextField(15);
        panel.add(usernameField);

        // 添加密码输入组件
        panel.add(new JLabel("密码:"));
        passwordField = new JPasswordField(15);
        panel.add(passwordField);

        // 添加确认密码输入组件
        panel.add(new JLabel("确认密码:"));
        confirmPasswordField = new JPasswordField(15);
        panel.add(confirmPasswordField);

        // 添加注册和返回按钮
        registerButton = new JButton("注册");
        panel.add(registerButton);

        backButton = new JButton("返回登录");
        panel.add(backButton);
    }

    /**
     * 设置事件处理器
     * 绑定按钮点击事件
     */
    private void setupEventHandlers() {
        // 注册按钮点击事件
        registerButton.addActionListener(e -> handleRegister());

        // 返回登录按钮点击事件
        backButton.addActionListener(e -> {
            loginWindow.setupMessageListener(); // 恢复登录窗口的消息监听器
            loginWindow.setVisible(true);
            this.dispose(); // 关闭注册窗口
        });
    }

    /**
     * 处理用户注册逻辑
     * 验证输入数据并发送注册请求
     */
    private void handleRegister() {
        String id = idField.getText();
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        // 验证输入完整性
        if (id.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "所有字段都不能为空！");
            return;
        }
        
        // 验证密码一致性
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "两次输入的密码不一致！");
            return;
        }

        try {
            // 确保与服务器连接
            if (client.socket == null || client.socket.isClosed()) {
                client.connect();
            }
            
            // 发送注册请求，格式：ID,用户名,密码
            String registerData = id + "," + username + "," + password;
            client.sendMessage(new Message(MessageType.REGISTER, id, "Server", registerData));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "连接服务器失败: " + ex.getMessage());
        }
    }

    /**
     * 设置消息监听器
     * 处理服务器返回的注册结果
     */
    private void setupMessageListener() {
        client.setMessageListener(message -> {
            if (message.getType() == MessageType.REGISTER_SUCCESS) {
                // 注册成功，返回登录窗口
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "注册成功！请登录。");
                    loginWindow.setupMessageListener(); // 恢复登录窗口的消息监听器
                    loginWindow.setVisible(true);
                    this.dispose(); // 关闭注册窗口
                });
            } else if (message.getType() == MessageType.REGISTER_FAIL) {
                // 注册失败，显示错误信息
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "注册失败: " + message.getContent());
                });
            }
        });
    }
}