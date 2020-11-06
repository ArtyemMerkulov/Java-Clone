package com.geekbrains.cloud.client;

import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

public class FilesTree {

    private List<List<FileDescription>> filesTree;

    public FilesTree() {
        filesTree = new ArrayList<>(1);
        filesTree.add(new ArrayList<>(1));
        filesTree.get(0).add(new FileDescription("root", Type.FOLDER, -1));
    }

    public FilesTree(List<Path> pathsList) {
        this();

        int maxLvl = 1;
        Set<FileDescription> tmpSet = new HashSet<>();

        for (Path path : pathsList) {
            String[] pathLvls = path.toString().split("\\\\");

            maxLvl = Math.max(maxLvl, pathLvls.length + 1);

            if (maxLvl > filesTree.size()) {
                int neededLen = maxLvl - filesTree.size() + 1;
                for (int i = 0; i < neededLen; i++) filesTree.add(new ArrayList<>());
            }

            for (int i = 0; i < pathLvls.length; i++) {
                String name = pathLvls[i];
                Type type = i != pathLvls.length - 1 && !pathLvls[i + 1].equals("") ? Type.FOLDER : Type.FILE;
                int link = filesTree.get(i).size() - 1;

                FileDescription fd = new FileDescription(name, type, link);

                if (tmpSet.add(fd)) filesTree.get(i + 1).add(fd);
            }
        }
    }

    public List<FileDescription> getFilesList(String folderName, int folderLvl) {
        if (folderName == null || folderLvl < -1 || folderLvl >= filesTree.size() - 1)
            throw new InvalidParameterException("Invalid arguments");

        List<FileDescription> filesList = null;

        int pos = getPos(folderName, folderLvl);

        if (pos > -1) {
            filesList = filesTree.get(folderLvl + 1)
                    .stream()
                    .filter(file -> file.getLink() == pos)
                    .collect(Collectors.toList());
        }

        return filesList;
    }

    public int getPos(String folderName, int folderLvl) {
        int l = folderLvl;
        for (int i = 0; i < filesTree.get(l).size(); i++)
            if (filesTree.get(l).get(i).getType() == Type.FOLDER &&
                    filesTree.get(l).get(i).getName().equals(folderName)) {
                return i;
            }

        return -1;
    }

    public FileDescription getFolder(String folderName, int folderLvl) {
        for (FileDescription fileDescription : filesTree.get(folderLvl + 1))
            if (fileDescription.getType() == Type.FOLDER && fileDescription.getName().equals(folderName))
                return fileDescription;

        return null;
    }
}
