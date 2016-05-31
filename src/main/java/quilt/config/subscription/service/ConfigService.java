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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Main entrypoint for configuration service with arg parsing.
 *
 * @author lulf
 */
public class ConfigService {

    public static void main(String [] args) throws ParseException {

        Options options = new Options();
        options.addOption(new Option("s", "openshiftUri", true, "Openshift URI"));
        options.addOption(new Option("l", "listenAddress", true, "AMQP listen address"));
        options.addOption(new Option("p", "port", true, "AMQP listen port"));
        options.addOption(new Option("h", "help", true, "Usage"));

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            String openshiftUri = cmd.getOptionValue("s", "https://localhost:8443");
            String listenAddress = cmd.getOptionValue("l", "0.0.0.0");
            int listenPort = Integer.parseInt(cmd.getOptionValue("p", "5672"));

            String openshiftNamespace = getOpenshiftNamespace();

            IClient client = new ClientFactory().create(openshiftUri, new NoopSSLCertificateCallback());
            client.setAuthorizationStrategy(new TokenAuthorizationStrategy(getAuthenticationToken()));

            OpenshiftConfigMapDatabase database = new OpenshiftConfigMapDatabase(client, openshiftNamespace);
            database.start();

            AMQPServer server = new AMQPServer(listenAddress, listenPort, database);

            server.run();
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("config-subscription-service", options);
        } catch (IOException e) {
            System.out.println("Error running config subscription service");
            System.exit(1);
        }
    }

    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";

    private static String getOpenshiftNamespace() throws IOException {
        return readFile(new File(SERVICEACCOUNT_PATH, "namespace"));
    }

    private static String getAuthenticationToken() throws IOException {
        return readFile(new File(SERVICEACCOUNT_PATH, "token"));
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
}
