package com.geekbrains.cloud.client;

import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Type;
import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

public class ClientCloud {

    private static final FileDescription localCloudRoot = new FileDescription(Paths.get("C:\\Users\\surga\\OneDrive\\Рабочий стол\\Уроки\\Разработка сетевого хранилища на Java\\"), Type.DIRECTORY);

    private static final FileDescription remoteCloudRoot = new FileDescription(Paths.get(""), Type.DIRECTORY);

    private FileDescription actionFile;

    private FileDescription currentLocalDirectory;

    private FileDescription currentRemoteDirectory;

    private List<FileDescription> currentLocalDirectoryFiles;

    private List<FileDescription> currentRemoteDirectoryFiles;

    private boolean start;

    private boolean fileReceived;

    private volatile boolean isDirectoryStructureReceived;

    public ClientCloud() {
        start = false;
        fileReceived = false;
        isDirectoryStructureReceived = false;
        actionFile = remoteCloudRoot;
        changeCurrentLocalDirectory(localCloudRoot);
        changeCurrentRemoteDirectory(remoteCloudRoot, new ArrayList<>());
    }

    @NotNull
    public FileDescription getCurrentLocalDirectory() {
        return currentLocalDirectory;
    }

    public void setCurrentLocalDirectory(@NotNull FileDescription currentLocalDirectory) {
        this.currentLocalDirectory = currentLocalDirectory;
    }

    @NotNull
    public FileDescription getCurrentRemoteDirectory() {
        return currentRemoteDirectory;
    }

    public void setCurrentRemoteDirectory(@NotNull FileDescription currentRemoteDirectory) {
        this.currentRemoteDirectory = currentRemoteDirectory;
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
    public FileDescription getActionFilePath() {
        return actionFile;
    }

    public void setActionFilePath(FileDescription actionFile) {
        this.actionFile = new FileDescription(actionFile);
    }

    @NotNull
    public boolean getStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public void setFileReceived(boolean fileReceived) {
        this.fileReceived = fileReceived;
    }

    public boolean isFileReceived() {
        return fileReceived;
    }

    public void setDirectoryStructureReceived(boolean isDirectoryStructureReceived) {
        this.isDirectoryStructureReceived = isDirectoryStructureReceived;
    }

    public boolean isDirectoryStructureReceived() {
        return isDirectoryStructureReceived;
    }
}
