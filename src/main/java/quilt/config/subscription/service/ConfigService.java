package quilt.config.subscription.service;

import quilt.config.subscription.service.amqp.AMQPServer;
import com.openshift.restclient.ClientFactory;
import com.openshift.restclient.IClient;
import com.openshift.restclient.NoopSSLCertificateCallback;
import com.openshift.restclient.authorization.TokenAuthorizationStrategy;
import quilt.config.subscription.service.openshift.OpenshiftConfigMapDatabase;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Main entrypoint for configuration service with arg parsing.
 *
 * @author lulf
 */
public class ConfigService {

    public static void main(String [] args) throws ParseException {

        Options options = new Options();
        options.addOption(new Option("s", "openshiftUri", true, "Openshift URI"));
        options.addOption(requiredOption("n", "namespace", true, "Openshift namespace"));
        options.addOption(requiredOption("u", "user", true, "Openshift user"));
        options.addOption(requiredOption("t", "token", true, "Openshift auth token"));
        options.addOption(new Option("l", "listenAddress", true, "AMQP listen address"));
        options.addOption(new Option("p", "port", true, "AMQP listen port"));
        options.addOption(new Option("h", "help", true, "Usage"));

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            String openshiftUri = cmd.getOptionValue("s", "https://localhost:8443");
            String openshiftToken = cmd.getOptionValue("t");
            String openshiftUser = cmd.getOptionValue("u");
            String openshiftNamespace = cmd.getOptionValue("n");
            String listenAddress = cmd.getOptionValue("l", "0.0.0.0");
            int listenPort = Integer.parseInt(cmd.getOptionValue("p", "5672"));

            IClient client = new ClientFactory().create(openshiftUri, new NoopSSLCertificateCallback());
            client.setAuthorizationStrategy(new TokenAuthorizationStrategy(openshiftToken, openshiftUser));

            OpenshiftConfigMapDatabase database = new OpenshiftConfigMapDatabase(client, openshiftNamespace);
            database.start();

            AMQPServer server = new AMQPServer(listenAddress, listenPort, database);

            server.run();
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("config-service", options);
        }
    }

    private static Option requiredOption(String shortName, String longName, boolean takesArguments, String description) {
        Option o = new Option(shortName, longName, takesArguments, description);
        o.setRequired(true);
        return o;
    }
}
