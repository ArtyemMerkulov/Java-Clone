package com.geekbrains.cloud.client;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private ClientNetwork clientNetwork;

    private ClientCloud clientCloud;

    private int currLvl = 0;

    @FXML
    ScrollPane remoteTreeScroll;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clientNetwork = new ClientNetwork();
    }

    public void exitAction() {
        clientNetwork.close();
        Platform.exit();
    }

    public void getFolderTreeStructure() {
        clientNetwork.getFolderTreeStructure();

        do {
            clientCloud = clientNetwork.getClientCloud();
        } while (clientCloud == null);

        drawTreeStructure("root", currLvl);
    }

    @FXML
    private void drawTreeStructure(String name, int lvl) {
        VBox container = new VBox();
        ObservableList<Node> containerChildren = container.getChildren();

        containerChildren.clear();

        FileDescription root;
        if (name.equals("root"))
            root = new FileDescription();
        else
            root = clientCloud.geTreeStructure().getFolder(name, lvl);

        Pane rootPane = new Pane();
        ObservableList<Node> rootPaneChildren = rootPane.getChildren();

        ImageView rootImageView = new ImageView(getImageUrlForFile(root));
        Label rootLabel = new Label(root.getName());

        rootLabel.setLayoutX(15);

        rootPaneChildren.add(rootImageView);
        rootPaneChildren.add(rootLabel);

        containerChildren.add(rootPane);

        List<FileDescription> filesList = clientCloud.geTreeStructure().getFilesList(name, lvl);
        currLvl = lvl;

        for (FileDescription fd : filesList) {
            Pane filePane = new Pane();
            ObservableList<Node> filePaneChildren = filePane.getChildren();

            filePane.setLayoutX(50);

            ImageView imageView = new ImageView(getImageUrlForFile(fd));
            Label label = new Label(fd.getName());

            imageView.setLayoutX(15);

            label.setLayoutX(30);
            label.setOnMouseClicked(event -> drawTreeStructure(label.getText(), currLvl + 1));

            filePaneChildren.add(imageView);
            filePaneChildren.add(label);

            containerChildren.add(filePane);
        }

        remoteTreeScroll.setContent(container);
        remoteTreeScroll.setPrefViewportHeight(0);
        remoteTreeScroll.setPrefHeight(0);
        remoteTreeScroll.setPannable(true);
    }

    private String getImageUrlForFile(FileDescription fd) {
        return fd.isFile() ? "img/file_icon.png" : "img/folder_icon.png";
    }
}