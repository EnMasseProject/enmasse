/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

// Code generated by lister-gen. DO NOT EDIT.

package v1

import (
	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/client-go/tools/cache"
)

// MessagingConsoleLister helps list MessagingConsoles.
type MessagingConsoleLister interface {
	// List lists all MessagingConsoles in the indexer.
	List(selector labels.Selector) (ret []*v1.MessagingConsole, err error)
	// MessagingConsoles returns an object that can list and get MessagingConsoles.
	MessagingConsoles(namespace string) MessagingConsoleNamespaceLister
	MessagingConsoleListerExpansion
}

// messagingConsoleLister implements the MessagingConsoleLister interface.
type messagingConsoleLister struct {
	indexer cache.Indexer
}

// NewMessagingConsoleLister returns a new MessagingConsoleLister.
func NewMessagingConsoleLister(indexer cache.Indexer) MessagingConsoleLister {
	return &messagingConsoleLister{indexer: indexer}
}

// List lists all MessagingConsoles in the indexer.
func (s *messagingConsoleLister) List(selector labels.Selector) (ret []*v1.MessagingConsole, err error) {
	err = cache.ListAll(s.indexer, selector, func(m interface{}) {
		ret = append(ret, m.(*v1.MessagingConsole))
	})
	return ret, err
}

// MessagingConsoles returns an object that can list and get MessagingConsoles.
func (s *messagingConsoleLister) MessagingConsoles(namespace string) MessagingConsoleNamespaceLister {
	return messagingConsoleNamespaceLister{indexer: s.indexer, namespace: namespace}
}

// MessagingConsoleNamespaceLister helps list and get MessagingConsoles.
type MessagingConsoleNamespaceLister interface {
	// List lists all MessagingConsoles in the indexer for a given namespace.
	List(selector labels.Selector) (ret []*v1.MessagingConsole, err error)
	// Get retrieves the MessagingConsole from the indexer for a given namespace and name.
	Get(name string) (*v1.MessagingConsole, error)
	MessagingConsoleNamespaceListerExpansion
}

// messagingConsoleNamespaceLister implements the MessagingConsoleNamespaceLister
// interface.
type messagingConsoleNamespaceLister struct {
	indexer   cache.Indexer
	namespace string
}

// List lists all MessagingConsoles in the indexer for a given namespace.
func (s messagingConsoleNamespaceLister) List(selector labels.Selector) (ret []*v1.MessagingConsole, err error) {
	err = cache.ListAllByNamespace(s.indexer, s.namespace, selector, func(m interface{}) {
		ret = append(ret, m.(*v1.MessagingConsole))
	})
	return ret, err
}

// Get retrieves the MessagingConsole from the indexer for a given namespace and name.
func (s messagingConsoleNamespaceLister) Get(name string) (*v1.MessagingConsole, error) {
	obj, exists, err := s.indexer.GetByKey(s.namespace + "/" + name)
	if err != nil {
		return nil, err
	}
	if !exists {
		return nil, errors.NewNotFound(v1.Resource("messagingconsole"), name)
	}
	return obj.(*v1.MessagingConsole), nil
}
