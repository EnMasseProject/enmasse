/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package altclient

import (
	"context"

	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type forwardingClient struct {
	reader client.Reader
	writer client.Writer
	status client.StatusWriter
}

var _ client.Client = &forwardingClient{}

func NewForwarding(reader client.Reader, writer client.Writer, status client.StatusWriter) client.Client {
	return forwardingClient{
		reader: reader,
		writer: writer,
		status: status,
	}
}

func (f forwardingClient) Get(ctx context.Context, key client.ObjectKey, obj runtime.Object) error {
	return f.reader.Get(ctx, key, obj)
}

func (f forwardingClient) List(ctx context.Context, list runtime.Object, opts ...client.ListOption) error {
	return f.reader.List(ctx, list, opts...)
}

func (f forwardingClient) Create(ctx context.Context, obj runtime.Object, opts ...client.CreateOption) error {
	return f.writer.Create(ctx, obj, opts...)
}

func (f forwardingClient) Delete(ctx context.Context, obj runtime.Object, opts ...client.DeleteOption) error {
	return f.writer.Delete(ctx, obj, opts...)
}

func (f forwardingClient) Update(ctx context.Context, obj runtime.Object, opts ...client.UpdateOption) error {
	return f.writer.Update(ctx, obj, opts...)
}

func (f forwardingClient) Patch(ctx context.Context, obj runtime.Object, patch client.Patch, opts ...client.PatchOption) error {
	return f.writer.Patch(ctx, obj, patch, opts...)
}

func (f forwardingClient) DeleteAllOf(ctx context.Context, obj runtime.Object, opts ...client.DeleteAllOfOption) error {
	return f.writer.DeleteAllOf(ctx, obj, opts...)
}

func (f forwardingClient) Status() client.StatusWriter {
	return f.status
}
