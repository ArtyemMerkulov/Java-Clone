package com.geekbrains.cloud;

public enum Command {
    REQUEST_CLOUD_TREE_STRUCTURE((byte) 1),
    REQUEST_DOWNLOAD_FILE((byte) 2),
    RECEIVE_PART_OF_DOWNLOAD_FILE((byte) 3),
    RECEIVE_END_PART_OF_DOWNLOAD_FILE((byte) 4);

    private final byte value;

    Command(byte value) {
        this.value = value;
    }

    public static Command getCommandByValue(byte value) {
        if (value < 1) throw new IllegalArgumentException("com.geekbrains.cloud.Command value cannot be lower than -1.");
        return Command.class.getEnumConstants()[value - 1];
    }

    public byte getValue() {
        return value;
    }

    public static int maxValue() {
        return Command.class.getEnumConstants().length;
    }
}
