package quilt.config.generator.service;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;

/**
 * @author lulf
 */
public class Main {

    public static void main(String args[]) {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption(createRequiredOption("o", "openshiftUrl", "Openshift Url"));
        options.addOption(createRequiredOption("c", "configUrl", "Config service URL"));
        options.addOption(createRequiredOption("n", "namespace", "Openshift namespace"));
        options.addOption(createRequiredOption("t", "token", "Openshift token"));
        options.addOption(createRequiredOption("u", "user", "Openshift user"));

        try {
            CommandLine cmd = parser.parse(options, args);
            String openshiftUrl = cmd.getOptionValue("o");
            String configUrl = cmd.getOptionValue("c");
            String namespace = cmd.getOptionValue("n");
            String token = cmd.getOptionValue("t");
            String user = cmd.getOptionValue("u");

            GeneratorService service = new GeneratorService(GeneratorServiceOptions.create(configUrl, openshiftUrl, namespace, token, user));
            service.run();
        } catch (ParseException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("config-generator-service", options);
            System.exit(1);
        }
    }

    private static Option createRequiredOption(String name, String longName, String description) {
        return Option.builder(name)
                .longOpt(longName)
                .hasArg()
                .desc(description)
                .required()
                .build();
    }
}
