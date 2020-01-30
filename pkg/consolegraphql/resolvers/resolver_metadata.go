/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
)
//v1 "k8s.io/apimachinery/pkg/apis/meta/v1"


func (r *Resolver) Metadata_consoleapi_enmasse_io_v1beta1() Metadata_consoleapi_enmasse_io_v1beta1Resolver {
	return &metadataK8sResolver{r}
}

type metadataK8sResolver struct{ *Resolver }

func (r *metadataK8sResolver) UID(ctx context.Context, obj *consolegraphql.Metadata) (string, error) {
	return string(obj.UID), nil
}

func (r *metadataK8sResolver) CreationTimestamp(ctx context.Context, obj *consolegraphql.Metadata) (string, error) {
	return obj.CreationTimestamp.UTC().Format("2006-01-02T15:04:05Z"), nil
}

func (r *metadataK8sResolver) Annotations(ctx context.Context, obj *consolegraphql.Metadata) ([]*KeyValue, error) {
	var list []*KeyValue
	for k, v := range obj.Annotations {
		plan := KeyValue{
			Key:   k,
			Value: v,
		}
		list = append(list, &plan)
	}
	return list, nil
}
