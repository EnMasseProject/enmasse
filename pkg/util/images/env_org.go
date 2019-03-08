/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"strings"

	"github.com/enmasseproject/enmasse/pkg/util"
	"k8s.io/api/core/v1"
)

type organizationEnvironmentImageResolver struct {
	provider util.EnvironmentProvider
}

const envOrgPrefix = "ENMASSE_IMGORG_"

func (r organizationEnvironmentImageResolver) Resolve(request ImageRequest, image ImageDefinition) (ImageDefinition, error) {

	name := nameReplacer.ReplaceAllString(strings.ToUpper(request.Organization), "_")

	registry := r.provider.Get(envOrgPrefix + name + "_REGISTRY")
	if registry == "" {
		registry = image.Registry
	}
	org := r.provider.Get(envOrgPrefix + name + "_ORG")
	if org == "" {
		org = image.Organization
	}
	tag := r.provider.Get(envOrgPrefix + name + "_TAG")
	if tag == "" {
		tag = image.Tag
	}
	pull := r.provider.Get(envOrgPrefix + name + "_PULL_POLICY")
	if pull == "" {
		pull = string(image.PullPolicy)
	}

	return ImageDefinition{registry, org, image.Name, tag, v1.PullPolicy(pull)}, nil
}

var _ ImageResolver = &nameEnvironmentImageResolver{}
