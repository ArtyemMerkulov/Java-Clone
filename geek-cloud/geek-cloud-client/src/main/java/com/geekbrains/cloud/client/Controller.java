package com.geekbrains.cloud.client;

import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Type;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.apache.commons.codec.digest.DigestUtils;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private static final int ROOT_FILE_PANE_LAYOUT_X = 0;

    private static final int ROOT_FILE_IMG_LAYOUT_X = 0;

    private static final int ROOT_FILE_LABEL_LAYOUT_X = 20;

    private static final int CHILD_FILE_PANE_LAYOUT_X = 20;

    private static final int CHILD_FILE_IMG_LAYOUT_X = 20;

    private static final int CHILD_FILE_LABEL_LAYOUT_X = 40;

    private final ClientNetwork clientNetwork = new ClientNetwork();

    private final ClientCloud clientCloud = clientNetwork.getClientCloud();

    private int nClicked = 0;

    private FileDescription selectedObject;

    private FileDescription currentLocalRoot;

    private FileDescription currentRemoteRoot;

    private List<FileDescription> currentLocalRootFiles;

    private List<FileDescription> currentRemoteRootFiles;

    private Label selectedLabel;

    @FXML
    public VBox stage;

    @FXML
    public Pane authPane;

    @FXML
    public HBox mainBox;

    @FXML
    private ScrollPane remoteTreeScroll;

    @FXML
    private ScrollPane localTreeScroll;

    @FXML
    private Button download;

    @FXML
    private Button upload;

    @FXML
    public Label regAnswer;

    @FXML
    public TextField loginField;

    @FXML
    public PasswordField passwordField;

    @FXML
    public Button signIn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        while (!clientCloud.isStart()) {
            waitResponse(50);
        }
    }

    public void exitAction() {
        clientNetwork.close();
        Platform.exit();
    }

    @FXML
    public void signIn() {
        clearRegAnswer();

        String userLogin = loginField.getText();
        String userPassword = DigestUtils.md5Hex(passwordField.getText());

        clientNetwork.sendAuthData(userLogin, userPassword);

        waitResponse(2000);

        if (clientCloud.isAuthorized() == 1) {
            getDefaultRemoteDirectory();
            getDefaultLocalDirectory();

            stage.getChildren().remove(0);
            mainBox.setVisible(true);
        } else if (clientCloud.isAuthorized() == -1) {
            setRegAnswer("Login or password is incorrect!");
        }
    }

    @FXML
    public void signUp() {
        clearRegAnswer();

        String userLogin = loginField.getText();
        String userPassword = DigestUtils.md5Hex(passwordField.getText());

        clientNetwork.sendRegData(userLogin, userPassword);

        while(true) {
            if (clientCloud.getRegistrationMessage() != null) {
                setRegAnswer(clientCloud.getRegistrationMessage());
                clientCloud.setRegistrationMessage(null);
                break;
            }

            waitResponse(50);
        }
    }

    private void clearRegAnswer() {
        regAnswer.setText(null);
        regAnswer.setVisible(false);
    }

    private void setRegAnswer(String msg) {
        regAnswer.setText(msg);
        regAnswer.setVisible(true);
    }

    private void getDefaultRemoteDirectory() {
        currentRemoteRoot = ClientCloud.getRemoteCloudRoot();
        currentRemoteRootFiles = clientCloud.getCurrentRemoteDirectoryFiles();

        drawTreeStructure(currentRemoteRoot, currentRemoteRootFiles, remoteTreeScroll);
    }

    private void getDefaultLocalDirectory() {
        currentLocalRoot = ClientCloud.getLocalCloudRoot();
        currentLocalRootFiles = clientCloud.getCurrentLocalDirectoryFiles();

        drawTreeStructure(currentLocalRoot, currentLocalRootFiles, localTreeScroll);
    }

    @FXML
    private void drawTreeStructure(FileDescription root, List<FileDescription> files, ScrollPane scrollPane) {
        VBox container = new VBox();
        ObservableList<Node> containerChildren = container.getChildren();

        containerChildren.clear();

        Pane rootFilePane = makeFilePane(root, scrollPane, ROOT_FILE_PANE_LAYOUT_X, ROOT_FILE_IMG_LAYOUT_X,
                ROOT_FILE_LABEL_LAYOUT_X);
        containerChildren.add(rootFilePane);

        for (FileDescription file : files) {
            Pane childrenFilePane = makeFilePane(file, scrollPane, CHILD_FILE_PANE_LAYOUT_X, CHILD_FILE_IMG_LAYOUT_X,
                    CHILD_FILE_LABEL_LAYOUT_X);
            containerChildren.add(childrenFilePane);
        }

        scrollPane.setContent(container);
        scrollPane.setPrefViewportHeight(0);
        scrollPane.setPrefHeight(0);
        scrollPane.setPannable(true);
    }

    private Pane makeFilePane(FileDescription file, ScrollPane scrollPane,
                              int paneLayoutX, int imageLayoutX, int labelLayoutX) {
        Pane filePane = new Pane();
        ObservableList<Node> filePaneChildren = filePane.getChildren();

        filePane.setLayoutX(paneLayoutX);

        String imageURL = getImageUrl(file);
        ImageView imageView = new ImageView(imageURL);

        imageView.setLayoutX(imageLayoutX);

        String fileName;
        if (file.getPath().equals(Paths.get(""))) fileName = " root";
        else fileName = file.getFileName();
        Label label = new Label(fileName);

        label.setLayoutX(labelLayoutX);

        if (!file.equals(ClientCloud.getRemoteCloudRoot())) {
            label.setOnMouseClicked(event -> {
                nClicked += 1;
                // If file was selected
                if (nClicked == 1) {
                    label.setBackground(new Background(new BackgroundFill(Color.CORNFLOWERBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
                    selectedLabel = label;
                    selectedObject = file;

                    clientCloud.setActionFile(file);

                    if (scrollPane.equals(localTreeScroll)) {
                        download.setDisable(true);
                        upload.setDisable(false);
                    } else if (scrollPane.equals(remoteTreeScroll)) {
                        download.setDisable(false);
                        upload.setDisable(true);
                    }
                } // If the file is a directory, then go to a level lower or higher
                else if (nClicked == 2 && file.getType() == Type.DIRECTORY && file.equals(selectedObject)) {
                    nClicked = 0;
                    selectedObject = null;
                    // If selected object in local repository and root, then go up
                    if (scrollPane.equals(localTreeScroll) && file.equals(clientCloud.getCurrentLocalDirectory())) {
                        setLocalDirectoryParams(file.getPath().getParent(), file.getType());
                    } // If selected object in local repository and not root, then go down
                    else if (scrollPane.equals(localTreeScroll) && !file.equals(clientCloud.getCurrentLocalDirectory())) {
                        setLocalDirectoryParams(currentLocalRoot.getPath().resolve(file.getPath()), file.getType());
                    } // If selected object in remote repository and root, then go up
                    else if (scrollPane.equals(remoteTreeScroll) && file.equals(clientCloud.getCurrentRemoteDirectory())) {
                        Path parentPath = file.getPath().getParent();
                        setRemoteDirectoryParams(file, parentPath != null ? parentPath : Paths.get(""), file.getType());
                    } // If selected object in remote repository and not root, then go down
                    else if (scrollPane.equals(remoteTreeScroll) && !file.equals(clientCloud.getCurrentRemoteDirectory())) {
                        setRemoteDirectoryParams(file, file.getPath(), file.getType());
                    }
                } else if (nClicked == 2 && selectedObject != null && (file.getType() == Type.FILE || !file.equals(selectedObject))) {
                    nClicked = 0;
                    disableSelectedObject();
                }

                if (nClicked > 2) nClicked = 0;

                clientCloud.setDirectoryStructureReceived(false);
            });
        }

        filePaneChildren.add(imageView);
        filePaneChildren.add(label);

        return filePane;
    }

    private void disableSelectedObject() {
        selectedObject = null;
        selectedLabel.setBackground(null);
        selectedLabel = null;
    }

    private void setLocalDirectoryParams(Path path, Type type) {
        FileDescription newRoot = new FileDescription(path, type);

        clientCloud.changeCurrentLocalDirectory(newRoot);

        List<FileDescription> filesInRoot = clientCloud.getCurrentLocalDirectoryFiles();
        ScrollPane variablePanel = localTreeScroll;

        currentLocalRoot = newRoot;
        currentLocalRootFiles = filesInRoot;

        drawTreeStructure(newRoot, filesInRoot, variablePanel);
    }

    private void setRemoteDirectoryParams(FileDescription srcFile, Path newPath, Type newType) {
        clientNetwork.getRemoteDirectoryTreeStructure(srcFile);

        FileDescription newRoot = new FileDescription(newPath, newType);

        clientCloud.setActionFile(newRoot);
        while(!clientCloud.isDirectoryStructureReceived()) {
            waitResponse(50);
        }

        List<FileDescription> filesInRoot = clientCloud.getCurrentRemoteDirectoryFiles();

        ScrollPane variablePanel = remoteTreeScroll;

        currentRemoteRoot = newRoot;
        currentRemoteRootFiles = filesInRoot;

        drawTreeStructure(newRoot, filesInRoot, variablePanel);
    }

    @FXML
    private void downloadFile() {
        if (selectedObject != null) {
            clientNetwork.requestDownloadFile(selectedObject);
            disableSelectedObject();

            while (true) {
                if (clientCloud.isFileReceived()) {
                    clientCloud.changeCurrentLocalDirectory(currentLocalRoot);

                    currentLocalRootFiles = clientCloud.getCurrentLocalDirectoryFiles();

                    drawTreeStructure(currentLocalRoot, currentLocalRootFiles, localTreeScroll);
                    clientCloud.setFileReceived(false);

                    break;
                }
            }
        }
    }

    @FXML
    private void uploadFile() {
        if (selectedObject != null) {
            clientNetwork.sendUploadFileDescription(selectedObject);

            while (true) {
                if (clientCloud.isFileSent()) {
                    FileDescription currentRemoteDir = clientCloud.getCurrentRemoteDirectory();

                    clientCloud.setActionFile(currentRemoteDir);
                    clientNetwork.getRemoteDirectoryTreeStructure(currentRemoteDir);

                    while (true) {
                        if (clientCloud.isDirectoryStructureReceived()) {
                            drawTreeStructure(clientCloud.getCurrentRemoteDirectory(),
                                    clientCloud.getCurrentRemoteDirectoryFiles(), remoteTreeScroll);

                            break;
                        }
                    }

                    disableSelectedObject();

                    clientCloud.setFileSent(false);

                    break;
                }
            }
        }
    }

    private String getImageUrl(FileDescription file) {
        return file.isFile() ? "img/file_icon.png" : "img/folder_icon.png";
    }

    private void waitResponse(int t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}