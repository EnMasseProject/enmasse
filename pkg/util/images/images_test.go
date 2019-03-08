/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"testing"

	"k8s.io/api/core/v1"
)

type ImageRequestTestEntry struct {
	resolvers        []ImageResolver
	request          ImageRequest
	resultName       string
	resultPullPolicy v1.PullPolicy
	resultError      bool
}

func mockTestEnvOnly(env map[string]string) []ImageResolver {
	return []ImageResolver{MockEnvironmentResolver(env)}
}

func Eval(t *testing.T, i ImageRequestTestEntry) {
	rc, err := i.request.Resolve(i.resolvers)

	if err != nil && !i.resultError {
		t.Errorf("Expected succeed, but failed with: %v", err)
		return
	} else if err == nil && i.resultError {
		t.Error("Expected to fail, but didn't")
		return
	}

	resultName, err := rc.ToImageName()
	if err != nil {
		t.Errorf("Failed to convert to name: %v - %v", err, i.request)
		return
	}

	resultPullPolicy := rc.PullPolicy

	if !i.resultError {
		if i.resultName != resultName {
			t.Errorf("Expected output '%s', but found '%s'", i.resultName, resultName)
		}
		if i.resultPullPolicy != resultPullPolicy {
			t.Errorf("Expected pull policy '%s', but found '%s'", i.resultPullPolicy, resultPullPolicy)
		}
	}

}

func TestDefaultEnv(t *testing.T) {

	for _, i := range []ImageRequestTestEntry{
		{mockTestEnvOnly(map[string]string{"ENMASSE_DEFAULT_IMG_TAG": "1.2.3"}), ImageRequest{"enmasseproject", "foo-bar"}, "enmasseproject/foo-bar:1.2.3", "", false},
		{mockTestEnvOnly(map[string]string{"ENMASSE_IMG_ENMASSEPROJECT_FOO_BAR_TAG": "1.2.4"}), ImageRequest{"enmasseproject", "foo-bar"}, "enmasseproject/foo-bar:1.2.4", "", false},
		{mockTestEnvOnly(map[string]string{"ENMASSE_IMG_ENMASSEPROJECT_FOO_BAR": "dummy.io/org/repo:1.2.5"}), ImageRequest{"enmasseproject", "foo-bar"}, "dummy.io/org/repo:1.2.5", "", false},
		{mockTestEnvOnly(map[string]string{"ENMASSE_IMG_ENMASSEPROJECT_FOO_BAR": "dummy.io:1234/org/repo:1.2.5"}), ImageRequest{"enmasseproject", "foo-bar"}, "dummy.io:1234/org/repo:1.2.5", "", false},
		{mockTestEnvOnly(map[string]string{"ENMASSE_DEFAULT_IMG_TAG": "1.2.3", "ENMASSE_IMGORG_ENMASSEPROJECT_REGISTRY": "otherreg", "ENMASSE_IMGORG_ENMASSEPROJECT_ORG": "otherorg"}), ImageRequest{"enmasseproject", "foo-bar"}, "otherreg/otherorg/foo-bar:1.2.3", "", false},

		// although there are defaults, the explicit cne should override
		{mockTestEnvOnly(map[string]string{
			"ENMASSE_DEFAULT_IMG_TAG":            "latest",
			"ENMASSE_DEFAULT_IMG_REGISTRY":       "otherreg",
			"ENMASSE_IMGORG_ENMASSEPROJECT_ORG":  "otherorg",
			"ENMASSE_IMG_ENMASSEPROJECT_FOO_BAR": "fooreg/fooorg/fooimg:footag",
		}),
			ImageRequest{
				"enmasseproject",
				"foo-bar"},
			"fooreg/fooorg/fooimg:footag",
			"",
			false,
		},
	} {
		Eval(t, i)
	}

}
