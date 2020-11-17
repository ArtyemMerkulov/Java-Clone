package com.geekbrains.cloud.server;

import com.geekbrains.cloud.FileDescription;
import com.geekbrains.cloud.Type;
import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ServerCloud {

    private static final FileDescription cloudRoot = new FileDescription(Paths.get("C:\\Users\\surga\\OneDrive\\Рабочий стол\\Уроки\\Разработка сетевого хранилища на Java\\CloudProject\\geek-cloud"), Type.DIRECTORY);

    private FileDescription currentRoot;

    private List<FileDescription> currentDirectoryTreeStructure;

    public ServerCloud() {
        currentRoot = null;
        try {
            currentDirectoryTreeStructure = getFilesOnPath(cloudRoot.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO изменение текущей рабочей директории:
    // Текущая директория равна "".
    // Если полученная директория равна "" и полученная директория равна текущей:
    //    Бросаем исключение о том, что ниже спуститься нельзя.
    // Иначе если полученная директория не равна текущей директории:
    //    получаем все файлы из cloudRoot + "/" + текущая директория + "/" + полученный путь;
    //    текущая директория = текущей директории + "/" + полученный путь;
    // Иначе:
    //    получаем все файлы из cloudRoot + "/" + текущая директория;
    //    текущая директория = Родитель(текущей директории) != null ? Родитель(текущей директории) : ""
    public ServerCloud changeCurrentDirectoryTreeStructure(@NotNull FileDescription newCurrRoot) {
        if (newCurrRoot.getPath().equals(Paths.get("")) &&
                currentRoot != null && currentRoot.equals(newCurrRoot)) {
            throw new IllegalArgumentException("Cannot go above the root.");
        } else if (!newCurrRoot.equals(currentRoot)) {
//            Path followPath = currentRoot == null ? Paths.get("") : currentRoot.getPath();
//            currentRoot = new FileDescription(followPath.resolve(newCurrRoot.getPath()), Type.DIRECTORY);
            currentRoot = new FileDescription(newCurrRoot.getPath(), Type.DIRECTORY);
        } else if (newCurrRoot.equals(currentRoot)) {
            Path parentPath = newCurrRoot.getPath().getParent();
            currentRoot = new FileDescription(parentPath != null ? parentPath : Paths.get(""), Type.DIRECTORY);
        }
        System.out.println("CURRENT ROOT: " + currentRoot);
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
}
