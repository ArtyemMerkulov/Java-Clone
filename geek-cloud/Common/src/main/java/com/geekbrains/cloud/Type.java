package com.geekbrains.cloud;

public enum Type {
    DIRECTORY((byte) 1),
    FILE((byte) 2);

    private final byte value;

    Type(byte value) {
        this.value = value;
    }

    public static Type getTypeByValue(byte value) {
        if (value < 1) throw new IllegalArgumentException("Value cannot be lower than -1.");
        return Type.class.getEnumConstants()[value - 1];
    }

    public byte getValue() {
        return value;
    }
}
