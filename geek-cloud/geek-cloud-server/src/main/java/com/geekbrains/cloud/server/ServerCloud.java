package com.geekbrains.cloud.server;

import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Type;
import com.sun.istack.internal.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ServerCloud {

    private static final FileDescription cloudRoot = new FileDescription(Paths.get(new File("").getAbsolutePath()), Type.DIRECTORY);

    private FileDescription currentRoot;

    private FileDescription actionFile;

    private List<FileDescription> currentDirectoryTreeStructure;

    public ServerCloud() {
        currentRoot = null;
        actionFile = null;
        try {
            currentDirectoryTreeStructure = getFilesOnPath(cloudRoot.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileDescription getCurrentRoot() {
        return currentRoot;
    }

    public ServerCloud changeCurrentDirectoryTreeStructure(@NotNull FileDescription newCurrRoot) {
        if (!newCurrRoot.equals(currentRoot) || (newCurrRoot.getPath().equals(Paths.get("")) &&
                currentRoot != null && currentRoot.equals(newCurrRoot))) {
            currentRoot = new FileDescription(newCurrRoot);
        } else if (newCurrRoot.equals(currentRoot)) {
            Path parentPath = newCurrRoot.getPath().getParent();
            currentRoot = new FileDescription(parentPath != null ? parentPath : Paths.get(""), Type.DIRECTORY);
        }

        try {
            currentDirectoryTreeStructure = getFilesOnPath(cloudRoot.getPath().resolve(currentRoot.getPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    @NotNull
    private List<FileDescription> getFilesOnPath(@NotNull Path path) throws IOException {
        if (!Files.isDirectory(path))
            throw new IllegalArgumentException("File in this path is not a directory.");
        return Files.walk(path, 1)
                .map(path::relativize)
                .skip(1)
                .map(p -> new FileDescription(currentRoot == null ? p : currentRoot.getPath().resolve(p),
                        Files.isDirectory(path.resolve(p)) ? Type.DIRECTORY : Type.FILE))
                .collect(Collectors.toList());
    }

    @NotNull
    public List<FileDescription> getCurrentDirectoryTreeStructure() {
        return currentDirectoryTreeStructure;
    }

    @NotNull
    public static Path getCloudRootPath() {
        return cloudRoot.getPath();
    }

    public void setActionFile(FileDescription actionFile) {
        this.actionFile = actionFile;
    }

    public FileDescription getActionFile() {
        return actionFile;
    }
}
