/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package keycloak

import (
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/url"
	"os"
	"reflect"
	"strings"
	"time"

	gocloak "github.com/Nerzal/gocloak/v3"

	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	logf "sigs.k8s.io/controller-runtime/pkg/log"
)

var log = logf.Log.WithName("keycloak")

type NewKeycloakClientFunc func(string, int, string, string, []byte) (KeycloakClient, error)
type AttributeFilter func(string, []string) bool

type KeycloakClient interface {
	CreateUser(realm string, user *userv1beta1.MessagingUser) error
	UpdateUser(realm string, existing *userv1beta1.MessagingUser, updated *userv1beta1.MessagingUser) error
	GetUser(realm string, username string) (*userv1beta1.MessagingUser, error)
	DeleteUser(realm string, user *userv1beta1.MessagingUser) error
	GetUsers(realm string, filters ...AttributeFilter) ([]*userv1beta1.MessagingUser, error)
	GetRealms() ([]string, error)
}

type keycloakClient struct {
	client      gocloak.GoCloak
	username    string
	password    string
	token       *gocloak.JWT
	tokenExpiry time.Time
}

var (
	_ KeycloakClient = &keycloakClient{}
)

const clusterCaPath = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"
const serviceCaPath = "/var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt"

func NewClient(hostname string, port int, username string, password string, caCert []byte) (KeycloakClient, error) {

	client := gocloak.NewClient(fmt.Sprintf("https://%s:%d", hostname, port))

	if _, err := os.Stat(clusterCaPath); err == nil {
		client.RestyClient().SetRootCertificate(clusterCaPath)
	}

	if _, err := os.Stat(serviceCaPath); err == nil {
		client.RestyClient().SetRootCertificate(serviceCaPath)
	}

	// client.RestyClient().SetDebug(true)

	if len(caCert) > 0 {
		tmpfile, err := ioutil.TempFile(os.TempDir(), "cacert*.pem")
		if err != nil {
			return nil, err
		}
		defer os.Remove(tmpfile.Name())

		_, err = tmpfile.Write(caCert)
		if err != nil {
			return nil, err
		}

		err = tmpfile.Close()
		if err != nil {
			return nil, err
		}

		client.RestyClient().SetRootCertificate(tmpfile.Name())
	}
	return &keycloakClient{
		client:      client,
		username:    username,
		password:    password,
		token:       nil,
		tokenExpiry: time.Time{},
	}, nil
}

func (c *keycloakClient) ensureLoggedIn() error {
	now := time.Now()
	if c.token == nil || c.tokenExpiry.Before(now) {
		token, err := c.client.LoginAdmin(c.username, c.password, "master")
		if err != nil {
			return err
		}
		c.token = token
		c.tokenExpiry = now.Add(time.Second * time.Duration(token.ExpiresIn-5))
	}
	return nil
}

func (c *keycloakClient) CreateUser(realm string, user *userv1beta1.MessagingUser) error {
	err := c.ensureLoggedIn()
	if err != nil {
		return err
	}
	if err != nil {
		return err
	}

	newUser, err := createUserRepresentation(user)
	if err != nil {
		return err
	}

	userId, err := c.client.CreateUser(c.token.AccessToken, realm, newUser)
	if err != nil {
		return err
	}
	log.V(4).Info("Created user", "username", newUser.Username, "userId", userId)

	err = c.applyAuthentication(realm, user.Spec.Authentication, userId)
	if err != nil {
		return err
	}

	err = c.applyAuthorization(realm, user.Spec.Authorization, userId)
	if err != nil {
		return err
	}
	return nil
}

const ATTR_RESOURCE_NAME = "resourceName"
const ATTR_RESOURCE_NAMESPACE = "resourceNamespace"
const ATTR_AUTHENTICATION_TYPE = "authenticationType"
const ATTR_ANNOTATIONS = "annotations"
const ATTR_OWNER_REFERENCES = "ownerReferences"
const ATTR_FROM_CRD = "fromCrd"

func createUserRepresentation(user *userv1beta1.MessagingUser) (gocloak.User, error) {
	attributes := make(map[string][]string)
	attributes[ATTR_FROM_CRD] = []string{"true"}
	attributes[ATTR_RESOURCE_NAME] = []string{user.Name}
	attributes[ATTR_RESOURCE_NAMESPACE] = []string{user.Namespace}
	attributes[ATTR_AUTHENTICATION_TYPE] = []string{string(user.Spec.Authentication.Type)}

	return gocloak.User{
		Username:   user.Spec.Username,
		Enabled:    true,
		Attributes: attributes,
	}, nil
}

