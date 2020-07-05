/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

// Code generated by client-gen. DO NOT EDIT.

package fake

import (
	enmassev1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	labels "k8s.io/apimachinery/pkg/labels"
	schema "k8s.io/apimachinery/pkg/runtime/schema"
	types "k8s.io/apimachinery/pkg/types"
	watch "k8s.io/apimachinery/pkg/watch"
	testing "k8s.io/client-go/testing"
)

// FakeMessagingConsoles implements MessagingConsoleInterface
type FakeMessagingConsoles struct {
	Fake *FakeEnmasseV1
	ns   string
}

var messagingconsolesResource = schema.GroupVersionResource{Group: "enmasse.io", Version: "v1", Resource: "messagingconsoles"}

var messagingconsolesKind = schema.GroupVersionKind{Group: "enmasse.io", Version: "v1", Kind: "MessagingConsole"}

// Get takes name of the messagingConsole, and returns the corresponding messagingConsole object, and an error if there is any.
func (c *FakeMessagingConsoles) Get(name string, options v1.GetOptions) (result *enmassev1.MessagingConsole, err error) {
	obj, err := c.Fake.
		Invokes(testing.NewGetAction(messagingconsolesResource, c.ns, name), &enmassev1.MessagingConsole{})

	if obj == nil {
		return nil, err
	}
	return obj.(*enmassev1.MessagingConsole), err
}

// List takes label and field selectors, and returns the list of MessagingConsoles that match those selectors.
func (c *FakeMessagingConsoles) List(opts v1.ListOptions) (result *enmassev1.MessagingConsoleList, err error) {
	obj, err := c.Fake.
		Invokes(testing.NewListAction(messagingconsolesResource, messagingconsolesKind, c.ns, opts), &enmassev1.MessagingConsoleList{})

	if obj == nil {
		return nil, err
	}

	label, _, _ := testing.ExtractFromListOptions(opts)
	if label == nil {
		label = labels.Everything()
	}
	list := &enmassev1.MessagingConsoleList{ListMeta: obj.(*enmassev1.MessagingConsoleList).ListMeta}
	for _, item := range obj.(*enmassev1.MessagingConsoleList).Items {
		if label.Matches(labels.Set(item.Labels)) {
			list.Items = append(list.Items, item)
		}
	}
	return list, err
}

// Watch returns a watch.Interface that watches the requested messagingConsoles.
func (c *FakeMessagingConsoles) Watch(opts v1.ListOptions) (watch.Interface, error) {
	return c.Fake.
		InvokesWatch(testing.NewWatchAction(messagingconsolesResource, c.ns, opts))

}

// Create takes the representation of a messagingConsole and creates it.  Returns the server's representation of the messagingConsole, and an error, if there is any.
func (c *FakeMessagingConsoles) Create(messagingConsole *enmassev1.MessagingConsole) (result *enmassev1.MessagingConsole, err error) {
	obj, err := c.Fake.
		Invokes(testing.NewCreateAction(messagingconsolesResource, c.ns, messagingConsole), &enmassev1.MessagingConsole{})

	if obj == nil {
		return nil, err
	}
	return obj.(*enmassev1.MessagingConsole), err
}

// Update takes the representation of a messagingConsole and updates it. Returns the server's representation of the messagingConsole, and an error, if there is any.
func (c *FakeMessagingConsoles) Update(messagingConsole *enmassev1.MessagingConsole) (result *enmassev1.MessagingConsole, err error) {
	obj, err := c.Fake.
		Invokes(testing.NewUpdateAction(messagingconsolesResource, c.ns, messagingConsole), &enmassev1.MessagingConsole{})

	if obj == nil {
		return nil, err
	}
	return obj.(*enmassev1.MessagingConsole), err
}

// UpdateStatus was generated because the type contains a Status member.
// Add a +genclient:noStatus comment above the type to avoid generating UpdateStatus().
func (c *FakeMessagingConsoles) UpdateStatus(messagingConsole *enmassev1.MessagingConsole) (*enmassev1.MessagingConsole, error) {
	obj, err := c.Fake.
		Invokes(testing.NewUpdateSubresourceAction(messagingconsolesResource, "status", c.ns, messagingConsole), &enmassev1.MessagingConsole{})

	if obj == nil {
		return nil, err
	}
	return obj.(*enmassev1.MessagingConsole), err
}

// Delete takes name of the messagingConsole and deletes it. Returns an error if one occurs.
func (c *FakeMessagingConsoles) Delete(name string, options *v1.DeleteOptions) error {
	_, err := c.Fake.
		Invokes(testing.NewDeleteAction(messagingconsolesResource, c.ns, name), &enmassev1.MessagingConsole{})

	return err
}

// DeleteCollection deletes a collection of objects.
func (c *FakeMessagingConsoles) DeleteCollection(options *v1.DeleteOptions, listOptions v1.ListOptions) error {
	action := testing.NewDeleteCollectionAction(messagingconsolesResource, c.ns, listOptions)

	_, err := c.Fake.Invokes(action, &enmassev1.MessagingConsoleList{})
	return err
}

// Patch applies the patch and returns the patched messagingConsole.
func (c *FakeMessagingConsoles) Patch(name string, pt types.PatchType, data []byte, subresources ...string) (result *enmassev1.MessagingConsole, err error) {
	obj, err := c.Fake.
		Invokes(testing.NewPatchSubresourceAction(messagingconsolesResource, c.ns, name, pt, data, subresources...), &enmassev1.MessagingConsole{})

	if obj == nil {
		return nil, err
	}
	return obj.(*enmassev1.MessagingConsole), err
}
