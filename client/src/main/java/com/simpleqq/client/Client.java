package com.simpleqq.client;

import com.simpleqq.common.Message;
import com.simpleqq.common.MessageType;
import com.simpleqq.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class Client {
    private static final String SERVER_IP = "127.0.0.1"; // 服务器IP地址
    private static final int SERVER_PORT = 8888; // 服务器端口

    public Socket socket; // Changed to public
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private User currentUser;

    private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private Consumer<Message> messageListener;

    public Client() {
    }

    public void connect() throws IOException {
        socket = new Socket(SERVER_IP, SERVER_PORT);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());

        new Thread(() -> {
            try {
                while (true) {
                    Message message = (Message) ois.readObject();
                    System.out.println("Client received: " + message);
                    if (messageListener != null) {
                        messageListener.accept(message);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Server disconnected or error: " + e.getMessage());
                disconnect();
            }
        }).start();
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
            if (ois != null) ois.close();
            if (oos != null) oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message message) {
        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void setMessageListener(Consumer<Message> listener) {
        this.messageListener = listener;
    }

    public static void main(String[] args) {
        // For testing purposes, will be replaced by GUI
        Client client = new Client();
        try {
            client.connect();
            // Example: send a login message
            // client.sendMessage(new Message(MessageType.LOGIN, "testId", "", "testId,password"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


