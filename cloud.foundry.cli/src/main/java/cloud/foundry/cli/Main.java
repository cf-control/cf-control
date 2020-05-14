package cloud.foundry.cli;

import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        CommandLine.run(null, System.err, args);
    }
}
