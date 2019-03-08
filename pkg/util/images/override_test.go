/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"testing"

	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
)

func mockTestEnvAndOverride(env map[string]string, overrides []v1beta1.ImageOverride) []ImageResolver {
	return []ImageResolver{
		MockEnvironmentResolver(env),
		OverrideImageResolver{overrides: overrides},
	}
}

func TestImageOverride(t *testing.T) {
	for _, i := range []ImageRequestTestEntry{

		{
			// no overrides present, shouldn't make a difference
			resolvers: mockTestEnvAndOverride(
				map[string]string{
					"ENMASSE_DEFAULT_IMG_TAG": "1.2.3",
				},
				[]v1beta1.ImageOverride{},
			),
			request: ImageRequest{
				Organization: "enmasseproject",
				Name:         "foo-bar",
			},
			resultName:       "enmasseproject/foo-bar:1.2.3",
			resultPullPolicy: "",
			resultError:      false,
		},

		{
			// matches env vars, but and override
			resolvers: mockTestEnvAndOverride(
				map[string]string{
					"ENMASSE_DEFAULT_IMG_TAG": "1.2.3",
				},
				[]v1beta1.ImageOverride{
					v1beta1.ImageOverride{
						Match: v1beta1.ImageMatch{
							ByNameMatch: &v1beta1.ByNameMatch{
								Organization: "enmasseproject",
								Name:         "foo-bar",
							},
						},
						Tag: "foo-123",
					},
				},
			),
			request: ImageRequest{
				Organization: "enmasseproject",
				Name:         "foo-bar",
			},
			resultName:       "enmasseproject/foo-bar:foo-123",
			resultPullPolicy: "",
			resultError:      false,
		},

		{
			// matches env vars, but not override
			resolvers: mockTestEnvAndOverride(
				map[string]string{
					"ENMASSE_DEFAULT_IMG_TAG": "1.2.3",
				},
				[]v1beta1.ImageOverride{
					v1beta1.ImageOverride{
						Match: v1beta1.ImageMatch{
							ByNameMatch: &v1beta1.ByNameMatch{
								Organization: "enmasseproject2", // <- doesn't match "enmasseproject"
								Name:         "foo-bar",
							},
						},
						Tag: "foo-123",
					},
				},
			),
			request: ImageRequest{
				Organization: "enmasseproject",
				Name:         "foo-bar",
			},
			resultName:       "enmasseproject/foo-bar:1.2.3",
			resultPullPolicy: "",
			resultError:      false,
		},
	} {
		Eval(t, i)
	}
}
