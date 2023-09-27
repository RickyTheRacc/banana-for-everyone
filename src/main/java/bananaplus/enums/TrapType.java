package bananaplus.enums;

public enum TrapType {
    Face,
    Top,
    Both,
    Any;

    public boolean face() {
        return this != Top;
    }

    public boolean top() {
        return this != Face;
    }
}
