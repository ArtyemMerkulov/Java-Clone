package com.geekbrains.cloud;

import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Objects;

public class FileDescription {

    private final Path path;

    private Type type;

    public FileDescription(Path path, Type type) {
        if (path == null || type == null)
            throw new InvalidParameterException("Invalid arguments");

        this.path = path;
        this.type = type;
    }

    public FileDescription(FileDescription other) {
        this.path = other.getPath();
        this.type = other.getType();
    }

    public Path getPath() {
        return path;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isFile() {
        return type == Type.FILE;
    }

    public String getFileName() {
        return path.getFileName().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileDescription)) return false;
        FileDescription that = (FileDescription) o;
        return getPath().equals(that.getPath()) &&
                getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath(), getType());
    }

    @Override
    public String toString() {
        return "(" + path + " " + type + ")";
    }
}
