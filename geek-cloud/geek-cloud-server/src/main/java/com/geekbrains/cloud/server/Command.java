package com.geekbrains.cloud.server;

public enum Command {
    REQUEST_CLOUD_TREE_STRUCTURE((byte) 3),
    REQUEST_DOWNLOAD_FILE((byte) 4);

    private final byte value;

    Command(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}
