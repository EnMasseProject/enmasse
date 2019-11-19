/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package keycloak

import (
	"fmt"

	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
)

type fakeClient struct {
	users map[string][]*userv1beta1.MessagingUser
}

var (
	_ KeycloakClient = &fakeClient{}
)

func (c *fakeClient) CreateUser(realm string, user *userv1beta1.MessagingUser) error {
	if _, ok := c.users[realm]; !ok {
		c.users[realm] = make([]*userv1beta1.MessagingUser, 0)
	}
	for _, users := range c.users {
		for _, existing := range users {
			if existing.Name == user.Name {
				return fmt.Errorf("User %s already exists!", user.Name)
			}
		}
	}
	c.users[realm] = append(c.users[realm], user)
	return nil
}

func (c *fakeClient) GetUser(realm string, username string) (*userv1beta1.MessagingUser, error) {
	users, ok := c.users[realm]
	if !ok {
		return nil, fmt.Errorf("Unknown realm %s", realm)
	}

	for _, user := range users {
		if user.Spec.Username == username {
			return user, nil
		}
	}
	return nil, nil
}

func (c *fakeClient) DeleteUser(realm string, user *userv1beta1.MessagingUser) error {
	users, ok := c.users[realm]
	if !ok {
		return fmt.Errorf("Unknown realm %s", realm)
	}

	element := -1
	for i, user := range users {
		if user.Spec.Username == user.Spec.Username {
			element = i
			break
		}
	}
	if element >= 0 {
		c.users[realm][element] = c.users[realm][len(c.users[realm])-1]
		c.users[realm] = c.users[realm][:len(c.users[realm])-1]
	}
	return nil
}

func (c *fakeClient) UpdateUser(realm string, _ *userv1beta1.MessagingUser, updated *userv1beta1.MessagingUser) error {
	if _, ok := c.users[realm]; !ok {
		c.users[realm] = make([]*userv1beta1.MessagingUser, 0)
	}
	for _, users := range c.users {
		for i, existing := range users {
			if existing.Spec.Username == updated.Spec.Username {
				c.users[realm][i] = updated
				return nil
			}
		}
	}
	c.users[realm] = append(c.users[realm], updated)
	return nil
}

func (c *fakeClient) GetUsers(realm string, filters ...AttributeFilter) ([]*userv1beta1.MessagingUser, error) {
	users, ok := c.users[realm]
	if !ok {
		return nil, fmt.Errorf("Unknown realm %s", realm)
	}
	return users, nil
}

func (c *fakeClient) GetRealms() ([]string, error) {
	realms := make([]string, 0)
	for key, _ := range c.users {
		realms = append(realms, key)
	}
	return realms, nil
}
