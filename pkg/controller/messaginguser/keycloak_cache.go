/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messaginguser

import (
	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/keycloak"
)

type keycloakCache struct {
	clients map[string]keycloak.KeycloakClient
}

type cachedClient struct {
	wrapped    keycloak.KeycloakClient
	invalidate func()
}

var (
	_ keycloak.KeycloakClient = &cachedClient{}
)

func NewKeycloakCache() keycloakCache {
	return keycloakCache{
		clients: make(map[string]keycloak.KeycloakClient, 0),
	}
}

func (c *keycloakCache) get(key string) keycloak.KeycloakClient {
	return c.clients[key]
}

func (c *keycloakCache) put(key string, client keycloak.KeycloakClient) keycloak.KeycloakClient {
	cached := &cachedClient{
		wrapped: client,
		invalidate: func() {
			delete(c.clients, key)
		},
	}
	c.clients[key] = cached
	return cached
}

func (c *cachedClient) CreateUser(realm string, user *userv1beta1.MessagingUser) error {
	err := c.wrapped.CreateUser(realm, user)
	if err != nil {
		c.invalidate()
		return err
	}
	return err
}

func (c *cachedClient) UpdateUser(realm string, existing *userv1beta1.MessagingUser, updated *userv1beta1.MessagingUser) error {
	err := c.wrapped.UpdateUser(realm, existing, updated)
	if err != nil {
		c.invalidate()
	}
	return err
}

func (c *cachedClient) GetUser(realm string, username string) (*userv1beta1.MessagingUser, error) {
	user, err := c.wrapped.GetUser(realm, username)
	if err != nil {
		c.invalidate()
	}
	return user, err
}
func (c *cachedClient) DeleteUser(realm string, user *userv1beta1.MessagingUser) error {
	err := c.wrapped.DeleteUser(realm, user)
	if err != nil {
		c.invalidate()
	}
	return err
}
func (c *cachedClient) GetUsers(realm string, filters ...keycloak.AttributeFilter) ([]*userv1beta1.MessagingUser, error) {
	users, err := c.wrapped.GetUsers(realm, filters...)
	if err != nil {
		c.invalidate()
	}
	return users, err
}

func (c *cachedClient) GetRealms() ([]string, error) {
	realms, err := c.wrapped.GetRealms()
	if err != nil {
		c.invalidate()
	}
	return realms, err
}
