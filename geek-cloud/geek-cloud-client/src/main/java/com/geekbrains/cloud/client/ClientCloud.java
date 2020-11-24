package com.geekbrains.cloud.client;

import com.geekbrains.cloud.Command;
import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Type;
import com.sun.istack.internal.NotNull;

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

    public ClientCloud() {
        isStart = false;
        isFileReceived = false;
        isFileSent = false;
        isDirectoryStructureReceived = false;
        authCommand = null;
        actionFile = remoteCloudRoot;
        changeCurrentLocalDirectory(localCloudRoot);
        changeCurrentRemoteDirectory(remoteCloudRoot, new ArrayList<>());
    }

    @NotNull
    public FileDescription getCurrentLocalDirectory() {
        return currentLocalDirectory;
    }

    @NotNull
    public FileDescription getCurrentRemoteDirectory() {
        return currentRemoteDirectory;
    }

    public ClientCloud changeCurrentLocalDirectory(@NotNull FileDescription newRootDirectory) {
        currentLocalDirectory = new FileDescription(newRootDirectory);
        try {
            currentLocalDirectoryFiles = getFilesOnPath(currentLocalDirectory.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    public ClientCloud changeCurrentRemoteDirectory(@NotNull FileDescription newRootDirectory,
                                                    List<FileDescription> remotePathFiles) {
        currentRemoteDirectory = new FileDescription(newRootDirectory);
        currentRemoteDirectoryFiles = remotePathFiles;

        return this;
    }

    @NotNull
    private List<FileDescription> getFilesOnPath(@NotNull Path path) throws IOException {
        if (!Files.isDirectory(path))
            throw new IllegalArgumentException("File in this path is not a directory.");
        return Files.walk(path, 1)
                .map(path::relativize)
                .skip(1)
                .map(p -> new FileDescription(p, Files.isDirectory(Paths.get(currentLocalDirectory.getPath()
                        .toString(), p.toString())) ? Type.DIRECTORY : Type.FILE))
                .collect(Collectors.toList());
    }

    @NotNull
    public List<FileDescription> getCurrentLocalDirectoryFiles() {
        return currentLocalDirectoryFiles;
    }

    @NotNull
    public List<FileDescription> getCurrentRemoteDirectoryFiles() {
        return currentRemoteDirectoryFiles;
    }

    @NotNull
    public static FileDescription getLocalCloudRoot() {
        return localCloudRoot;
    }

    @NotNull
    public static FileDescription getRemoteCloudRoot() {
        return remoteCloudRoot;
    }

    @NotNull
    public FileDescription getActionFile() {
        return actionFile;
    }

    public void setActionFile(FileDescription actionFile) {
        this.actionFile = new FileDescription(actionFile);
    }

    @NotNull
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
//        System.out.println("TEST: " + authCommand);
        if (authCommand == Command.AUTH_OK) {
            return 1;
        } else if (authCommand == Command.AUTH_NOT_OK) {
            return -1;
        } else return 0;
    }
}
