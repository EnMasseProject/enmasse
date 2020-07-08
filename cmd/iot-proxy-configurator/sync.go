/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"encoding/json"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/controller/iotconfig"
	"reflect"
	"strings"

	"github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/qdr"
	"github.com/enmasseproject/enmasse/pkg/util"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func (c *Configurator) syncResource(currentPointer interface{}, resource interface{}) (bool, error) {
	return c.syncResourceWithCreator(currentPointer, resource, toMapStringString)
}

func (c *Configurator) syncResourceWithCreator(currentPointer interface{}, resource interface{}, creator func(interface{}) (map[string]string, error)) (bool, error) {

	r, ok := resource.(qdr.RouterResource)

	if !ok {
		return false, fmt.Errorf("requested resource must implement 'qdr.RouterResource'")
	}

	found, err := c.manage.ReadAsObject(r, currentPointer)
	if err != nil {
		return false, err
	}

	log.V(4).Info("Found", "object", found)
	log.V(3).Info("Current", "object", currentPointer)
	log.V(3).Info("Request", "object", resource)

	if found {
		equals := reflect.DeepEqual(currentPointer, resource)
		log.V(2).Info("Resource equals", "equals", equals)
		if equals {
			return false, nil
		}
	}

	if found {
		err = c.manage.Delete(r)
		if err != nil {
			return false, err
		}
	}

	m, err := creator(resource)
	if err != nil {
		return false, err
	}

	_, err = c.manage.Create(r, m)

	return true, err

}

func (c *Configurator) syncLinkRoute(route qdr.LinkRoute) (bool, error) {
	return c.syncResource(&qdr.LinkRoute{}, &route)
}

func resourceName(object metav1.Object, name string) string {

	result := name + "-" + object.GetNamespace() + "-" + object.GetName()

	// NOTE: qdrouterd cannot properly handle "." and "/" in resource names

	return strings.
		NewReplacer(".", "-", "/", "-").
		Replace(result)
}

func namedResource(object metav1.Object, name string) qdr.NamedResource {
	return qdr.NamedResource{
		Name: resourceName(object, name),
	}
}

// Convert an object to a map of string/string
func toMapStringString(v interface{}) (map[string]string, error) {

	out, err := json.Marshal(v)
	if err != nil {
		return nil, err
	}

	var f interface{}

	err = json.Unmarshal(out, &f)
	if err != nil {
		return nil, err
	}

	s := f.(map[string]interface{})

	result := map[string]string{}

	for k, v := range s {
		switch t := v.(type) {
		case string:
			result[k] = t
		}
	}

	return result, nil
}

type RouteConfig struct {
	Tag       string
	Direction string
	Prefix    string
}

var routes = []RouteConfig{
	{"t", "in", "telemetry"},
	{"e", "in", "event"},
	{"c_i", "in", "command_response"},
	{"c_o", "out", "command"},
}

func (c *Configurator) syncProject(project *v1alpha1.IoTProject) (bool, error) {

	log.Info("Sync project", "project", project)

	m := util.MultiTool{}

	for _, r := range routes {
		m.RunChange(func() (b bool, e error) {
			return c.syncLinkRoute(qdr.LinkRoute{
				NamedResource:     namedResource(project, "linkRoute/"+r.Tag),
				Direction:         r.Direction,
				Pattern:           util.AddressName(project, r.Prefix) + "/#",
				Connection:        iotconfig.SharedInfraConnectionName,
				AddExternalPrefix: project.Status.MessagingInfrastructurePrefix + "/",
			})
		})
	}

	return m.Return()
}

func (c *Configurator) deleteProject(object metav1.Object) error {

	log.Info("Delete project", "project", object)

	m := util.MultiTool{}

	for _, r := range routes {
		m.RunChange(func() (b bool, e error) {
			return true, c.manage.Delete(qdr.NamedLinkRoute(resourceName(object, "linkRoute/"+r.Tag)))
		})
	}

	return m.Error
}
