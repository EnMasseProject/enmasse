/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package project

import (
	"fmt"

	"github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// Takes the owner references of `obj` of type `IoTProject` and tries to resolved those into `IoTProject` instances.
// returns `error` in case anything goes wrong
// returns `found`, an array of resolved projects
// returns `notFound` an array of unresolved references of kind `IoTProject`
func (p *projectCollector) findOwningProjects(obj metav1.Object, controller bool) (found []v1alpha1.IoTProject, notFound []metav1.OwnerReference, err error) {

	found = make([]v1alpha1.IoTProject, 0)
	notFound = make([]metav1.OwnerReference, 0)

	for _, ref := range obj.GetOwnerReferences() {
		if ref.Kind != "IoTProject" {
			continue
		}
		if controller && (ref.Controller != nil && !*ref.Controller) {
			continue
		}
		project, err := p.loadOwnerProject(obj.GetNamespace(), &ref)
		if err != nil {
			return nil, nil, err
		}
		if project == nil {
			// project no longer found
			notFound = append(notFound, ref)
		} else {
			found = append(found, *project)
		}
	}

	return
}

// find the owner project of a resource
func (p *projectCollector) loadOwnerProject(namespace string, owner *metav1.OwnerReference) (*v1alpha1.IoTProject, error) {

	if owner.Kind != "IoTProject" {
		return nil, fmt.Errorf("can only load Kind IoTProject, was: %s", owner.Kind)
	}

	project, err := p.client.IotV1alpha1().
		IoTProjects(namespace).
		Get(owner.Name, metav1.GetOptions{})

	if err != nil {
		if errors.IsNotFound(err) {
			return nil, nil
		}
		return nil, err
	}

	if project.ObjectMeta.UID != owner.UID {
		return nil, nil
	}

	return project, nil
}
