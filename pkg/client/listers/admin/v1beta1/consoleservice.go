/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

// Code generated by lister-gen. DO NOT EDIT.

package v1beta1

import (
	v1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/client-go/tools/cache"
)

// ConsoleServiceLister helps list ConsoleServices.
type ConsoleServiceLister interface {
	// List lists all ConsoleServices in the indexer.
	List(selector labels.Selector) (ret []*v1beta1.ConsoleService, err error)
	// ConsoleServices returns an object that can list and get ConsoleServices.
	ConsoleServices(namespace string) ConsoleServiceNamespaceLister
	ConsoleServiceListerExpansion
}

// consoleServiceLister implements the ConsoleServiceLister interface.
type consoleServiceLister struct {
	indexer cache.Indexer
}

// NewConsoleServiceLister returns a new ConsoleServiceLister.
func NewConsoleServiceLister(indexer cache.Indexer) ConsoleServiceLister {
	return &consoleServiceLister{indexer: indexer}
}

// List lists all ConsoleServices in the indexer.
func (s *consoleServiceLister) List(selector labels.Selector) (ret []*v1beta1.ConsoleService, err error) {
	err = cache.ListAll(s.indexer, selector, func(m interface{}) {
		ret = append(ret, m.(*v1beta1.ConsoleService))
	})
	return ret, err
}

// ConsoleServices returns an object that can list and get ConsoleServices.
func (s *consoleServiceLister) ConsoleServices(namespace string) ConsoleServiceNamespaceLister {
	return consoleServiceNamespaceLister{indexer: s.indexer, namespace: namespace}
}

// ConsoleServiceNamespaceLister helps list and get ConsoleServices.
type ConsoleServiceNamespaceLister interface {
	// List lists all ConsoleServices in the indexer for a given namespace.
	List(selector labels.Selector) (ret []*v1beta1.ConsoleService, err error)
	// Get retrieves the ConsoleService from the indexer for a given namespace and name.
	Get(name string) (*v1beta1.ConsoleService, error)
	ConsoleServiceNamespaceListerExpansion
}

// consoleServiceNamespaceLister implements the ConsoleServiceNamespaceLister
// interface.
type consoleServiceNamespaceLister struct {
	indexer   cache.Indexer
	namespace string
}

// List lists all ConsoleServices in the indexer for a given namespace.
func (s consoleServiceNamespaceLister) List(selector labels.Selector) (ret []*v1beta1.ConsoleService, err error) {
	err = cache.ListAllByNamespace(s.indexer, s.namespace, selector, func(m interface{}) {
		ret = append(ret, m.(*v1beta1.ConsoleService))
	})
	return ret, err
}

// Get retrieves the ConsoleService from the indexer for a given namespace and name.
func (s consoleServiceNamespaceLister) Get(name string) (*v1beta1.ConsoleService, error) {
	obj, exists, err := s.indexer.GetByKey(s.namespace + "/" + name)
	if err != nil {
		return nil, err
	}
	if !exists {
		return nil, errors.NewNotFound(v1beta1.Resource("consoleservice"), name)
	}
	return obj.(*v1beta1.ConsoleService), nil
}
