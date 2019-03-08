/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"github.com/enmasseproject/enmasse/pkg/util"
	"k8s.io/api/core/v1"
)

type defaultEnvironmentImageResolver struct {
	provider util.EnvironmentProvider
}

func (r defaultEnvironmentImageResolver) Resolve(request ImageRequest, image ImageDefinition) (ImageDefinition, error) {

	registry := r.provider.Get("ENMASSE_DEFAULT_IMG_REGISTRY")
	tag := r.provider.Get("ENMASSE_DEFAULT_IMG_TAG")
	pull := r.provider.Get("ENMASSE_DEFAULT_IMG_PULL_POLICY")

	return ImageDefinition{registry, image.Organization, image.Name, tag, v1.PullPolicy(pull)}, nil
}

var _ ImageResolver = &defaultEnvironmentImageResolver{}
