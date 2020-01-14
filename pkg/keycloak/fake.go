/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package keycloak

import (
	"fmt"

	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
)

type FakeClient struct {
	Users map[string][]*userv1beta1.MessagingUser
}

var (
	_ KeycloakClient = &FakeClient{}
)

func (c *FakeClient) CreateUser(realm string, user *userv1beta1.MessagingUser) error {
	if _, ok := c.Users[realm]; !ok {
		c.Users[realm] = make([]*userv1beta1.MessagingUser, 0)
	}
	for _, users := range c.Users {
		for _, existing := range users {
			if existing.Name == user.Name {
				return fmt.Errorf("User %s already exists!", user.Name)
			}
		}
	}
	c.Users[realm] = append(c.Users[realm], user)
	return nil
}

func (c *FakeClient) GetUser(realm string, username string) (*userv1beta1.MessagingUser, error) {
	users, ok := c.Users[realm]
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

func (c *FakeClient) DeleteUser(realm string, user *userv1beta1.MessagingUser) error {
	users, ok := c.Users[realm]
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
		c.Users[realm][element] = c.Users[realm][len(c.Users[realm])-1]
		c.Users[realm] = c.Users[realm][:len(c.Users[realm])-1]
	}
	return nil
}

func (c *FakeClient) UpdateUser(realm string, _ *userv1beta1.MessagingUser, updated *userv1beta1.MessagingUser) error {
	if _, ok := c.Users[realm]; !ok {
		c.Users[realm] = make([]*userv1beta1.MessagingUser, 0)
	}
	for _, users := range c.Users {
		for i, existing := range users {
			if existing.Spec.Username == updated.Spec.Username {
				c.Users[realm][i] = updated
				return nil
			}
		}
	}
	c.Users[realm] = append(c.Users[realm], updated)
	return nil
}

func (c *FakeClient) GetUsers(realm string, filters ...AttributeFilter) ([]*userv1beta1.MessagingUser, error) {
	users, ok := c.Users[realm]
	if !ok {
		return nil, fmt.Errorf("Unknown realm %s", realm)
	}
	return users, nil
}

func (c *FakeClient) GetRealms() ([]string, error) {
	realms := make([]string, 0)
	for key, _ := range c.Users {
		realms = append(realms, key)
	}
	return realms, nil
}
