package com.geekbrains.cloud.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/client.fxml"));
        Parent root = fxmlLoader.load();
        Controller controller = fxmlLoader.getController();

        primaryStage.setScene(new Scene(root, 550, 500));
        primaryStage.setTitle("Geek Cloud Client");
        primaryStage.setOnCloseRequest(event -> controller.exitAction());
        primaryStage.setOnShowing(event -> {
            controller.getDefaultRemoteDirectory();
            controller.getDefaultLocalDirectory();
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}