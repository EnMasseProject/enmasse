/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package project

import (
	"fmt"
	"strings"

	"github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	userv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func (p *projectCollector) collectMessagingUsers() error {

	log.Info("Collect MessagingUsers")

	opts := metav1.ListOptions{}

	list, err := p.client.UserV1beta1().
		MessagingUsers(p.namespace).
		List(opts)

	if err != nil {
		return err
	}

	mt := util.MultiTool{}

	for _, user := range list.Items {
		mt.Ran(p.checkMessagingUser(&user))
	}

	return mt.Error
}

func (p *projectCollector) checkMessagingUser(user *userv1alpha1.MessagingUser) error {
	log.Info("Checking address", "MessagingUser", user)

	found, notFound, err := p.findOwningProjects(user, true)
	if err != nil {
		return err
	}

	log.Info(fmt.Sprintf("IoTProject references - found: %v, notFound: %v, err: %v", len(found), len(notFound), err))

	if len(found) == 0 && len(notFound) == 0 {
		// no IoTProject owner references, we don't touch it
		return nil
	}

	if len(found) <= 0 && len(notFound) > 0 {
		// we were owned, but now everyone is gone
		return p.deleteUser(user)
	}

	for _, proj := range found {
		// we are still owned, but now check if we are still needed
		if p.needUser(user, &proj) {
			return nil
		}
	}

	return p.deleteUser(user)
}

// check if the project still needs this user, as the project owns the user, this function may assume
// that no one else is interested in this user
func (p *projectCollector) needUser(user *userv1alpha1.MessagingUser, project *v1alpha1.IoTProject) bool {

	if project.Spec.DownstreamStrategy.ManagedDownstreamStrategy == nil {
		// owning project is no longer of typed "managed"
		return false
	}

	toks := strings.Split(user.Name, ".")
	if len(toks) != 2 {
		// invalid user name format ... better delete this
		return false
	}

	if toks[0] != project.Spec.DownstreamStrategy.ManagedDownstreamStrategy.AddressSpace.Name {
		// address space name doesn't match user name ... as we own it, we delete it
		return false
	}

	// keep it … for now ;-)
	return true

}

func (p *projectCollector) deleteUser(user *userv1alpha1.MessagingUser) error {
	log.Info("Deleting Messaging User", "MessagingUser", user, "UID", user.UID)

	return p.client.UserV1beta1().
		MessagingUsers(user.Namespace).
		Delete(user.Name, &metav1.DeleteOptions{
			Preconditions: &metav1.Preconditions{UID: &user.UID},
		})
}