func decodeBase64(in []byte) ([]byte, error) {
	out := make([]byte, base64.StdEncoding.DecodedLen(len(in)))
	_, err := base64.StdEncoding.Decode(out, in)
	return out, err
}

func (c *keycloakClient) applyAuthentication(realm string, authSpec userv1beta1.AuthenticationSpec, userId string) error {
	switch authSpec.Type {
	case userv1beta1.Password:
		if authSpec.Password != nil {
			log.V(4).Info("Setting password", "userId", userId)
			err := c.client.SetPassword(c.token.AccessToken, userId, realm, string(authSpec.Password), false)
			if err != nil {
				return err
			}
		}
	case userv1beta1.Federated:
		// TODO: Implement support in gocloak?
	case userv1beta1.ServiceAccount:
		// Nothing to do
	}
	return nil
}

func createDesiredGroups(authzSpec []userv1beta1.AuthorizationSpec) map[string]bool {
	desired := make(map[string]bool)
	for _, authzEntry := range authzSpec {
		for _, operation := range authzEntry.Operations {
			switch operation {
			case userv1beta1.Manage:
				desired["manage"] = true
			case userv1beta1.View:
				desired["monitor"] = true
			default:
				if authzEntry.Addresses != nil {
					for _, addressPattern := range authzEntry.Addresses {
						groupName := string(operation) + "_" + url.QueryEscape(addressPattern)
						desired[groupName] = true
					}
				}

			}
		}
	}
	return desired
}

func (c *keycloakClient) applyAuthorization(realm string, authzSpec []userv1beta1.AuthorizationSpec, userId string) error {
	var desiredGroups map[string]bool = createDesiredGroups(authzSpec)
	userGroups, err := c.client.GetUserGroups(c.token.AccessToken, realm, userId)
	if err != nil {
		return err
	}
	var existingGroups map[string]bool = make(map[string]bool)
	for _, userGroup := range userGroups {
		existingGroups[userGroup.Name] = true
	}

	allGroups, err := c.client.GetGroups(c.token.AccessToken, realm, gocloak.GetGroupsParams{})
	if err != nil {
		return err
	}

	if !reflect.DeepEqual(existingGroups, desiredGroups) {
		log.V(4).Info("Changing user groups", "userId", userId, "realm", realm, "old", existingGroups, "new", desiredGroups)

		for groupName := range existingGroups {
			if !desiredGroups[groupName] {
				groupId := findGroupId(groupName, allGroups)
				if groupId != nil {
					err = c.client.DeleteUserFromGroup(c.token.AccessToken, realm, userId, *groupId)
					if err != nil {
						return err
					}
				}

			}
		}

		for groupName := range desiredGroups {
			if !existingGroups[groupName] {
				groupId := findGroupId(groupName, allGroups)
				if groupId == nil {
					group := gocloak.Group{
						Name: groupName,
					}
					err = c.client.CreateGroup(c.token.AccessToken, realm, group)
				}
				// Re-read groups is necessary because CreateGroup API does not return response
				// TODO: Submit PR to fix gocloak API
				allGroups, err = c.client.GetGroups(c.token.AccessToken, realm, gocloak.GetGroupsParams{})
				if err != nil {
					return err
				}
				groupId = findGroupId(groupName, allGroups)
				if groupId != nil {
					err = c.client.AddUserToGroup(c.token.AccessToken, realm, userId, *groupId)
					if err != nil {
						return err
					}
				}
			}
		}
	}

	return nil
}

func findGroupId(groupName string, groups []*gocloak.Group) *string {
	for _, group := range groups {
		if group.Name == groupName {
			return &group.ID
		}
	}
	return nil
}

func (c *keycloakClient) GetUser(realm string, username string) (*userv1beta1.MessagingUser, error) {
	err := c.ensureLoggedIn()
	if err != nil {
		return nil, err
	}

	users, err := c.client.GetUsers(c.token.AccessToken, realm, gocloak.GetUsersParams{
		Search: username,
	})
	if err != nil {
		log.Error(err, "Error getting users")
		return nil, err
	}
	for _, kcUser := range users {
		if kcUser.Username == username {
			userGroups, err := c.client.GetUserGroups(c.token.AccessToken, realm, kcUser.ID)
			if err != nil {
				return nil, err
			}
			return userRepToMessagingUser(kcUser, userGroups)
		}
	}
	return nil, nil
}

