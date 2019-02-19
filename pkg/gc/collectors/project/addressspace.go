/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package project

import (
	"fmt"

	corev1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func (p *projectCollector) collectAddressSpaces() error {

	log.Info("Collect AddressSpaces")

	opts := metav1.ListOptions{}

	list, err := p.client.EnmasseV1beta1().
		AddressSpaces(p.namespace).
		List(opts)

	if err != nil {
		return err
	}

	mt := util.MultiTool{}

	for _, as := range list.Items {
		mt.Ran(p.checkAddressSpace(&as))
	}

	return mt.Error
}

func (p *projectCollector) checkAddressSpace(as *corev1alpha1.AddressSpace) error {
	log.Info("Checking address space", "AddressSpace", as)

	// looking for all references to IoTProjects
	found, notFound, err := p.findOwningProjects(as, false)
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
		// this could be more than one, as we are not looking for controllers
		return p.deleteAddressSpace(as)
	}

	return nil
}

func (p *projectCollector) deleteAddressSpace(as *corev1alpha1.AddressSpace) error {
	log.Info("Deleting Address Space", "AddressSpace", as, "UID", as.UID)

	return p.client.EnmasseV1beta1().
		AddressSpaces(as.Namespace).
		Delete(as.Name, &metav1.DeleteOptions{
			Preconditions: &metav1.Preconditions{UID: &as.UID},
		})
}
