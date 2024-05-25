package me.ricky.banana.enums;

public enum SwitchMode {
    Normal,
    Silent,
    Inventory;

    public boolean onlyHotbar() {
        return this != Inventory;
    }
}