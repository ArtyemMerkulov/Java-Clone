package com.geekbrains.cloud.client;


import java.nio.file.Path;
import java.util.List;

public class ClientCloud {

    private FilesTree ft;

    public ClientCloud() {
        ft = new FilesTree();
    }

    public FilesTree geTreeStructure() {
        return ft;
    }

    public void setTreeStructure(List<Path> pathsList) {
        ft = new FilesTree(pathsList);
    }
}
