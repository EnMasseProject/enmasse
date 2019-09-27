/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func (r *Resolver) ObjectMeta_v1() ObjectMeta_v1Resolver {
	return &objectMetaK8sResolver{r}
}

type objectMetaK8sResolver struct{ *Resolver }

func (r *objectMetaK8sResolver) UID(ctx context.Context, obj *v1.ObjectMeta) (string, error) {
	return string(obj.UID), nil
}

func (r *objectMetaK8sResolver) CreationTimestamp(ctx context.Context, obj *v1.ObjectMeta) (string, error) {
	return obj.CreationTimestamp.String(), nil
}

func (r *objectMetaK8sResolver) Annotations(ctx context.Context, obj *v1.ObjectMeta) ([]*KeyValue, error) {
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
