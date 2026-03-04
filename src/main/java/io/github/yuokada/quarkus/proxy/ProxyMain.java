package io.github.yuokada.quarkus.proxy;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

@QuarkusMain
public class ProxyMain {
    public static void main(String... args) {
        ProxyCli cli = new ProxyCli();
        CommandLine commandLine = new CommandLine(cli);
        try {
            commandLine.parseArgs(args);
        } catch (CommandLine.ParameterException exception) {
            System.err.println(exception.getMessage());
            commandLine.usage(System.err);
            return;
        }

        if (cli.master != null) {
            System.setProperty("proxy.upstream.master", cli.master);
        }
        if (cli.shadow != null) {
            System.setProperty("proxy.upstream.shadow", cli.shadow);
        }

        Quarkus.run(new String[0]);
    }
}
