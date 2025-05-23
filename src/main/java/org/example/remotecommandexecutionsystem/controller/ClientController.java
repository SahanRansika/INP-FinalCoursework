package org.example.remotecommandexecutionsystem.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
    public class ClientController {

        @FXML
        private ListView<Object> messageView;

        @FXML
        private Button btnSend;

        @FXML
        private TextField txtMessage;

        @FXML
        private Button btnImage;

        private ObjectOutputStream objectOutputStream;
        private ObjectInputStream objectInputStream;
        private String clientName;
        private boolean nameAccept = false;


        private void promptForName(){
            Platform.runLater(()->{
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Enter Your Name");
                dialog.setHeaderText("Please Enter Your Name!");

                dialog.showAndWait().ifPresent(name ->{
                    clientName = name.trim();
                    if (clientName.isEmpty()){
                        messageView.getItems().add("Client Name cannot be empty, Please try again");
                        promptForName();
                    }else {
                        try {
                            objectOutputStream.writeObject(clientName);
                            objectOutputStream.flush();
                            dialog.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            messageView.getItems().add("Error Sending Name, Please try again");
                        }
                    }
                });

                if (dialog.getResult().isEmpty()){
                    Platform.exit();
                }
            });
        }

        private void listenForMessages() {
            try {
                while(true){
                    Object message = objectInputStream.readObject();
                    if (message == null) break;
                    if (message instanceof String){
                        String text = (String) message;
                        if (text.startsWith("NAME-SUBMIT")){
                            if (!nameAccept){
                                promptForName();
                            }
                        }else if (text.startsWith("NAME-ACCEPT")){
                            nameAccept = true;
                            Platform.runLater(() -> messageView.getItems().add("Connected As " + clientName));
                        }else if (text.startsWith("TEXT")){
                            Platform.runLater(() ->{
                                if (text.startsWith("TEXT " + clientName + ": ")){
                                    messageView.getItems().add("You: " + text.substring(clientName.length()+2+5));
                                }else {
                                    messageView.getItems().add(text.substring(5));
                                }
                            });
                        } else if (text.startsWith("IMAGE")) {
                            byte[] imageData = (byte[]) objectInputStream.readObject();
                            Image image = new Image(new ByteArrayInputStream(imageData));
                            Platform.runLater(() -> {
                                messageView.getItems().add(text.substring(6) + " sent an image");
                                messageView.getItems().add(image);
                            });
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                Platform.runLater(() -> messageView.getItems().add("Disconnected" + e.getMessage()));
            }finally {
                closeConnection();
            }
        }

        public void closeConnection() {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }

                if (objectInputStream != null) {
                    objectInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @FXML
        void btnSendOnAction(ActionEvent event) {
            String message = txtMessage.getText().trim();
            if (message.isEmpty()) return;
            try {
                objectOutputStream.writeObject(message);
                objectOutputStream.flush();
                txtMessage.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @FXML
        void btnImageOnAction(ActionEvent event) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
            );

            File file = fileChooser.showOpenDialog(null);
            if (file != null) {

                try {
                    byte [] imageData = Files.readAllBytes(file.toPath());
                    objectOutputStream.writeObject(imageData);
                    objectOutputStream.flush();
                } catch (IOException e) {
                    Platform.runLater(() -> messageView.getItems().add("Error to Sending Image, Please try again"));
                }
            }
        }

        public void initialize(){
            messageView.setCellFactory(listView -> new ListCell<Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else if (item instanceof String) {
                        setText((String) item);
                        setGraphic(null);
                    } else if (item instanceof Image) {
                        Image image = (Image) item;
                        ImageView imageView = new ImageView(image);
                        imageView.setFitHeight(100);
                        imageView.setFitWidth(100);
                        imageView.setPreserveRatio(true);
                        imageView.setSmooth(true);
                        imageView.setCache(true);

                        setGraphic(imageView);
                        setText(null);
                    }
                }
            });


            try {
                Socket socket = new Socket("localhost", 5000);
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectInputStream = new ObjectInputStream(socket.getInputStream());

                Thread thread = new Thread(() -> listenForMessages());
                thread.start();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

