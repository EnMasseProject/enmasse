/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"regexp"
	"strings"

	"github.com/enmasseproject/enmasse/pkg/util"
)

var imagePattern = regexp.MustCompile("^((.*)/)?([^/]+)/([^/]+):([^:]+)$")
var nameReplacer = regexp.MustCompile("[^a-zA-Z0-9]")

func nameToEnvName(request ImageRequest) string {

	name := strings.ToUpper(request.Organization + "_" + request.Name)
	name = nameReplacer.ReplaceAllString(name, "_")

	return name
}

func defaultEnvironmentResolver(provider util.EnvironmentProvider) CompositeImageResolver {
	return CompositeImageResolver{
		[]ImageResolver{
			defaultEnvironmentImageResolver{provider},
			organizationEnvironmentImageResolver{provider},
			nameEnvironmentImageResolver{provider},
			explicitEnvironmentImageResolver{provider},
		},
	}
}

func OSEnvironmentResolver() CompositeImageResolver {
	return defaultEnvironmentResolver(util.OSEnvironmentProvider{})
}

func MockEnvironmentResolver(env map[string]string) CompositeImageResolver {
	return defaultEnvironmentResolver(util.MockEnvironmentProvider{Environment: env})
}
