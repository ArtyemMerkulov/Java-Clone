package com.geekbrains.cloud;

public enum Command {
    AUTH_DATA((byte) 1),
    AUTH_OK((byte) 2),
    AUTH_NOT_OK((byte) 3),

    REGISTRATION_DATA((byte) 4),
    REGISTRATION_OK((byte) 5),
    REGISTRATION_NOT_OK((byte) 6),

    REQUEST_CLOUD_TREE_STRUCTURE((byte) 7),

    REQUEST_DOWNLOAD_FILE((byte) 8),
    RECEIVE_PART_OF_DOWNLOAD_FILE((byte) 9),
    RECEIVE_END_PART_OF_DOWNLOAD_FILE((byte) 10),

    RECEIVE_UPLOAD_FILE_DESCRIPTION((byte) 11),
    UPLOAD_FILE_DESCRIPTION_RECEIVED((byte) 12),

    RECEIVE_PART_OF_UPLOAD_FILE((byte) 13),
    RECEIVE_END_PART_OF_UPLOAD_FILE((byte) 14),
    UPLOAD_FILE_RECEIVED((byte) 15);

    private final byte value;

    Command(byte value) {
        this.value = value;
    }

    public static Command getCommandByValue(byte value) {
        if (value < 1) throw new IllegalArgumentException("Command value cannot be lower than -1.");
        return Command.class.getEnumConstants()[value - 1];
    }

    public byte getValue() {
        return value;
    }

    public static int maxValue() {
        return Command.class.getEnumConstants().length;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
