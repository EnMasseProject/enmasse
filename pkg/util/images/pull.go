/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"strings"

	corev1 "k8s.io/api/core/v1"
)

// Replace pull policy based on "-SNAPSHOT" or "latest" tag value.
type EvalPullPolicyImageResolver struct {
}

var _ ImageResolver = &EvalPullPolicyImageResolver{}

func (r EvalPullPolicyImageResolver) Resolve(request ImageRequest, image ImageDefinition) (ImageDefinition, error) {

	tag := image.Tag

	if tag == "" || image.PullPolicy != "" {
		return image, nil
	}

	image.PullPolicy = PullPolicyFromTag(tag)

	return image, nil
}

func PullPolicyFromTag(tag string) corev1.PullPolicy {

	if strings.HasSuffix(tag, "-SNAPSHOT") || tag == "latest" {
		return corev1.PullAlways
	} else {
		return corev1.PullIfNotPresent
	}

}
