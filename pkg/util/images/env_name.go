/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"github.com/enmasseproject/enmasse/pkg/util"
	"k8s.io/api/core/v1"
)

type nameEnvironmentImageResolver struct {
	provider util.EnvironmentProvider
}

const envPrefix = "ENMASSE_IMG_"

func (r nameEnvironmentImageResolver) Resolve(request ImageRequest, image ImageDefinition) (ImageDefinition, error) {

	name := nameToEnvName(request)

	registry := r.provider.Get(envPrefix + name + "_REGISTRY")
	if registry == "" {
		registry = image.Registry
	}
	org := r.provider.Get(envPrefix + name + "_ORG")
	if org == "" {
		org = image.Organization
	}
	tag := r.provider.Get(envPrefix + name + "_TAG")
	if tag == "" {
		tag = image.Tag
	}
	pull := r.provider.Get(envPrefix + name + "_PULL_POLICY")
	if pull == "" {
		pull = string(image.PullPolicy)
	}
	newname := r.provider.Get(envPrefix + name + "_NAME")
	if newname == "" {
		newname = image.Name
	}

	return ImageDefinition{registry, org, newname, tag, v1.PullPolicy(pull)}, nil
}

var _ ImageResolver = &nameEnvironmentImageResolver{}
