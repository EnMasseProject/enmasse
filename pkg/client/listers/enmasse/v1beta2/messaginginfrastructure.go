/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

// Code generated by lister-gen. DO NOT EDIT.

package v1beta2

import (
	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/client-go/tools/cache"
)

// MessagingInfrastructureLister helps list MessagingInfrastructures.
type MessagingInfrastructureLister interface {
	// List lists all MessagingInfrastructures in the indexer.
	List(selector labels.Selector) (ret []*v1beta2.MessagingInfrastructure, err error)
	// MessagingInfrastructures returns an object that can list and get MessagingInfrastructures.
	MessagingInfrastructures(namespace string) MessagingInfrastructureNamespaceLister
	MessagingInfrastructureListerExpansion
}

// messagingInfrastructureLister implements the MessagingInfrastructureLister interface.
type messagingInfrastructureLister struct {
	indexer cache.Indexer
}

// NewMessagingInfrastructureLister returns a new MessagingInfrastructureLister.
func NewMessagingInfrastructureLister(indexer cache.Indexer) MessagingInfrastructureLister {
	return &messagingInfrastructureLister{indexer: indexer}
}

// List lists all MessagingInfrastructures in the indexer.
func (s *messagingInfrastructureLister) List(selector labels.Selector) (ret []*v1beta2.MessagingInfrastructure, err error) {
	err = cache.ListAll(s.indexer, selector, func(m interface{}) {
		ret = append(ret, m.(*v1beta2.MessagingInfrastructure))
	})
	return ret, err
}

// MessagingInfrastructures returns an object that can list and get MessagingInfrastructures.
func (s *messagingInfrastructureLister) MessagingInfrastructures(namespace string) MessagingInfrastructureNamespaceLister {
	return messagingInfrastructureNamespaceLister{indexer: s.indexer, namespace: namespace}
}

// MessagingInfrastructureNamespaceLister helps list and get MessagingInfrastructures.
type MessagingInfrastructureNamespaceLister interface {
	// List lists all MessagingInfrastructures in the indexer for a given namespace.
	List(selector labels.Selector) (ret []*v1beta2.MessagingInfrastructure, err error)
	// Get retrieves the MessagingInfrastructure from the indexer for a given namespace and name.
	Get(name string) (*v1beta2.MessagingInfrastructure, error)
	MessagingInfrastructureNamespaceListerExpansion
}

// messagingInfrastructureNamespaceLister implements the MessagingInfrastructureNamespaceLister
// interface.
type messagingInfrastructureNamespaceLister struct {
	indexer   cache.Indexer
	namespace string
}

// List lists all MessagingInfrastructures in the indexer for a given namespace.
func (s messagingInfrastructureNamespaceLister) List(selector labels.Selector) (ret []*v1beta2.MessagingInfrastructure, err error) {
	err = cache.ListAllByNamespace(s.indexer, s.namespace, selector, func(m interface{}) {
		ret = append(ret, m.(*v1beta2.MessagingInfrastructure))
	})
	return ret, err
}

// Get retrieves the MessagingInfrastructure from the indexer for a given namespace and name.
func (s messagingInfrastructureNamespaceLister) Get(name string) (*v1beta2.MessagingInfrastructure, error) {
	obj, exists, err := s.indexer.GetByKey(s.namespace + "/" + name)
	if err != nil {
		return nil, err
	}
	if !exists {
		return nil, errors.NewNotFound(v1beta2.Resource("messaginginfrastructure"), name)
	}
	return obj.(*v1beta2.MessagingInfrastructure), nil
}
