package enmasse.broker.prestop

import com.openshift.internal.restclient.model.Pod
import com.openshift.restclient.ClientBuilder
import com.openshift.restclient.IClient
import com.openshift.restclient.ResourceKind
import com.openshift.restclient.authorization.TokenAuthorizationStrategy
import com.openshift.restclient.images.DockerImageURI
import io.vertx.core.impl.FileResolver
import java.io.File
import java.nio.file.Files
import java.util.*

/**
 * @author Ulf Lilleengen
 */
fun main(args: Array<String>) {
    System.setProperty(FileResolver.CACHE_DIR_BASE_PROP_NAME, "/tmp/vert.x")

    val debug = System.getenv("PRESTOP_DEBUG") != null
    val address = System.getenv("QUEUE_NAME")
    val messagingHost = System.getenv("MESSAGING_SERVICE_HOST")
    val messagingPort = Integer.parseInt(System.getenv("MESSAGING_SERVICE_PORT"))

    val from = Endpoint("127.0.0.1", 5673)
    val to = Endpoint(messagingHost, messagingPort)

    val debugFn: Optional<() -> Unit> = if (debug) {
        val kubeHost = System.getenv("KUBERNETES_SERVICE_HOST")
        val kubePort = System.getenv("KUBERNETES_SERVICE_PORT")
        val kubeUrl = "https://${kubeHost}:${kubePort}";
        val client = ClientBuilder(kubeUrl).authorizationStrategy(TokenAuthorizationStrategy(openshiftToken())).build();
        val namespace = openshiftNamespace()
        Optional.of({
            signalSuccess(client, namespace, address)
        })
    } else {
        Optional.empty()
    }
    val mgmtEndpoint = Endpoint("127.0.0.1", 61616)
    val client = DrainClient(mgmtEndpoint, from, address, debugFn)
    client.drainMessages(to)
}

private fun signalSuccess(osClient: IClient, namespace: String, address: String) {
    val pod = osClient.resourceFactory.create<Pod>("v1", ResourceKind.POD)
    pod.name = "mypod-${address}"
    pod.addContainer("containerfoo")
    val cont = pod.containers.iterator().next()
    cont.image = DockerImageURI("lulf/artemis:latest")
    cont.addEnvVar("QUEUE_NAME", "myqueue")

    osClient.create(pod, namespace)
    Thread.sleep(10000)
}

val serviceAccountPath = "/var/run/secrets/kubernetes.io/serviceaccount";
fun openshiftNamespace(): String {
    return readFile(File(serviceAccountPath, "namespace"))
}

fun openshiftToken(): String {
    return readFile(File(serviceAccountPath, "token"))
}

fun readFile(file: File): String {
    return String(Files.readAllBytes(file.toPath()));
}
