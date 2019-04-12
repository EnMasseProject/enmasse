/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package qdr

import (
	json2 "encoding/json"

	"os"
	"os/exec"
	"strings"

	"k8s.io/klog"
)

const DefaultCommand = "/usr/bin/qdmanage"

type ResourceNotFoundError struct {
}

func (e *ResourceNotFoundError) Error() string { return "Resource not found" }

var _ error = &ResourceNotFoundError{}

func IsNotFound(err error) bool {
	switch err.(type) {
	case *ResourceNotFoundError:
		return true
	}
	return false
}

type Manage struct {
	URL     string
	Command string
}

func NewManage() *Manage {
	return &Manage{URL: "", Command: DefaultCommand}
}

func NewManageWithUrl(URL string) *Manage {
	return &Manage{URL: URL, Command: DefaultCommand}
}

// Call a manage operation
func (m *Manage) Manage(operation string, attributes map[string]string) (string, error) {

	var args []string
	if m.URL != "" {
		args = append(args, "-b", m.URL)
	}

	args = append(args, operation)

	for k, v := range attributes {
		args = append(args, k+"="+v)
	}

	klog.V(1).Infof("Call with args: %v - %v", m.Command, args)

	cmd := exec.Command(m.Command, args...)
	cmd.Stderr = os.Stderr

	out, err := cmd.Output()
	text := string(out)

	klog.V(2).Infof("Command output: %v", text)

	if _, ok := err.(*exec.ExitError); ok {
		if strings.HasPrefix(text, "NotFoundStatus:") {
			return "", &ResourceNotFoundError{}
		} else {
			return "", err
		}
	}

	if err != nil {
		klog.V(2).Infof("Result: %v", err.Error())
		return "", err
	} else {
		return string(text), nil
	}
}

// Get a resource, returns the JSON of the resource, or and empty string if the object does not exists
func (m *Manage) Read(routerResource RouterResource) (string, error) {
	out, err := m.Manage("read", map[string]string{
		"--type": routerResource.GetType(),
		"--name": routerResource.GetName(),
	})
	if err != nil {
		if _, ok := err.(*ResourceNotFoundError); ok {
			return "", nil
		} else {
			return "", err
		}
	} else {
		return out, nil
	}
}

// Read an object
//
// Return `true` if the object was found, `false` otherwise
func (m *Manage) ReadAsObject(routerResource RouterResource, v interface{}) (bool, error) {
	json, err := m.Read(routerResource)
	if err != nil {
		return false, err // failure
	} else {
		if json == "" {
			return false, nil // success: not found
		}
		err := json2.Unmarshal([]byte(json), &v)
		if err != nil {
			return false, err // failure
		} else {
			return true, nil // success: found
		}
	}
}

func (m *Manage) Exists(routerResource RouterResource) (bool, error) {
	str, err := m.Read(routerResource)

	if err != nil {
		return false, err
	} else {
		return str != "", nil
	}
}

func (m *Manage) Create(routerResource RouterResource, attributes map[string]string) (string, error) {

	arguments := map[string]string{
		"--name": routerResource.GetName(),
		"--type": routerResource.GetType(),
	}

	for k, v := range attributes {
		arguments[k] = v
	}

	return m.Manage("create", arguments)
}

func (m *Manage) Delete(routerResource RouterResource) error {

	_, err := m.Manage("delete", map[string]string{
		"--name": routerResource.GetName(),
		"--type": routerResource.GetType(),
	})

	if IsNotFound(err) {
		// we want to delete, and it was gone already
		return nil
	}

	return err
}
