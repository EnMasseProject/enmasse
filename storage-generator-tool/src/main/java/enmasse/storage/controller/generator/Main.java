package enmasse.storage.controller.generator;

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
    public static void main(String [] args) {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption(createRequiredOption("i", "inputFile", "Input configuration file"));
        options.addOption(createRequiredOption("o", "outputDir", "Output directory"));

        try {
            CommandLine cmd = parser.parse(options, args);
            new FileGenerator().generate(cmd.getOptionValue("i"), cmd.getOptionValue("o"));

        } catch (ParseException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("config-generator", options);
            System.exit(1);

        } catch (IOException e) {
            System.out.println(String.format("Error generating config: %s", e.getMessage()));
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
