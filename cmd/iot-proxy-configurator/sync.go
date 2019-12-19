/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"path"
	"path/filepath"
	"reflect"
	"strconv"
	"strings"

	"github.com/google/uuid"

	"github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/qdr"
	"github.com/enmasseproject/enmasse/pkg/util"
	"go.uber.org/multierr"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

var (
	nameIdNamespace = uuid.MustParse("11db97c2-1d87-11ea-976f-c85b762e5a2c")
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

func (c *Configurator) syncConnector(connector qdr.Connector) (bool, error) {
	return c.syncResource(&qdr.Connector{}, &connector)
}

func fileExists(file string) bool {
	if _, err := os.Stat(file); err != nil {
		return false
	}
	return true
}

func certPathAndPrefix(object metav1.Object) (string, string) {
	name := object.GetNamespace() + "." + object.GetName()

	nameId := uuid.NewSHA1(nameIdNamespace, []byte(name)).String()
	l1 := nameId[0:2] // we know it is ASCII only
	l2 := nameId[0:4] // we know it is ASCII only

	return filepath.Join(l1, l2), name
}

func (c *Configurator) certificatePath(object metav1.Object, certificate []byte) string {

	if certificate == nil || len(certificate) == 0 {
		return ""
	}

	suffix := fmt.Sprintf("-%x-cert.crt", sha256.Sum256(certificate))
	dir, prefix := certPathAndPrefix(object)
	return path.Join(c.ephemeralCertBase, dir, prefix+suffix)

}

func (c *Configurator) deleteCertificatesForProject(object metav1.Object) error {
	dir, prefix := certPathAndPrefix(object)

	p := path.Join(c.ephemeralCertBase, dir)
	log.V(2).Info("Scanning base directory", "base", p)

	files, err := ioutil.ReadDir(p)
	if err != nil {
		if os.IsNotExist(err) {
			return nil
		}
		log.Error(err, "Failed to read directory", "path", p)
		return err
	}

	log.Info("Cleaning up certificates for", "object", object)

	for _, f := range files {
		log.V(2).Info("Checking file", "file", f.Name())
		if strings.HasPrefix(f.Name(), prefix) {
			log.Info("Deleting file", "file", f.Name())
			removeErr := os.Remove(filepath.Join(c.ephemeralCertBase, f.Name()))
			if !os.IsNotExist(removeErr) {
				err = multierr.Append(err, removeErr)
			}
		}
	}

	return err
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

func (c *Configurator) syncSslProfile(object metav1.Object, certificate []byte) (bool, error) {

	hasCert := certificate != nil && len(certificate) > 0
	if hasCert && c.ephemeralCertBase == "" {
		return false, fmt.Errorf("unable to configure custom certificate, ephemeral base directory is not configured")
	}

	certFile := c.certificatePath(object, certificate)
	log.V(2).Info("Certificate path", "path", certFile)

	if !hasCert && c.ephemeralCertBase != "" {

		// delete all certificates for this project
		if err := c.deleteCertificatesForProject(object); err != nil {
			return false, err
		}

	} else if hasCert && !fileExists(certFile) {

		// delete all certificates for this project
		if err := c.deleteCertificatesForProject(object); err != nil {
			return false, err
		}

		if err := os.MkdirAll(filepath.Dir(certFile), 0777); err != nil {
			return false, err
		}

		// cert file currently does not exists, write to file system
		if err := ioutil.WriteFile(certFile, certificate, 0666); err != nil {
			return false, err
		}
	}

	// sync with qdr

	return c.syncResource(&qdr.SslProfile{}, &qdr.SslProfile{
		NamedResource:     namedResource(object, "sslProfile"),
		CACertificatePath: certFile,
	})
}

type RouteConfig struct {
	Tag       string
	Direction string
	Prefix    string
}

var routes = []RouteConfig{
	{"t", "in", "telemetry"},
	{"e", "in", "event"},
	{"cl_i", "in", "control"},
	{"cl_o", "out", "control"},
	{"c_i", "in", "command_response"},
	{"c_o", "out", "command"},
}

func (c *Configurator) syncProject(project *v1alpha1.IoTProject) (bool, error) {

	endpoint := project.Status.DownstreamEndpoint

	connectorName := resourceName(project, "connector")
	sslProfileName := ""

	log.Info("Sync project", "project", project)

	m := util.MultiTool{}

	if endpoint.TLS {
		m.RunChange(func() (b bool, e error) {
			return c.syncSslProfile(project, endpoint.Certificate)
		})
		sslProfileName = resourceName(project, "sslProfile")
	}

	m.RunChange(func() (b bool, e error) {
		return c.syncConnector(qdr.Connector{
			NamedResource: qdr.NamedResource{Name: connectorName},
			Host:          endpoint.Host,
			Port:          strconv.Itoa(int(endpoint.Port)),
			Role:          "route-container",
			SASLUsername:  endpoint.Username,
			SASLPassword:  endpoint.Password,
			SSLProfile:    sslProfileName,
		})
	})

	for _, r := range routes {
		m.RunChange(func() (b bool, e error) {
			return c.syncLinkRoute(qdr.LinkRoute{
				NamedResource: namedResource(project, "linkRoute/"+r.Tag),
				Direction:     r.Direction,
				Pattern:       util.AddressName(project, r.Prefix) + "/#",
				Connection:    connectorName,
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

	m.RunChange(func() (b bool, e error) {
		return true, c.manage.Delete(qdr.NamedConnector(resourceName(object, "connector")))
	})

	m.RunChange(func() (b bool, e error) {
		return true, c.manage.Delete(qdr.NamedSslProfile(resourceName(object, "sslProfile")))
	})

	m.RunChange(func() (b bool, e error) {
		return true, c.deleteCertificatesForProject(object)
	})

	return m.Error
}
