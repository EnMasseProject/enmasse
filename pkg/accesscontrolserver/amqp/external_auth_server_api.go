/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package amqp

const addressAuthzCapability = "ADDRESS-AUTHZ"

const authenticatedIdentityProperty = "authenticated-identity"
const groupsProperty = "groups"

func buildAuthenticatedIdentity(c string) map[string]interface{} {
	authUser := make(map[string]interface{})
	authUser["sub"] = c
	authUser["preferred_username"] = c
	return authUser
}

func buildGroups() []string {
	return []string{"manage"} // Allow all
}
