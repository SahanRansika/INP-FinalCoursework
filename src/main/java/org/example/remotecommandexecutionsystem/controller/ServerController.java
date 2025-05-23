package org.example.remotecommandexecutionsystem.controller;

import org.example.remotecommandexecutionsystem.controller.ClientController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

public class ServerController {
    private static final int PORT = 5000;
    private boolean isRunning = false;
    private ServerSocket serverSocket;
    private static HashSet<ObjectOutputStream> writer = new HashSet<>();

    @FXML
    private TextArea txtServerStatus;

    @FXML
    public void initialize(){
        appendStatus("Server is initialized, click 'Add client' to start");
    }

    void appendStatus(String message) {
        Platform.runLater(() -> txtServerStatus.appendText(message + "\n"));
    }

    public void btnAddClientOnAction(ActionEvent actionEvent) {
        if (!isRunning){
            startServer();
        }

        openClientWindow();
    }


    private void startServer() {
        isRunning = true;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                appendStatus("Server started on port " + PORT);
                while(isRunning){
                    Socket clientSocket = serverSocket.accept();
                    appendStatus("New Client Connected");

                    Thread clientThread = new Thread(new ClientHandler(clientSocket));
                    clientThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (isRunning){
                    appendStatus("Error starting server: " + e.getMessage());
                }
            }
        }).start();

    }

    private void openClientWindow() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/remotecommandexecutionsystem/client.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = new Stage();
                stage.setTitle("Client");
                stage.setScene(scene);
                ;               stage.setOnCloseRequest(event -> {
                    ClientController clientController = loader.getController();
                    clientController.closeConnection();
                });
                stage.show();
                appendStatus("New client window opened");
            } catch (IOException e) {
                appendStatus("Error opening to client window!");
                e.printStackTrace();
            }
        });
    }

    private class ClientHandler implements Runnable{
        private Socket socket;
        private ObjectInputStream objectInputStream;
        private ObjectOutputStream objectOutputStream;
        private String clientName;

        ClientHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectInputStream = new ObjectInputStream(socket.getInputStream());

                while(true){
                    objectOutputStream.writeObject("NAME-SUBMIT");
                    clientName = (String) objectInputStream.readObject();
                    if (!clientName.trim().isEmpty() && clientName != null){
                        break;
                    }
                    appendStatus("Invalid name, requesting again");
                }

                objectOutputStream.writeObject("NAME-ACCEPT");
                appendStatus("Client " + clientName + " connected");
                broadcast("TEXT " + clientName + " joined the chat");

                synchronized (writer){
                    writer.add(objectOutputStream);
                }

                while(true){
                    Object message = objectInputStream.readObject();
                    if (message == null) break;
                    if (message instanceof String){
                        String text = (String) message;
                        broadcast("TEXT " + clientName + ": " +text);
                    } else if (message instanceof byte []){
                        broadcast("IMAGE " + clientName, (byte []) message);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                appendStatus("Error connecting to client");
            }finally {
                if (clientName != null){
                    appendStatus("Client " + clientName + " disconnected");
                    broadcast("TEXT " + clientName + " left the chat");
                }

                synchronized (writer){
                    writer.remove(objectOutputStream);
                }

                try {
                    socket.close();
                } catch (IOException e) {
                    appendStatus("Error closing to client socket!");
                    e.printStackTrace();
                }
            }
        }

        private void broadcast ( String message){
            synchronized (writer){
                for (ObjectOutputStream writer : writer){
                    try {
                        writer.writeObject(message);
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        appendStatus("Error broadcasting to message!");
                    }
                }
            }
        }

        private void broadcast(String header,byte [] imageData) {
            synchronized (writer){
                for (ObjectOutputStream writer : writer){
                    try {
                        writer.writeObject(header);
                        writer.writeObject(imageData);
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        appendStatus("Error broadcasting to image!");
                    }
                }
            }
        }
    }
}
