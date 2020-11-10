package com.geekbrains.cloud.client;

import java.security.InvalidParameterException;
import java.util.Objects;

public class FileDescription {

    private String name;

    private Type type;

    private int link;

    public FileDescription() {
        this.name = "root";
        this.type = Type.FOLDER;
        this.link = -1;
    }

    public FileDescription(String name, Type type, int link) {
        if (name == null || type == null || link < -1)
            throw new InvalidParameterException("Invalid arguments");

        this.name = name;
        this.type = type;
        this.link = link;
    }

    public FileDescription(FileDescription other) {
        this.name = other.getName();
        this.type = other.getType();
        this.link = other.getLink();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getLink() {
        return link;
    }

    public void setLink(int link) {
        this.link = link;
    }

    public boolean isFolder() {
        return type == Type.FOLDER;
    }

    public boolean isFile() {
        return type == Type.FILE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileDescription)) return false;
        FileDescription that = (FileDescription) o;
        return getLink() == that.getLink() &&
                getName().equals(that.getName()) &&
                getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getType(), getLink());
    }

    @Override
    public String toString() {
        return "(" + name + " " + type + " " + link + ")";
    }
}
