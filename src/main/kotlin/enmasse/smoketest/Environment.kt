/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    val client = ClientBuilder(url).authorizationStrategy(TokenAuthorizationStrategy(token, user)).build();
    val service: IService = client.get(ResourceKind.SERVICE, "messaging", namespace)
    val endpoint = Endpoint(service.portalIP, service.port)
}
