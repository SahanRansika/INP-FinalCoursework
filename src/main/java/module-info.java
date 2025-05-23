module org.example.remotecommandexecutionsystem {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.remotecommandexecutionsystem.controller to javafx.fxml;
    exports org.example.remotecommandexecutionsystem;
}