package bananaplus.enums;

public enum SwitchMode {
    Normal,
    Silent,
    Inventory;

    public boolean onlyHotbar() {
        return this != Inventory;
    }
}
