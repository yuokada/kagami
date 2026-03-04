package io.github.yuokada.quarkus.proxy;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "kagami", mixinStandardHelpOptions = true, description = "Shadow Proxy server")
public class ProxyCli {
    @Option(names = "--master", description = "Master upstream URL")
    String master;

    @Option(names = "--shadow", description = "Shadow upstream URL")
    String shadow;
}
