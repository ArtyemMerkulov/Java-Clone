package com.geekbrains.cloud.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ServerCloud {

    private static final Path cloudPath = Paths.get("C:\\Users\\surga\\OneDrive\\Рабочий стол\\Уроки\\Разработка сетевого хранилища на Java\\geek-cloud");

    private List<Path> folderTreeStructure;

    public ServerCloud() {
        try {
            folderTreeStructure = initFolderTreeStructure();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Path> initFolderTreeStructure() throws IOException {
        return Files.walk(cloudPath)
                .filter(Files::isRegularFile)
                .map(cloudPath::relativize)
                .collect(Collectors.toList());
    }

    public List<Path> getFolderTreeStructure() {
        return folderTreeStructure;
    }
}
