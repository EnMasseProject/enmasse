/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
)

type OverrideImageResolver struct {
	overrides []v1beta1.ImageOverride
}

func NewOverrideImageResolver(overrides []v1beta1.ImageOverride) OverrideImageResolver {
	return OverrideImageResolver{
		overrides,
	}
}

func matchesImageName(match *v1beta1.ByNameMatch, image ImageDefinition) bool {
	if match.Organization != "" && match.Organization != image.Organization {
		return false
	}

	if match.Name != "" && match.Name != image.Name {
		return false
	}

	return true
}

func MatchesImage(override v1beta1.ImageOverride, image ImageDefinition) bool {

	if override.Match.ByNameMatch != nil {
		return matchesImageName(override.Match.ByNameMatch, image)
	}

	return false

}

func ApplyImageOverride(override v1beta1.ImageOverride, image ImageDefinition) ImageDefinition {

	if override.Registry != "" {
		image.Registry = override.Registry
	}
	if override.Organization != "" {
		image.Organization = override.Organization
	}
	if override.Name != "" {
		image.Name = override.Name
	}
	if override.Tag != "" {
		image.Tag = override.Tag
	}
	if override.PullPolicy != "" {
		image.PullPolicy = override.PullPolicy
	}

	return image
}

func (r OverrideImageResolver) Resolve(request ImageRequest, image ImageDefinition) (ImageDefinition, error) {

	def := image

	for _, o := range r.overrides {
		if MatchesImage(o, def) {
			def = ApplyImageOverride(o, image)
		}
	}

	return def, nil
}

var _ ImageResolver = &OverrideImageResolver{}
