package com.geekbrains.cloud;

import com.sun.istack.internal.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.Objects;

public class FileDescription {

    private Path path;

    private Type type;

    public FileDescription() {
        this.path = Paths.get("/");
        this.type = Type.DIRECTORY;
    }

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

    @NotNull
    public Path getPath() {
        return path;
    }

    public void setPath(@NotNull Path path) {
        this.path = path;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    public void setType(@NotNull Type type) {
        this.type = type;
    }

    public boolean isDirectory() {
        return type == Type.DIRECTORY;
    }

    public boolean isFile() {
        return type == Type.FILE;
    }

    @NotNull
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

    @NotNull
    public FileDescription getParent() {
        Path parentPath = this.path.getParent();
        return new FileDescription(parentPath != null ? parentPath : Paths.get(""), Type.DIRECTORY);
    }
}
