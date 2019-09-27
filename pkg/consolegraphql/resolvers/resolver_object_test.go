/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/stretchr/testify/assert"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"testing"
	"time"
)

func TestCreationTimestamp(t *testing.T) {
	resolver := objectMetaK8sResolver{}

	epoch := time.Unix(0, 0).In(time.UTC)
	expected := "1970-01-01T00:00:00Z"

	meta := &v1.ObjectMeta{
		CreationTimestamp: v1.NewTime(epoch),
	}
	actual, e := resolver.CreationTimestamp(context.TODO(), meta)
	assert.NoError(t, e)
	assert.Equal(t, expected, actual, "Unexpected timestamp")
}
