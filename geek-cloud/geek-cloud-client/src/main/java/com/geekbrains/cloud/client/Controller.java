package com.geekbrains.cloud.client;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private ClientNetwork clientNetwork;

    private ClientCloud clientCloud;

    private int nClicked = 0;

    private FileDescription selectedObject;

    private FileDescription currentLocalDirectory;

    private FileDescription currentRemoteDirectory;

    private Label selectedLabel;

    private int currLocalLvl = 0;

    private int currRemoteLvl = 0;

    @FXML
    private ScrollPane remoteTreeScroll;

    @FXML
    private ScrollPane localTreeScroll;

    @FXML
    private Button download;

    @FXML
    private Button upload;

    @Override
    public synchronized void initialize(URL location, ResourceBundle resources) {
        clientNetwork = new ClientNetwork();
        do {
            clientCloud = clientNetwork.getClientCloud();
        } while (clientCloud == null);

        while (!clientCloud.getStart()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void exitAction() {
        clientNetwork.close();
        Platform.exit();
    }

    @FXML
    public void getRemoteFolderTreeStructure() {
        drawTreeStructure(clientCloud.getRemoteTreeStructure(), remoteTreeScroll, "root", 0);
    }

    @FXML
    public void getLocalFolderTreeStructure() {
        drawTreeStructure(clientCloud.getLocalTreeStructure(), localTreeScroll, "root", 0);
    }

    @FXML
    private void drawTreeStructure(FilesTree filesTree, ScrollPane scrollPane, String name, int lvl) {
        FileDescription root;
        if (name.equals("root")) root = new FileDescription();
        else root = filesTree.getFolder(name, lvl);

        if (root == null) return;

        VBox container = new VBox();
        ObservableList<Node> containerChildren = container.getChildren();

        containerChildren.clear();

        containerChildren.add(makeFileInTree(filesTree, scrollPane, root,
                lvl, -1, 0, 0, 15));

        List<FileDescription> filesList = filesTree.getFilesList(name, lvl);

        for (FileDescription fd : filesList)
            containerChildren.add(makeFileInTree(filesTree, scrollPane, fd,
                    lvl,1, 50, 15, 30));

        scrollPane.setContent(container);
        scrollPane.setPrefViewportHeight(0);
        scrollPane.setPrefHeight(0);
        scrollPane.setPannable(true);
    }

    private Pane makeFileInTree(FilesTree filesTree, ScrollPane scrollPane, FileDescription fd,
                                int lvl, int stepDirection, int paneLX, int imgLX, int labelLX) {
        Pane filePane = new Pane();
        ObservableList<Node> filePaneChildren = filePane.getChildren();

        filePane.setLayoutX(paneLX);

        ImageView imageView = new ImageView(getImageUrlForFile(fd));

        imageView.setLayoutX(imgLX);

        Label label = new Label(fd.getName());

        label.setLayoutX(labelLX);

        if (!fd.getName().equals("root")) {
            label.setOnMouseClicked(event -> {
                nClicked += 1;

                if (nClicked == 2 && fd.getType() == Type.FOLDER && fd.getName().equals(selectedObject.getName())) {
                    nClicked = 0;

                    if (scrollPane.equals(localTreeScroll) && label.getLayoutX() == 15) {
                        currLocalLvl -= 1;
                    }
                    else if (scrollPane.equals(remoteTreeScroll) && label.getLayoutX() == 15) {
                        currRemoteLvl -= 1;
                    }
                    else if (scrollPane.equals(localTreeScroll) && label.getLayoutX() == 30) {
                        currLocalLvl += 1;
                        currentLocalDirectory = new FileDescription(selectedObject);
                    }
                    else if (scrollPane.equals(remoteTreeScroll) && label.getLayoutX() == 30) {
                        currRemoteLvl += 1;
                        currentRemoteDirectory = new FileDescription(selectedObject);
                    }

                    selectedObject = null;

                    drawTreeStructure(filesTree, scrollPane,
                            stepDirection == -1 ? filesTree.getElement(lvl - 1, fd.getLink()).getName() : fd.getName(),
                            lvl + stepDirection);
                } else if (nClicked == 2 && selectedObject != null &&
                        (fd.getType() == Type.FILE || !fd.getName().equals(selectedObject.getName()))) {
                    selectedLabel.setBackground(null);
                    selectedObject = null;
                    selectedLabel = null;
                }

                if (nClicked > 2) nClicked = 1;

                if (nClicked == 1) {
                    label.setBackground(new Background(new BackgroundFill(Color.CORNFLOWERBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
                    selectedLabel = label;
                    selectedObject = fd;
                }

            });
        }

        filePaneChildren.add(imageView);
        filePaneChildren.add(label);

        return filePane;
    }

    @FXML
    private void downloadFile() {
        if (selectedObject != null && selectedLabel != null) {
            Path actionFilePath = ClientCloud.getLocalCloudPath()
                    .resolve(clientCloud.getLocalTreeStructure()
                            .getPath(currentLocalDirectory, currLocalLvl - 1)
                            .resolve(Paths.get(selectedObject.getName())));

            clientCloud.setActionFilePath(actionFilePath);
            clientNetwork.requestDownloadFile(selectedObject, currRemoteLvl);
        }

        while (true) {
            if (clientCloud.isFileReceived()) {
                drawTreeStructure(clientCloud.getLocalTreeStructure(), localTreeScroll,
                        selectedObject.getName(), currLocalLvl);
                clientCloud.setFileReceived(false);
                break;
            }
        }

        clientCloud.setActionFilePath(null);
    }

    @FXML
    private void uploadFile() {
        if (selectedObject != null && selectedLabel != null)
            clientNetwork.requestUploadFile(selectedObject, currLocalLvl);
    }

    private String getImageUrlForFile(FileDescription fd) {
        return fd.isFile() ? "img/file_icon.png" : "img/folder_icon.png";
    }
}