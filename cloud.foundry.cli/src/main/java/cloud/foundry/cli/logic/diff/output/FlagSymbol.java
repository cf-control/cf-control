package cloud.foundry.cli.logic.diff.output;

/**
 * Holds symbols that are used to indicate whether something was added, removed or unchanged.
 */
public enum FlagSymbol {
    ADDED("+"),
    REMOVED("-"),
    NONE(" ");

    private String symbol;

    FlagSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
