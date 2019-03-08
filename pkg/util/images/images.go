/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"fmt"

	"k8s.io/api/core/v1"
)

type ImageRequest struct {
	Organization string
	Name         string
}

type ImageDefinition struct {
	Registry     string
	Organization string
	Name         string
	Tag          string
	PullPolicy   v1.PullPolicy
}

func (img ImageDefinition) IsComplete() bool {
	if img.Organization == "" || img.Name == "" {
		return false
	}

	return true
}

type ImageResolver interface {
	Resolve(request ImageRequest, image ImageDefinition) (ImageDefinition, error)
}

func runResolvers(request ImageRequest, image ImageDefinition, resolvers []ImageResolver) (ImageDefinition, error) {

	def := image
	var err error

	for _, r := range resolvers {
		def, err = r.Resolve(request, def)
		if err != nil {
			return def, err
		}
	}

	return def, nil
}

func (img ImageRequest) Resolve(resolvers []ImageResolver) (ImageDefinition, error) {

	def := ImageDefinition{
		Organization: img.Organization,
		Name:         img.Name,
	}

	def, err := runResolvers(img, def, resolvers)

	if err != nil {
		return def, err
	}

	if !def.IsComplete() {
		return def, fmt.Errorf("unable to resolve image: %v", def)
	}

	return def, nil
}

func (img ImageDefinition) ToImageStreamTag() (string, error) {
	if img.Name == "" {
		return "", fmt.Errorf("name must not be empty")
	}
	if img.Tag == "" {
		return "", fmt.Errorf("tag must not be empty")
	}

	return img.Name + ": " + img.Tag, nil
}

func (img ImageDefinition) ToImageName() (string, error) {
	if img.Name == "" {
		return "", fmt.Errorf("name must not be empty")
	}
	if img.Organization == "" {
		return "", fmt.Errorf("organization must not be empty")
	}
	if img.Tag == "" {
		return "", fmt.Errorf("tag must not be empty")
	}

	s := ""

	if img.Registry != "" {
		s = img.Registry + "/"
	}

	s = s + img.Organization + "/" + img.Name + ":" + img.Tag

	return s, nil
}
