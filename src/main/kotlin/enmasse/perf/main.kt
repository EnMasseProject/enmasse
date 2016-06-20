package enmasse.perf

import com.openshift.internal.restclient.ResourceFactory
import com.openshift.restclient.ClientBuilder
import com.openshift.restclient.authorization.TokenAuthorizationStrategy
import com.openshift.restclient.model.IResource

public fun main(args: Array<String>) {
    val user = System.getenv("OPENSHIFT_USER")
    val token = System.getenv("OPENSHIFT_TOKEN")
    val url = System.getenv("OPENSHIFT_URL")
    val client = ClientBuilder(url).authorizationStrategy(TokenAuthorizationStrategy(token, user)).build();
    val rf = ResourceFactory(client)

    val resources = listOf("configmap-bridge-rc.yaml", "configuration-service.yaml", "messaging-service.yaml", "qdrouterd-rc.yaml", "ragent-rc.yaml", "ragent-service.yaml", "rc-generator-rc.yaml")
    resources.forEach { r ->
        val rstream = ClassLoader.getSystemResourceAsStream(r)
        val resource = rf.create<IResource>(rstream)
        client.create(resource)
        System.out.println("Created resource ${resource.name}")
    }
}