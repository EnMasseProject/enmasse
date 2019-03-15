/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package install

import (
	"os"

	"github.com/enmasseproject/enmasse/pkg/util"

	corev1 "k8s.io/api/core/v1"

	"github.com/enmasseproject/enmasse/version"

	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
)

var (
	// The default tag used for images
	defaultTag string
	// The default repository used for images
	defaultRepository string
	// The default setting of using image streams
	defaultUseImageStreams bool
	// The default pull policy, might be empty
	defaultPullPolicy corev1.PullPolicy
)

var log = logf.Log.WithName("install")

func init() {
	defaultTag = os.Getenv("ENMASSE_DEFAULT_TAG")
	if defaultTag == "" {
		defaultTag = version.Version
	}

	defaultUseImageStreams = util.GetBooleanEnv("ENMASSE_DEFAULT_USE_IMAGE_STREAMS")

	defaultRepository = os.Getenv("ENMASSE_DEFAULT_REPOSITORY")
	if defaultRepository == "" {
		defaultRepository = "unset.container.repository"
	}

	defaultPullPolicy = corev1.PullPolicy(os.Getenv("ENMASSE_DEFAULT_PULL_POLICY"))
}
