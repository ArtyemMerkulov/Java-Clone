package com.geekbrains.cloud.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoaderMainController = new FXMLLoader(getClass().getResource("/client.fxml"));
        Parent MainRoot = fxmlLoaderMainController.load();
        Controller mainController = fxmlLoaderMainController.getController();

        primaryStage.setScene(new Scene(MainRoot, 550, 500));
        primaryStage.setTitle("Geek Cloud Client");
        primaryStage.setOnCloseRequest(event -> mainController.exitAction());

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}