func userRepToMessagingUser(user *gocloak.User, groups []*gocloak.UserGroup) (*userv1beta1.MessagingUser, error) {
	if user == nil {
		return nil, nil
	}

	resourceName, err := lookupFirstAttribute(ATTR_RESOURCE_NAME, user.Attributes)
	if err != nil {
		return nil, err
	}

	resourceNamespace, err := lookupFirstAttribute(ATTR_RESOURCE_NAMESPACE, user.Attributes)
	if err != nil {
		return nil, err
	}
	authenticationType, err := lookupFirstAttribute(ATTR_AUTHENTICATION_TYPE, user.Attributes)
	if err != nil {
		return nil, err
	}

	muser := &userv1beta1.MessagingUser{
		ObjectMeta: metav1.ObjectMeta{
			Name:      resourceName,
			Namespace: resourceNamespace,
		},
		Spec: userv1beta1.MessagingUserSpec{
			Username: user.Username,
		},
	}

	annotationList, ok := user.Attributes[ATTR_ANNOTATIONS]
	if ok {
		annotations, err := annotationsFromList(annotationList)
		if err != nil {
			return nil, err
		}
		muser.Annotations = annotations
	}

	ownerReferencesList, ok := user.Attributes[ATTR_OWNER_REFERENCES]
	if ok {
		ownerReferences, err := ownerReferencesFromList(ownerReferencesList)
		if err != nil {
			return nil, err
		}
		muser.OwnerReferences = ownerReferences
	}

	if authenticationType == "password" {
		muser.Spec.Authentication = userv1beta1.AuthenticationSpec{
			Type: userv1beta1.Password,
		}
	} else if authenticationType == "serviceaccount" {
		muser.Spec.Authentication = userv1beta1.AuthenticationSpec{
			Type: userv1beta1.ServiceAccount,
		}
	} else {
		return nil, errors.New("Unknown authentication type " + authenticationType)
	}

	sendAddresses := make([]string, 0)
	recvAddresses := make([]string, 0)
	globalOperations := make([]userv1beta1.AuthorizationOperation, 0)

	for _, group := range groups {
		if strings.HasPrefix(group.Name, "manage") {
			globalOperations = append(globalOperations, userv1beta1.Manage)
		} else if strings.HasPrefix(group.Name, "monitor") {
			globalOperations = append(globalOperations, userv1beta1.View)
		} else if strings.Contains(group.Name, "_") {
			parts := strings.SplitN(group.Name, "_", 2)
			operation, ok := userv1beta1.Operations[parts[0]]
			if !ok {
				return nil, errors.New("Unknown operation " + string(operation))
			}
			address, err := url.QueryUnescape(parts[1])
			if err != nil {
				return nil, err
			}

			if operation == userv1beta1.Send {
				sendAddresses = append(sendAddresses, address)
			} else if operation == userv1beta1.Recv {
				recvAddresses = append(recvAddresses, address)
			}
		}

	}

	muser.Spec.Authorization = make([]userv1beta1.AuthorizationSpec, 0)
	if len(globalOperations) > 0 {
		muser.Spec.Authorization = append(muser.Spec.Authorization,
			userv1beta1.AuthorizationSpec{
				Operations: globalOperations,
				Addresses:  make([]string, 0),
			})
	}

	if len(sendAddresses) > 0 {
		muser.Spec.Authorization = append(muser.Spec.Authorization, userv1beta1.AuthorizationSpec{
			Operations: []userv1beta1.AuthorizationOperation{userv1beta1.Send},
			Addresses:  sendAddresses,
		})
	}

	if len(recvAddresses) > 0 {
		muser.Spec.Authorization = append(muser.Spec.Authorization, userv1beta1.AuthorizationSpec{
			Operations: []userv1beta1.AuthorizationOperation{userv1beta1.Recv},
			Addresses:  recvAddresses,
		})
	}

	return muser, nil
}

