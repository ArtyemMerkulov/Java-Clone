package com.geekbrains.cloud.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ClientCloud {

    private static final Path localCloudPath = Paths.get("C:\\Users\\surga\\OneDrive\\Рабочий стол\\Уроки\\Разработка сетевого хранилища на Java\\");

    private FilesTree remoteTree;
    private FilesTree localTree;

    private boolean start = false;

    private Path actionFilePath;

    private boolean fileReceived;

    public ClientCloud() {
        remoteTree = new FilesTree();
        try {
            localTree = new FilesTree(initLocalFolderTreeStructure(localCloudPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Path getActionFilePath() {
        return actionFilePath;
    }

    private List<Path> initLocalFolderTreeStructure(Path path) throws IOException {
        return Files.walk(path)
                .filter(Files::isRegularFile)
                .map(path::relativize)
                .collect(Collectors.toList());
    }

    public FilesTree getLocalTreeStructure() {
        return localTree;
    }

    public void setLocalTreeStructure(List<Path> localPathsList) {
        localTree = new FilesTree(localPathsList);
    }

    public void setRemoteTreeStructure(List<Path> remotePathsList) {
        remoteTree = new FilesTree(remotePathsList);
    }

    public FilesTree getRemoteTreeStructure() {
        return remoteTree;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public boolean getStart() {
        return start;
    }

    public static Path getFullPath(FilesTree filesTree, FileDescription file, int lvl) {
        return filesTree.getPath(file, lvl);
    }

    public void setFileReceived(boolean fileReceived) {
        this.fileReceived = fileReceived;
    }

    public boolean isFileReceived() {
        return fileReceived;
    }

    public void setActionFilePath(Path path) {
        actionFilePath = path;
    }

    public static Path getLocalCloudPath() {
        return localCloudPath;
    }
}
