package cloud.foundry.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Greet", header = "%n@|green Hello world demo|@")
class Greet implements Runnable {

    @Option(names = {"-u", "--user"}, required = true, description = "The user name.")
    String userName;

    @SuppressWarnings("deprecation")
    public static void main(String... args) {
        String[] tmp = {"-u=World"};
        CommandLine.run(new Greet(), System.err, tmp);
    }

    public void run() {
        System.out.println("Hello " + userName);
    }
}
