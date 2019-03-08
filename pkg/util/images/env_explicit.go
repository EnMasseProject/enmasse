/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"fmt"

	"github.com/enmasseproject/enmasse/pkg/util"
	"k8s.io/api/core/v1"
)

type explicitEnvironmentImageResolver struct {
	provider util.EnvironmentProvider
}

func (r explicitEnvironmentImageResolver) Resolve(request ImageRequest, image ImageDefinition) (ImageDefinition, error) {

	name := nameToEnvName(request)

	envName := "ENMASSE_IMG_" + name
	fullImage := r.provider.Get(envName)
	if fullImage == "" {
		return image, nil
	}

	match := imagePattern.FindStringSubmatch(fullImage)

	if match == nil {
		return ImageDefinition{}, fmt.Errorf("invalid image format: %v in %v", fullImage, envName)
	}

	return ImageDefinition{match[2], match[3], match[4], match[5], v1.PullPolicy("")}, nil
}

var _ ImageResolver = &explicitEnvironmentImageResolver{}
