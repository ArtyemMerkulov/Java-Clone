package com.geekbrains.cloud.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ServerCloud {

    private static final Path cloudPath = Paths.get("C:\\Users\\surga\\OneDrive\\Рабочий стол\\Уроки\\Разработка сетевого хранилища на Java\\Урок 3\\geek-cloud");

    private List<Path> folderTreeStructure;

    public ServerCloud() {
        try {
            folderTreeStructure = initFolderTreeStructure(cloudPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Path> initFolderTreeStructure(Path path) throws IOException {
        return Files.walk(path)
                .filter(Files::isRegularFile)
                .map(path::relativize)
                .collect(Collectors.toList());
    }

    public List<Path> getFolderTreeStructure() {
        return folderTreeStructure;
    }

    public static Path getCloudPath() {
        return cloudPath;
    }
}
