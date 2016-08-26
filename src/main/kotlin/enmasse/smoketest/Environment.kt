package enmasse.smoketest

import com.openshift.restclient.ClientBuilder
import com.openshift.restclient.ResourceKind
import com.openshift.restclient.authorization.TokenAuthorizationStrategy
import com.openshift.restclient.model.IService

/**
 * @author Ulf Lilleengen
 */
object Environment {
    val user = System.getenv("OPENSHIFT_USER")
    val token = System.getenv("OPENSHIFT_TOKEN")
    val url = System.getenv("OPENSHIFT_URL")
    val namespace = "enmasse-ci"
    //val client = ClientBuilder(url).authorizationStrategy(TokenAuthorizationStrategy(token, user)).build();
    //val service: IService = client.get(ResourceKind.SERVICE, "messaging", namespace)
    //val endpoint = Endpoint(service.portalIP, service.port)
    val endpoint = Endpoint("172.30.187.220", 5672)
}
