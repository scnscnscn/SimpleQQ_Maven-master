# SimpleQQ 即时通讯系统

一个基于 Java Swing 和 Socket 的即时通讯应用，支持用户注册登录、好友管理、私聊、群聊和图片传输等功能。

## 项目结构

```
SimpleQQ/
├── common/                 # 公共模块
│   └── src/main/java/com/simpleqq/common/
│       ├── Message.java    # 消息实体类
│       ├── MessageType.java # 消息类型枚举
│       └── User.java       # 用户实体类
├── server/                 # 服务器端
│   └── src/main/java/com/simpleqq/server/
│       ├── Server.java     # 服务器主类
│       ├── ClientHandler.java # 客户端处理器
│       ├── UserManager.java   # 用户管理器
│       └── GroupManager.java  # 群组管理器
├── client/                 # 客户端
│   └── src/main/java/com/simpleqq/client/
│       ├── LoginWindow.java    # 登录窗口
│       ├── RegisterWindow.java # 注册窗口
│       ├── ChatWindow.java     # 主聊天窗口
│       ├── SingleChatWindow.java # 私聊窗口
│       ├── GroupChatWindow.java  # 群聊窗口
│       └── Client.java         # 客户端核心类
└── pom.xml                 # Maven 父项目配置
```

## 功能特性

### 用户管理
- 用户注册：支持用户ID、用户名、密码注册
- 用户登录：验证用户身份，防止重复登录
- 在线状态：实时显示好友在线/离线状态

### 好友系统
- 好友请求：发送好友申请
- 好友管理：接受/拒绝好友请求
- 好友删除：删除已添加的好友
- 好友列表：显示所有好友及其在线状态

### 聊天功能
- 私聊：与好友进行一对一聊天
- 群聊：创建群组，邀请成员进行群聊
- 图片传输：支持发送和接收图片文件
- 聊天记录：自动保存聊天历史，支持手动导出

### 群组管理
- 创建群组：用户可创建新的聊天群组
- 群组邀请：邀请其他用户加入群组
- 成员管理：查看群组成员列表及在线状态
- 群组消息：群内消息实时同步

## 技术架构

### 后端技术
- **Java Socket**：网络通信基础
- **多线程**：每个客户端连接独立线程处理
- **文件存储**：用户数据、好友关系、群组信息持久化
- **消息序列化**：使用 Java 对象序列化传输消息

### 前端技术
- **Java Swing**：图形用户界面
- **事件驱动**：基于监听器的用户交互
- **多窗口管理**：主窗口、聊天窗口分离设计

### 数据存储
- `users.txt`：用户基本信息
- `friendships.txt`：好友关系数据
- `friend_requests.txt`：待处理好友请求
- `groups.txt`：群组信息和成员
- `group_invites.txt`：待处理群组邀请
- `chat_history_*.txt`：聊天记录文件

## 快速开始

### 环境要求
- Java 17 或更高版本
- Maven 3.6 或更高版本

### 编译项目
```bash
# 克隆项目
git clone <repository-url>
cd SimpleQQ

# 编译所有模块
mvn clean compile

# 打包项目
mvn clean package
```

### 启动服务器
```bash
# 方式1：直接运行
cd server/target/classes
java com.simpleqq.server.Server

# 方式2：使用 Maven
cd server
mvn exec:java -Dexec.mainClass="com.simpleqq.server.Server"
```

### 启动客户端
```bash
# 方式1：直接运行
cd client/target/classes
java com.simpleqq.client.LoginWindow

# 方式2：使用 Maven
cd client
mvn exec:java -Dexec.mainClass="com.simpleqq.client.LoginWindow"
```

## 使用说明

### 用户注册和登录
1. 启动客户端后显示登录界面
2. 首次使用点击"注册"按钮创建账户
3. 输入唯一的用户ID、用户名和密码
4. 注册成功后返回登录界面
5. 使用注册的ID和密码登录系统

### 好友管理
1. 登录后在主界面点击"添加好友"
2. 输入要添加的好友ID
3. 对方会收到好友请求通知
4. 在"请求"标签页处理收到的好友请求
5. 接受后双方成为好友，可以开始聊天

### 私聊功能
1. 在好友列表中点击好友头像
2. 打开私聊窗口
3. 输入文字消息或点击"发送图片"
4. 支持实时消息传输和图片分享
5. 可导出聊天记录到本地文件

### 群聊功能
1. 点击"创建群聊"输入群组ID
2. 创建成功后在群聊列表显示
3. 点击群组进入群聊窗口
4. 使用"邀请成员"功能添加好友到群组
5. 群内消息对所有成员可见

### 图片传输
1. 在聊天窗口点击"发送图片"
2. 选择要发送的图片文件
3. 图片自动转换为Base64编码传输
4. 接收方自动保存图片到本地目录
5. 聊天记录中显示图片文件名

## 消息协议

系统使用自定义消息协议，主要消息类型包括：

### 用户认证
- `LOGIN`：用户登录请求
- `REGISTER`：用户注册请求
- `LOGIN_SUCCESS/FAIL`：登录结果
- `REGISTER_SUCCESS/FAIL`：注册结果

### 好友管理
- `FRIEND_REQUEST`：好友请求
- `FRIEND_ACCEPT/REJECT`：处理好友请求
- `DELETE_FRIEND`：删除好友
- `FRIEND_LIST`：获取好友列表

### 消息传输
- `TEXT_MESSAGE`：文本消息
- `IMAGE_MESSAGE`：图片消息
- `GROUP_MESSAGE`：群组消息

### 群组管理
- `CREATE_GROUP`：创建群组
- `GROUP_INVITE`：群组邀请
- `GROUP_ACCEPT/REJECT`：处理群组邀请
- `GET_GROUPS`：获取群组列表
- `GET_GROUP_MEMBERS`：获取群组成员

## 配置说明

### 服务器配置
- 默认端口：8888
- 最大并发连接：无限制
- 数据存储：文本文件格式

### 客户端配置
- 服务器地址：127.0.0.1（本地）
- 连接端口：8888
- 图片保存路径：received_images_from_[发送者ID]

## 注意事项

1. **网络环境**：确保服务器和客户端网络连通
2. **文件权限**：确保程序有读写文件的权限
3. **端口占用**：确保8888端口未被其他程序占用
4. **数据备份**：重要聊天记录建议定期备份
5. **图片大小**：建议图片文件不超过10MB

## 故障排除

### 连接问题
- 检查服务器是否正常启动
- 确认网络连接和端口配置
- 查看防火墙设置

### 登录失败
- 确认用户ID和密码正确
- 检查是否已在其他地方登录
- 验证服务器用户数据文件

### 消息发送失败
- 确认好友关系是否建立
- 检查网络连接状态
- 验证接收方是否在线

## 开发扩展

### 添加新功能
1. 在 `MessageType` 枚举中添加新消息类型
2. 在 `ClientHandler` 中添加对应处理逻辑
3. 在客户端界面添加相应操作入口
4. 更新消息协议文档

### 性能优化
- 实现消息队列机制
- 添加数据库支持
- 优化图片传输算法
- 实现负载均衡

### 安全增强
- 添加消息加密
- 实现用户权限管理
- 添加防刷机制
- 完善日志记录

## 许可证

本项目采用 MIT 许可证，详见 LICENSE 文件。

## 贡献指南

欢迎提交 Issue 和 Pull Request 来改进项目。请确保：
1. 代码风格一致
2. 添加必要的测试
3. 更新相关文档
4. 遵循项目规范