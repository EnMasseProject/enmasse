/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"testing"

	"k8s.io/api/core/v1"

	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
)

func TestImagePull(t *testing.T) {
	for _, i := range []ImageRequestTestEntry{

		{
			// Default with explicit version, no explicit pull policy
			resolvers: DefaultMockResolvers(
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
			resultPullPolicy: v1.PullIfNotPresent,
			resultError:      false,
		},

		{
			// Default with explicit version, and explicit pull policy
			resolvers: DefaultMockResolvers(
				map[string]string{
					"ENMASSE_DEFAULT_IMG_TAG":         "1.2.3",
					"ENMASSE_DEFAULT_IMG_PULL_POLICY": "Never",
				},
				[]v1beta1.ImageOverride{},
			),
			request: ImageRequest{
				Organization: "enmasseproject",
				Name:         "foo-bar",
			},
			resultName:       "enmasseproject/foo-bar:1.2.3",
			resultPullPolicy: v1.PullNever,
			resultError:      false,
		},

		{
			// Default with explicit version, and explicit pull policy, with more specific one
			resolvers: DefaultMockResolvers(
				map[string]string{
					"ENMASSE_DEFAULT_IMG_TAG":                        "1.2.3",
					"ENMASSE_DEFAULT_IMG_PULL_POLICY":                "Never",
					"ENMASSE_IMG_ENMASSEPROJECT_FOO_BAR_PULL_POLICY": "Always",
				},
				[]v1beta1.ImageOverride{},
			),
			request: ImageRequest{
				Organization: "enmasseproject",
				Name:         "foo-bar",
			},
			resultName:       "enmasseproject/foo-bar:1.2.3",
			resultPullPolicy: v1.PullAlways,
			resultError:      false,
		},

		{
			// Default with explicit version, and explicit pull policy, with more specific one
			// and additional override
			resolvers: DefaultMockResolvers(
				map[string]string{
					"ENMASSE_DEFAULT_IMG_TAG":                        "1.2.3",
					"ENMASSE_DEFAULT_IMG_PULL_POLICY":                "Never",
					"ENMASSE_IMG_ENMASSEPROJECT_FOO_BAR_PULL_POLICY": "Always",
				},
				[]v1beta1.ImageOverride{
					{
						Match: v1beta1.ImageMatch{
							ByNameMatch: &v1beta1.ByNameMatch{
								Name: "foo-bar",
							},
						},
						PullPolicy: v1.PullNever,
					},
				},
			),
			request: ImageRequest{
				Organization: "enmasseproject",
				Name:         "foo-bar",
			},
			resultName:       "enmasseproject/foo-bar:1.2.3",
			resultPullPolicy: v1.PullNever,
			resultError:      false,
		},

		{
			// Default with explicit snapshot version, no explicit pull policy
			resolvers: DefaultMockResolvers(
				map[string]string{
					"ENMASSE_DEFAULT_IMG_TAG": "1.2.3-SNAPSHOT",
				},
				[]v1beta1.ImageOverride{},
			),
			request: ImageRequest{
				Organization: "enmasseproject",
				Name:         "foo-bar",
			},
			resultName:       "enmasseproject/foo-bar:1.2.3-SNAPSHOT",
			resultPullPolicy: v1.PullAlways,
			resultError:      false,
		},

		{
			// Default with explicit "latest" version, no explicit pull policy
			resolvers: DefaultMockResolvers(
				map[string]string{
					"ENMASSE_DEFAULT_IMG_TAG": "latest",
				},
				[]v1beta1.ImageOverride{},
			),
			request: ImageRequest{
				Organization: "enmasseproject",
				Name:         "foo-bar",
			},
			resultName:       "enmasseproject/foo-bar:latest",
			resultPullPolicy: v1.PullAlways,
			resultError:      false,
		},
	} {
		Eval(t, i)
	}
}
