package cloud.foundry.cli.logic.diff.output;

public enum WrapperColor {
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    DEFAULT("\u001B[0m");

    private String color;

    private WrapperColor(String color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return color;
    }
}
