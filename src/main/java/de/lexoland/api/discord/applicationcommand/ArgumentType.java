package de.lexoland.api.discord.applicationcommand;

public enum ArgumentType {

    STRING(3),
    INTEGER(4),
    BOOLEAN(5),
    USER(6),
    CHANNEL(7),
    ROLE(8),
    MENTIONABLE(9);

    private final int value;

    ArgumentType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
