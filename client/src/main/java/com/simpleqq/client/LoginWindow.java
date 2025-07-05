package com.simpleqq.client;

import com.simpleqq.common.Message;
import com.simpleqq.common.MessageType;
import com.simpleqq.common.User;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * 登录窗口类
 * 提供用户登录界面和注册入口
 * 处理用户认证相关的界面交互
 */
public class LoginWindow extends JFrame {
    private JTextField idField;           // 用户ID输入框
    private JPasswordField passwordField; // 密码输入框
    private JButton loginButton;          // 登录按钮
    private JButton registerButton;       // 注册按钮
    private Client client;                // 客户端连接对象

    /**
     * 构造函数
     * @param client 客户端对象，用于网络通信
     */
    public LoginWindow(Client client) {
        this.client = client;
        initializeUI();
        setupEventHandlers();
        setupMessageListener();
    }

    /**
     * 初始化用户界面
     * 设置窗口布局和组件
     */
    private void initializeUI() {
        setTitle("QQ登录");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 窗口居中显示

        // 创建主面板，使用网格布局
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1));
        add(panel);

        // 添加用户ID输入组件
        panel.add(new JLabel("ID:"));
        idField = new JTextField(15);
        panel.add(idField);

        // 添加密码输入组件
        panel.add(new JLabel("密码:"));
        passwordField = new JPasswordField(15);
        panel.add(passwordField);

        // 添加登录和注册按钮
        loginButton = new JButton("登录");
        panel.add(loginButton);

        registerButton = new JButton("注册");
        panel.add(registerButton);
    }

    /**
     * 设置事件处理器
     * 绑定按钮点击事件
     */
    private void setupEventHandlers() {
        // 登录按钮点击事件
        loginButton.addActionListener(e -> handleLogin());

        // 注册按钮点击事件
        registerButton.addActionListener(e -> {
            RegisterWindow registerWindow = new RegisterWindow(client, this);
            registerWindow.setVisible(true);
            this.setVisible(false); // 隐藏登录窗口
        });
    }

    /**
     * 处理用户登录逻辑
     * 验证输入并发送登录请求
     */
    private void handleLogin() {
        String id = idField.getText();
        String password = new String(passwordField.getPassword());
        
        // 验证输入是否为空
        if (id.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ID和密码不能为空！");
            return;
        }
        
        try {
            // 确保与服务器连接
            if (client.socket == null || client.socket.isClosed()) {
                client.connect();
            }
            
            // 发送登录请求，格式：ID,密码
            String loginData = id + "," + password;
            client.sendMessage(new Message(MessageType.LOGIN, id, "Server", loginData));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "连接服务器失败: " + ex.getMessage());
        }
    }

    /**
     * 设置消息监听器
     * 处理服务器返回的登录结果
     */
    public void setupMessageListener() {
        client.setMessageListener(message -> {
            if (message.getType() == MessageType.LOGIN_SUCCESS) {
                // 登录成功，创建用户对象并打开主窗口
                User loggedInUser = new User(message.getReceiverId(), message.getContent(), "");
                loggedInUser.setOnline(true);
                client.setCurrentUser(loggedInUser);
                
                SwingUtilities.invokeLater(() -> {
                    ChatWindow chatWindow = new ChatWindow(client);
                    chatWindow.setVisible(true);
                    this.dispose(); // 关闭登录窗口
                });
            } else if (message.getType() == MessageType.LOGIN_FAIL) {
                // 登录失败，显示错误信息
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "登录失败: " + message.getContent());
                });
            }
        });
    }

    /**
     * 主方法，程序入口点
     */
    public static void main(String[] args) {
        Client client = new Client();
        SwingUtilities.invokeLater(() -> {
            new LoginWindow(client).setVisible(true);
        });
    }
}