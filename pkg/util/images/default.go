/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
)

func defaultResolvers(envProvider util.EnvironmentProvider, overrides []v1beta1.ImageOverride) []ImageResolver {
	return []ImageResolver{
		defaultEnvironmentResolver(envProvider),
		OverrideImageResolver{overrides: overrides},
		EvalPullPolicyImageResolver{},
	}
}

func DefaultResolvers(overrides []v1beta1.ImageOverride) []ImageResolver {
	return defaultResolvers(util.OSEnvironmentProvider{}, overrides)
}

func DefaultMockResolvers(env map[string]string, overrides []v1beta1.ImageOverride) []ImageResolver {
	return defaultResolvers(util.MockEnvironmentProvider{Environment: env}, overrides)
}
