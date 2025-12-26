package wily.legacy.core.network.types;

public enum SendType {
    SMALL, MEDIUM, CLEAR_SMALL, CLEAR_MEDIUM, CLEAR_ALL;

    public boolean isSmall() {
        return this == SMALL || this == CLEAR_SMALL || this == CLEAR_ALL;
    }

    public boolean isMedium() {
        return this == MEDIUM || this == CLEAR_MEDIUM || this == CLEAR_ALL;
    }

    public boolean clear() {
        return this.ordinal() > 1;
    }
}