func annotationsFromList(values []string) (map[string]string, error) {
	annotations := make(map[string]string, 0)
	for _, value := range values {
		entry := make(map[string]string, 0)
		err := json.Unmarshal([]byte(value), &entry)
		if err != nil {
			return nil, err
		}
		for key, val := range entry {
			annotations[key] = val
		}
	}
	return annotations, nil
}

func ownerReferencesFromList(values []string) ([]metav1.OwnerReference, error) {
	ownerReferences := make([]metav1.OwnerReference, 0)
	for _, value := range values {
		ownerReference := metav1.OwnerReference{}
		err := json.Unmarshal([]byte(value), &ownerReference)
		if err != nil {
			return nil, err
		}
		ownerReferences = append(ownerReferences, ownerReference)
	}
	return ownerReferences, nil
}

func lookupFirstAttribute(attrName string, attributes map[string][]string) (string, error) {
	lst, ok := attributes[attrName]
	if !ok {
		return "", errors.New("Unable to find user attribute " + attrName)
	}
	if len(lst) > 0 {
		return lst[0], nil
	} else {
		return "", errors.New("Not enough elements for user attribute " + attrName)
	}
}

func (c *keycloakClient) DeleteUser(realm string, user *userv1beta1.MessagingUser) error {
	err := c.ensureLoggedIn()
	if err != nil {
		return err
	}

	users, err := c.client.GetUsers(c.token.AccessToken, realm, gocloak.GetUsersParams{
		Search: user.Spec.Username,
	})
	if err != nil {
		log.Error(err, "Error getting users")
		return err
	}
	for _, kcUser := range users {
		if kcUser.Username == user.Spec.Username {
			return c.client.DeleteUser(c.token.AccessToken, realm, kcUser.ID)
		}
	}
	return nil
}

func (c *keycloakClient) UpdateUser(realm string, existing *userv1beta1.MessagingUser, updated *userv1beta1.MessagingUser) error {
	// If username changed, create new user and delete old
	err := c.ensureLoggedIn()
	if err != nil {
		return err
	}

	if existing.Spec.Username != updated.Spec.Username {
		err := c.CreateUser(realm, updated)
		if err != nil {
			return err
		}
		return c.DeleteUser(realm, existing)
	} else {
		users, err := c.client.GetUsers(c.token.AccessToken, realm, gocloak.GetUsersParams{
			Search: updated.Spec.Username,
		})
		if err != nil {
			log.Error(err, "Error getting users")
			return err
		}
		for _, kcUser := range users {
			if kcUser.Username == updated.Spec.Username {
				err = c.applyAuthentication(realm, updated.Spec.Authentication, kcUser.ID)
				if err != nil {
					log.Error(err, "applyAuthentication")
					return err
				}

				err = c.applyAuthorization(realm, updated.Spec.Authorization, kcUser.ID)
				if err != nil {
					log.Error(err, "applyAuthorization")
					return err
				}
			}
		}
	}
	return nil
}

func (c *keycloakClient) GetUsers(realm string, filters ...AttributeFilter) ([]*userv1beta1.MessagingUser, error) {
	err := c.ensureLoggedIn()
	if err != nil {
		return nil, err
	}
	users, err := c.client.GetUsers(c.token.AccessToken, realm, gocloak.GetUsersParams{})
	if err != nil {
		return nil, err
	}

	messagingUsers := make([]*userv1beta1.MessagingUser, 0)
	for _, kcUser := range users {
		userGroups, err := c.client.GetUserGroups(c.token.AccessToken, realm, kcUser.ID)
		if err != nil {
			return nil, err
		}

		passedFilter := true
		for _, filter := range filters {
			for attribute, attributeValues := range kcUser.Attributes {
				if !filter(attribute, attributeValues) {
					passedFilter = false
				}
			}
		}

		if passedFilter {
			messagingUser, err := userRepToMessagingUser(kcUser, userGroups)
			if err != nil {
				return nil, err
			}
			messagingUsers = append(messagingUsers, messagingUser)
		}
	}
	return messagingUsers, nil
}

func (c *keycloakClient) GetRealms() ([]string, error) {
	err := c.ensureLoggedIn()
	if err != nil {
		return nil, err
	}
	realms, err := c.client.GetRealms(c.token.AccessToken)
	if err != nil {
		return nil, err
	}

	realmNames := make([]string, 0)
	for _, realm := range realms {
		realmNames = append(realmNames, realm.Realm)
	}
	return realmNames, nil
}
