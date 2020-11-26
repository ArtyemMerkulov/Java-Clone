package com.geekbrains.cloud.client;

import com.geekbrains.cloud.Command;
import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Type;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClientCloud {

    private static final FileDescription localCloudRoot = new FileDescription(Paths.get(new File("").getAbsolutePath()), Type.DIRECTORY);

    private static final FileDescription remoteCloudRoot = new FileDescription(Paths.get(""), Type.DIRECTORY);

    private FileDescription actionFile;

    private FileDescription currentLocalDirectory;

    private FileDescription currentRemoteDirectory;

    private List<FileDescription> currentLocalDirectoryFiles;

    private List<FileDescription> currentRemoteDirectoryFiles;

    private boolean isStart;

    private volatile boolean isFileReceived;

    private volatile boolean isFileSent;

    private volatile boolean isDirectoryStructureReceived;

    private volatile Command authCommand;

    private volatile String regAnswer;

    public ClientCloud() {
        isStart = false;
        isFileReceived = false;
        isFileSent = false;
        isDirectoryStructureReceived = false;
        authCommand = null;
        regAnswer = null;
        actionFile = remoteCloudRoot;
        changeCurrentLocalDirectory(localCloudRoot);
        changeCurrentRemoteDirectory(remoteCloudRoot, new ArrayList<>());
    }

    public FileDescription getCurrentLocalDirectory() {
        return currentLocalDirectory;
    }

    public FileDescription getCurrentRemoteDirectory() {
        return currentRemoteDirectory;
    }

    public ClientCloud changeCurrentLocalDirectory(FileDescription newRootDirectory) {
        currentLocalDirectory = new FileDescription(newRootDirectory);
        try {
            currentLocalDirectoryFiles = getFilesOnPath(currentLocalDirectory.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    public ClientCloud changeCurrentRemoteDirectory(FileDescription newRootDirectory,
                                                    List<FileDescription> remotePathFiles) {
        currentRemoteDirectory = new FileDescription(newRootDirectory);
        currentRemoteDirectoryFiles = remotePathFiles;

        return this;
    }

    private List<FileDescription> getFilesOnPath(Path path) throws IOException {
        if (!Files.isDirectory(path))
            throw new IllegalArgumentException("File in this path is not a directory.");
        return Files.walk(path, 1)
                .map(path::relativize)
                .skip(1)
                .map(p -> new FileDescription(p, Files.isDirectory(Paths.get(currentLocalDirectory.getPath()
                        .toString(), p.toString())) ? Type.DIRECTORY : Type.FILE))
                .collect(Collectors.toList());
    }

    public List<FileDescription> getCurrentLocalDirectoryFiles() {
        return currentLocalDirectoryFiles;
    }

    public List<FileDescription> getCurrentRemoteDirectoryFiles() {
        return currentRemoteDirectoryFiles;
    }

    public static FileDescription getLocalCloudRoot() {
        return localCloudRoot;
    }

    public static FileDescription getRemoteCloudRoot() {
        return remoteCloudRoot;
    }

    public FileDescription getActionFile() {
        return actionFile;
    }

    public void setActionFile(FileDescription actionFile) {
        this.actionFile = new FileDescription(actionFile);
    }

    public boolean isStart() {
        return isStart;
    }

    public void setIsStart(boolean isStart) {
        this.isStart = isStart;
    }

    public void setFileReceived(boolean isFileReceived) {
        this.isFileReceived = isFileReceived;
    }

    public boolean isFileReceived() {
        return isFileReceived;
    }

    public void setDirectoryStructureReceived(boolean isDirectoryStructureReceived) {
        this.isDirectoryStructureReceived = isDirectoryStructureReceived;
    }

    public boolean isDirectoryStructureReceived() {
        return isDirectoryStructureReceived;
    }

    public boolean isFileSent() {
        return isFileSent;
    }

    public void setFileSent(boolean isFileSent) {
        this.isFileSent = isFileSent;
    }

    public void setAuthorized(Command authCommand) {
        this.authCommand = authCommand;
    }

    public int isAuthorized() {
        if (authCommand == Command.AUTH_OK) {
            return 1;
        } else if (authCommand == Command.AUTH_NOT_OK) {
            return -1;
        } else return 0;
    }

    public void setRegistrationMessage(Command registrationMessage) {
        if (registrationMessage == Command.REGISTRATION_OK) {
            this.regAnswer = "Your are registered!";
        } else if (registrationMessage == Command.REGISTRATION_NOT_OK) {
            this.regAnswer = "Registration failed!";
        } else if (registrationMessage == null){
            this.regAnswer = null;
        }
    }

    public String getRegistrationMessage() {
        return regAnswer;
    }
}
