package quilt.config.generator.agent;

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

        try {
            CommandLine cmd = parser.parse(options, args);
            String openshiftUrl = cmd.getOptionValue("o");
            String configUrl = cmd.getOptionValue("c");

            GeneratorAgent service = new GeneratorAgent(GeneratorAgentOptions.fromUrls(configUrl, openshiftUrl));
            service.run();
        } catch (ParseException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("config-generator-agent", options);
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Error creating generator agent: " + e.getMessage());
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
