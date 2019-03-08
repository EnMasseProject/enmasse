/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

type CompositeImageResolver struct {
	Resolvers []ImageResolver
}

func (r CompositeImageResolver) Resolve(request ImageRequest, image ImageDefinition) (ImageDefinition, error) {
	return runResolvers(request, image, r.Resolvers)
}

var _ ImageResolver = &CompositeImageResolver{}
