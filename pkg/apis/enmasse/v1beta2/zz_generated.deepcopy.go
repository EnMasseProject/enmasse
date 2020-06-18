// +build !ignore_autogenerated

/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

// Code generated by deepcopy-gen. DO NOT EDIT.

package v1beta2

import (
	v1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	routev1 "github.com/openshift/api/route/v1"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	runtime "k8s.io/apimachinery/pkg/runtime"
)

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *InputValue) DeepCopyInto(out *InputValue) {
	*out = *in
	if in.ValueFromSecret != nil {
		in, out := &in.ValueFromSecret, &out.ValueFromSecret
		*out = new(v1.SecretKeySelector)
		(*in).DeepCopyInto(*out)
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new InputValue.
func (in *InputValue) DeepCopy() *InputValue {
	if in == nil {
		return nil
	}
	out := new(InputValue)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingAddress) DeepCopyInto(out *MessagingAddress) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ObjectMeta.DeepCopyInto(&out.ObjectMeta)
	in.Spec.DeepCopyInto(&out.Spec)
	in.Status.DeepCopyInto(&out.Status)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingAddress.
func (in *MessagingAddress) DeepCopy() *MessagingAddress {
	if in == nil {
		return nil
	}
	out := new(MessagingAddress)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *MessagingAddress) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingAddressBroker) DeepCopyInto(out *MessagingAddressBroker) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingAddressBroker.
func (in *MessagingAddressBroker) DeepCopy() *MessagingAddressBroker {
	if in == nil {
		return nil
	}
	out := new(MessagingAddressBroker)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingAddressCondition) DeepCopyInto(out *MessagingAddressCondition) {
	*out = *in
	in.LastTransitionTime.DeepCopyInto(&out.LastTransitionTime)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingAddressCondition.
func (in *MessagingAddressCondition) DeepCopy() *MessagingAddressCondition {
	if in == nil {
		return nil
	}
	out := new(MessagingAddressCondition)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingAddressList) DeepCopyInto(out *MessagingAddressList) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ListMeta.DeepCopyInto(&out.ListMeta)
	if in.Items != nil {
		in, out := &in.Items, &out.Items
		*out = make([]MessagingAddress, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingAddressList.
func (in *MessagingAddressList) DeepCopy() *MessagingAddressList {
	if in == nil {
		return nil
	}
	out := new(MessagingAddressList)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *MessagingAddressList) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingAddressSpec) DeepCopyInto(out *MessagingAddressSpec) {
	*out = *in
	if in.Address != nil {
		in, out := &in.Address, &out.Address
		*out = new(string)
		**out = **in
	}
	if in.Anycast != nil {
		in, out := &in.Anycast, &out.Anycast
		*out = new(MessagingAddressSpecAnycast)
		**out = **in
	}
	if in.Multicast != nil {
		in, out := &in.Multicast, &out.Multicast
		*out = new(MessagingAddressSpecMulticast)
		**out = **in
	}
	if in.Queue != nil {
		in, out := &in.Queue, &out.Queue
		*out = new(MessagingAddressSpecQueue)
		**out = **in
	}
	if in.Topic != nil {
		in, out := &in.Topic, &out.Topic
		*out = new(MessagingAddressSpecTopic)
		**out = **in
	}
	if in.Subscription != nil {
		in, out := &in.Subscription, &out.Subscription
		*out = new(MessagingAddressSpecSubscription)
		**out = **in
	}
	if in.DeadLetter != nil {
		in, out := &in.DeadLetter, &out.DeadLetter
		*out = new(MessagingAddressSpecDeadLetter)
		**out = **in
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingAddressSpec.
func (in *MessagingAddressSpec) DeepCopy() *MessagingAddressSpec {
	if in == nil {
		return nil
	}
	out := new(MessagingAddressSpec)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingAddressSpecAnycast) DeepCopyInto(out *MessagingAddressSpecAnycast) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingAddressSpecAnycast.
func (in *MessagingAddressSpecAnycast) DeepCopy() *MessagingAddressSpecAnycast {
	if in == nil {
		return nil
	}
	out := new(MessagingAddressSpecAnycast)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingAddressSpecDeadLetter) DeepCopyInto(out *MessagingAddressSpecDeadLetter) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingAddressSpecDeadLetter.
func (in *MessagingAddressSpecDeadLetter) DeepCopy() *MessagingAddressSpecDeadLetter {
	if in == nil {
		return nil
	}
	out := new(MessagingAddressSpecDeadLetter)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingAddressSpecMulticast) DeepCopyInto(out *MessagingAddressSpecMulticast) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingAddressSpecMulticast.
func (in *MessagingAddressSpecMulticast) DeepCopy() *MessagingAddressSpecMulticast {
	if in == nil {
		return nil
	}
	out := new(MessagingAddressSpecMulticast)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingAddressSpecQueue) DeepCopyInto(out *MessagingAddressSpecQueue) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingAddressSpecQueue.
func (in *MessagingAddressSpecQueue) DeepCopy() *MessagingAddressSpecQueue {
	if in == nil {
		return nil
	}
	out := new(MessagingAddressSpecQueue)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingAddressSpecSubscription) DeepCopyInto(out *MessagingAddressSpecSubscription) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingAddressSpecSubscription.
func (in *MessagingAddressSpecSubscription) DeepCopy() *MessagingAddressSpecSubscription {
	if in == nil {
		return nil
	}
	out := new(MessagingAddressSpecSubscription)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingAddressSpecTopic) DeepCopyInto(out *MessagingAddressSpecTopic) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingAddressSpecTopic.
func (in *MessagingAddressSpecTopic) DeepCopy() *MessagingAddressSpecTopic {
	if in == nil {
		return nil
	}
	out := new(MessagingAddressSpecTopic)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingAddressStatus) DeepCopyInto(out *MessagingAddressStatus) {
	*out = *in
	if in.Conditions != nil {
		in, out := &in.Conditions, &out.Conditions
		*out = make([]MessagingAddressCondition, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
	if in.Brokers != nil {
		in, out := &in.Brokers, &out.Brokers
		*out = make([]MessagingAddressBroker, len(*in))
		copy(*out, *in)
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingAddressStatus.
func (in *MessagingAddressStatus) DeepCopy() *MessagingAddressStatus {
	if in == nil {
		return nil
	}
	out := new(MessagingAddressStatus)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpoint) DeepCopyInto(out *MessagingEndpoint) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ObjectMeta.DeepCopyInto(&out.ObjectMeta)
	in.Spec.DeepCopyInto(&out.Spec)
	in.Status.DeepCopyInto(&out.Status)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpoint.
func (in *MessagingEndpoint) DeepCopy() *MessagingEndpoint {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpoint)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *MessagingEndpoint) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointCertValidity) DeepCopyInto(out *MessagingEndpointCertValidity) {
	*out = *in
	in.NotBefore.DeepCopyInto(&out.NotBefore)
	in.NotAfter.DeepCopyInto(&out.NotAfter)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointCertValidity.
func (in *MessagingEndpointCertValidity) DeepCopy() *MessagingEndpointCertValidity {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointCertValidity)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointCondition) DeepCopyInto(out *MessagingEndpointCondition) {
	*out = *in
	in.LastTransitionTime.DeepCopyInto(&out.LastTransitionTime)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointCondition.
func (in *MessagingEndpointCondition) DeepCopy() *MessagingEndpointCondition {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointCondition)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointList) DeepCopyInto(out *MessagingEndpointList) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ListMeta.DeepCopyInto(&out.ListMeta)
	if in.Items != nil {
		in, out := &in.Items, &out.Items
		*out = make([]MessagingEndpoint, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointList.
func (in *MessagingEndpointList) DeepCopy() *MessagingEndpointList {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointList)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *MessagingEndpointList) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointPort) DeepCopyInto(out *MessagingEndpointPort) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointPort.
func (in *MessagingEndpointPort) DeepCopy() *MessagingEndpointPort {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointPort)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointSpec) DeepCopyInto(out *MessagingEndpointSpec) {
	*out = *in
	if in.Tls != nil {
		in, out := &in.Tls, &out.Tls
		*out = new(MessagingEndpointSpecTls)
		(*in).DeepCopyInto(*out)
	}
	if in.Annotations != nil {
		in, out := &in.Annotations, &out.Annotations
		*out = make(map[string]string, len(*in))
		for key, val := range *in {
			(*out)[key] = val
		}
	}
	if in.Protocols != nil {
		in, out := &in.Protocols, &out.Protocols
		*out = make([]MessagingEndpointProtocol, len(*in))
		copy(*out, *in)
	}
	if in.Host != nil {
		in, out := &in.Host, &out.Host
		*out = new(string)
		**out = **in
	}
	if in.Cluster != nil {
		in, out := &in.Cluster, &out.Cluster
		*out = new(MessagingEndpointSpecCluster)
		**out = **in
	}
	if in.Ingress != nil {
		in, out := &in.Ingress, &out.Ingress
		*out = new(MessagingEndpointSpecIngress)
		**out = **in
	}
	if in.Route != nil {
		in, out := &in.Route, &out.Route
		*out = new(MessagingEndpointSpecRoute)
		(*in).DeepCopyInto(*out)
	}
	if in.NodePort != nil {
		in, out := &in.NodePort, &out.NodePort
		*out = new(MessagingEndpointSpecNodePort)
		**out = **in
	}
	if in.LoadBalancer != nil {
		in, out := &in.LoadBalancer, &out.LoadBalancer
		*out = new(MessagingEndpointSpecLoadBalancer)
		**out = **in
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointSpec.
func (in *MessagingEndpointSpec) DeepCopy() *MessagingEndpointSpec {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointSpec)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointSpecCluster) DeepCopyInto(out *MessagingEndpointSpecCluster) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointSpecCluster.
func (in *MessagingEndpointSpecCluster) DeepCopy() *MessagingEndpointSpecCluster {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointSpecCluster)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointSpecIngress) DeepCopyInto(out *MessagingEndpointSpecIngress) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointSpecIngress.
func (in *MessagingEndpointSpecIngress) DeepCopy() *MessagingEndpointSpecIngress {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointSpecIngress)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointSpecLoadBalancer) DeepCopyInto(out *MessagingEndpointSpecLoadBalancer) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointSpecLoadBalancer.
func (in *MessagingEndpointSpecLoadBalancer) DeepCopy() *MessagingEndpointSpecLoadBalancer {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointSpecLoadBalancer)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointSpecNodePort) DeepCopyInto(out *MessagingEndpointSpecNodePort) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointSpecNodePort.
func (in *MessagingEndpointSpecNodePort) DeepCopy() *MessagingEndpointSpecNodePort {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointSpecNodePort)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointSpecRoute) DeepCopyInto(out *MessagingEndpointSpecRoute) {
	*out = *in
	if in.TlsTermination != nil {
		in, out := &in.TlsTermination, &out.TlsTermination
		*out = new(routev1.TLSTerminationType)
		**out = **in
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointSpecRoute.
func (in *MessagingEndpointSpecRoute) DeepCopy() *MessagingEndpointSpecRoute {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointSpecRoute)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointSpecTls) DeepCopyInto(out *MessagingEndpointSpecTls) {
	*out = *in
	if in.Protocols != nil {
		in, out := &in.Protocols, &out.Protocols
		*out = new(string)
		**out = **in
	}
	if in.Ciphers != nil {
		in, out := &in.Ciphers, &out.Ciphers
		*out = new(string)
		**out = **in
	}
	if in.Selfsigned != nil {
		in, out := &in.Selfsigned, &out.Selfsigned
		*out = new(MessagingEndpointSpecTlsSelfsigned)
		**out = **in
	}
	if in.Openshift != nil {
		in, out := &in.Openshift, &out.Openshift
		*out = new(MessagingEndpointSpecTlsOpenshift)
		**out = **in
	}
	if in.External != nil {
		in, out := &in.External, &out.External
		*out = new(MessagingEndpointSpecTlsExternal)
		(*in).DeepCopyInto(*out)
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointSpecTls.
func (in *MessagingEndpointSpecTls) DeepCopy() *MessagingEndpointSpecTls {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointSpecTls)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointSpecTlsExternal) DeepCopyInto(out *MessagingEndpointSpecTlsExternal) {
	*out = *in
	in.Key.DeepCopyInto(&out.Key)
	in.Certificate.DeepCopyInto(&out.Certificate)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointSpecTlsExternal.
func (in *MessagingEndpointSpecTlsExternal) DeepCopy() *MessagingEndpointSpecTlsExternal {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointSpecTlsExternal)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointSpecTlsOpenshift) DeepCopyInto(out *MessagingEndpointSpecTlsOpenshift) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointSpecTlsOpenshift.
func (in *MessagingEndpointSpecTlsOpenshift) DeepCopy() *MessagingEndpointSpecTlsOpenshift {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointSpecTlsOpenshift)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointSpecTlsSelfsigned) DeepCopyInto(out *MessagingEndpointSpecTlsSelfsigned) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointSpecTlsSelfsigned.
func (in *MessagingEndpointSpecTlsSelfsigned) DeepCopy() *MessagingEndpointSpecTlsSelfsigned {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointSpecTlsSelfsigned)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointStatus) DeepCopyInto(out *MessagingEndpointStatus) {
	*out = *in
	if in.Conditions != nil {
		in, out := &in.Conditions, &out.Conditions
		*out = make([]MessagingEndpointCondition, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
	if in.Ports != nil {
		in, out := &in.Ports, &out.Ports
		*out = make([]MessagingEndpointPort, len(*in))
		copy(*out, *in)
	}
	if in.Tls != nil {
		in, out := &in.Tls, &out.Tls
		*out = new(MessagingEndpointStatusTls)
		(*in).DeepCopyInto(*out)
	}
	if in.InternalPorts != nil {
		in, out := &in.InternalPorts, &out.InternalPorts
		*out = make([]MessagingEndpointPort, len(*in))
		copy(*out, *in)
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointStatus.
func (in *MessagingEndpointStatus) DeepCopy() *MessagingEndpointStatus {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointStatus)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingEndpointStatusTls) DeepCopyInto(out *MessagingEndpointStatusTls) {
	*out = *in
	if in.CertificateValidity != nil {
		in, out := &in.CertificateValidity, &out.CertificateValidity
		*out = new(MessagingEndpointCertValidity)
		(*in).DeepCopyInto(*out)
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingEndpointStatusTls.
func (in *MessagingEndpointStatusTls) DeepCopy() *MessagingEndpointStatusTls {
	if in == nil {
		return nil
	}
	out := new(MessagingEndpointStatusTls)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructure) DeepCopyInto(out *MessagingInfrastructure) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ObjectMeta.DeepCopyInto(&out.ObjectMeta)
	in.Spec.DeepCopyInto(&out.Spec)
	in.Status.DeepCopyInto(&out.Status)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructure.
func (in *MessagingInfrastructure) DeepCopy() *MessagingInfrastructure {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructure)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *MessagingInfrastructure) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureCondition) DeepCopyInto(out *MessagingInfrastructureCondition) {
	*out = *in
	in.LastTransitionTime.DeepCopyInto(&out.LastTransitionTime)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureCondition.
func (in *MessagingInfrastructureCondition) DeepCopy() *MessagingInfrastructureCondition {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureCondition)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureList) DeepCopyInto(out *MessagingInfrastructureList) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ListMeta.DeepCopyInto(&out.ListMeta)
	if in.Items != nil {
		in, out := &in.Items, &out.Items
		*out = make([]MessagingInfrastructure, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureList.
func (in *MessagingInfrastructureList) DeepCopy() *MessagingInfrastructureList {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureList)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *MessagingInfrastructureList) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureReference) DeepCopyInto(out *MessagingInfrastructureReference) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureReference.
func (in *MessagingInfrastructureReference) DeepCopy() *MessagingInfrastructureReference {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureReference)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureSpec) DeepCopyInto(out *MessagingInfrastructureSpec) {
	*out = *in
	if in.NamespaceSelector != nil {
		in, out := &in.NamespaceSelector, &out.NamespaceSelector
		*out = new(NamespaceSelector)
		(*in).DeepCopyInto(*out)
	}
	in.Router.DeepCopyInto(&out.Router)
	in.Broker.DeepCopyInto(&out.Broker)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureSpec.
func (in *MessagingInfrastructureSpec) DeepCopy() *MessagingInfrastructureSpec {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureSpec)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureSpecBroker) DeepCopyInto(out *MessagingInfrastructureSpecBroker) {
	*out = *in
	if in.InitImage != nil {
		in, out := &in.InitImage, &out.InitImage
		*out = new(v1beta1.ImageOverride)
		**out = **in
	}
	if in.Image != nil {
		in, out := &in.Image, &out.Image
		*out = new(v1beta1.ImageOverride)
		**out = **in
	}
	if in.ScalingStrategy != nil {
		in, out := &in.ScalingStrategy, &out.ScalingStrategy
		*out = new(MessagingInfrastructureSpecBrokerScalingStrategy)
		(*in).DeepCopyInto(*out)
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureSpecBroker.
func (in *MessagingInfrastructureSpecBroker) DeepCopy() *MessagingInfrastructureSpecBroker {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureSpecBroker)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureSpecBrokerScalingStrategy) DeepCopyInto(out *MessagingInfrastructureSpecBrokerScalingStrategy) {
	*out = *in
	if in.Static != nil {
		in, out := &in.Static, &out.Static
		*out = new(MessagingInfrastructureSpecBrokerScalingStrategyStatic)
		**out = **in
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureSpecBrokerScalingStrategy.
func (in *MessagingInfrastructureSpecBrokerScalingStrategy) DeepCopy() *MessagingInfrastructureSpecBrokerScalingStrategy {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureSpecBrokerScalingStrategy)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureSpecBrokerScalingStrategyStatic) DeepCopyInto(out *MessagingInfrastructureSpecBrokerScalingStrategyStatic) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureSpecBrokerScalingStrategyStatic.
func (in *MessagingInfrastructureSpecBrokerScalingStrategyStatic) DeepCopy() *MessagingInfrastructureSpecBrokerScalingStrategyStatic {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureSpecBrokerScalingStrategyStatic)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureSpecRouter) DeepCopyInto(out *MessagingInfrastructureSpecRouter) {
	*out = *in
	if in.Image != nil {
		in, out := &in.Image, &out.Image
		*out = new(v1beta1.ImageOverride)
		**out = **in
	}
	if in.ScalingStrategy != nil {
		in, out := &in.ScalingStrategy, &out.ScalingStrategy
		*out = new(MessagingInfrastructureSpecRouterScalingStrategy)
		(*in).DeepCopyInto(*out)
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureSpecRouter.
func (in *MessagingInfrastructureSpecRouter) DeepCopy() *MessagingInfrastructureSpecRouter {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureSpecRouter)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureSpecRouterScalingStrategy) DeepCopyInto(out *MessagingInfrastructureSpecRouterScalingStrategy) {
	*out = *in
	if in.Static != nil {
		in, out := &in.Static, &out.Static
		*out = new(MessagingInfrastructureSpecRouterScalingStrategyStatic)
		**out = **in
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureSpecRouterScalingStrategy.
func (in *MessagingInfrastructureSpecRouterScalingStrategy) DeepCopy() *MessagingInfrastructureSpecRouterScalingStrategy {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureSpecRouterScalingStrategy)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureSpecRouterScalingStrategyStatic) DeepCopyInto(out *MessagingInfrastructureSpecRouterScalingStrategyStatic) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureSpecRouterScalingStrategyStatic.
func (in *MessagingInfrastructureSpecRouterScalingStrategyStatic) DeepCopy() *MessagingInfrastructureSpecRouterScalingStrategyStatic {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureSpecRouterScalingStrategyStatic)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureStatus) DeepCopyInto(out *MessagingInfrastructureStatus) {
	*out = *in
	if in.Conditions != nil {
		in, out := &in.Conditions, &out.Conditions
		*out = make([]MessagingInfrastructureCondition, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
	if in.Routers != nil {
		in, out := &in.Routers, &out.Routers
		*out = make([]MessagingInfrastructureStatusRouter, len(*in))
		copy(*out, *in)
	}
	if in.Brokers != nil {
		in, out := &in.Brokers, &out.Brokers
		*out = make([]MessagingInfrastructureStatusBroker, len(*in))
		copy(*out, *in)
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureStatus.
func (in *MessagingInfrastructureStatus) DeepCopy() *MessagingInfrastructureStatus {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureStatus)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureStatusBroker) DeepCopyInto(out *MessagingInfrastructureStatusBroker) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureStatusBroker.
func (in *MessagingInfrastructureStatusBroker) DeepCopy() *MessagingInfrastructureStatusBroker {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureStatusBroker)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingInfrastructureStatusRouter) DeepCopyInto(out *MessagingInfrastructureStatusRouter) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingInfrastructureStatusRouter.
func (in *MessagingInfrastructureStatusRouter) DeepCopy() *MessagingInfrastructureStatusRouter {
	if in == nil {
		return nil
	}
	out := new(MessagingInfrastructureStatusRouter)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingTenant) DeepCopyInto(out *MessagingTenant) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ObjectMeta.DeepCopyInto(&out.ObjectMeta)
	in.Spec.DeepCopyInto(&out.Spec)
	in.Status.DeepCopyInto(&out.Status)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingTenant.
func (in *MessagingTenant) DeepCopy() *MessagingTenant {
	if in == nil {
		return nil
	}
	out := new(MessagingTenant)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *MessagingTenant) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingTenantCondition) DeepCopyInto(out *MessagingTenantCondition) {
	*out = *in
	in.LastTransitionTime.DeepCopyInto(&out.LastTransitionTime)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingTenantCondition.
func (in *MessagingTenantCondition) DeepCopy() *MessagingTenantCondition {
	if in == nil {
		return nil
	}
	out := new(MessagingTenantCondition)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingTenantList) DeepCopyInto(out *MessagingTenantList) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ListMeta.DeepCopyInto(&out.ListMeta)
	if in.Items != nil {
		in, out := &in.Items, &out.Items
		*out = make([]MessagingTenant, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingTenantList.
func (in *MessagingTenantList) DeepCopy() *MessagingTenantList {
	if in == nil {
		return nil
	}
	out := new(MessagingTenantList)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *MessagingTenantList) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingTenantSpec) DeepCopyInto(out *MessagingTenantSpec) {
	*out = *in
	if in.MessagingInfrastructureRef != nil {
		in, out := &in.MessagingInfrastructureRef, &out.MessagingInfrastructureRef
		*out = new(MessagingInfrastructureReference)
		**out = **in
	}
	if in.Capabilities != nil {
		in, out := &in.Capabilities, &out.Capabilities
		*out = make([]MessagingCapability, len(*in))
		copy(*out, *in)
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingTenantSpec.
func (in *MessagingTenantSpec) DeepCopy() *MessagingTenantSpec {
	if in == nil {
		return nil
	}
	out := new(MessagingTenantSpec)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *MessagingTenantStatus) DeepCopyInto(out *MessagingTenantStatus) {
	*out = *in
	out.MessagingInfrastructureRef = in.MessagingInfrastructureRef
	if in.Conditions != nil {
		in, out := &in.Conditions, &out.Conditions
		*out = make([]MessagingTenantCondition, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
	if in.Capabilities != nil {
		in, out := &in.Capabilities, &out.Capabilities
		*out = make([]MessagingCapability, len(*in))
		copy(*out, *in)
	}
	if in.Broker != nil {
		in, out := &in.Broker, &out.Broker
		*out = new(MessagingAddressBroker)
		**out = **in
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new MessagingTenantStatus.
func (in *MessagingTenantStatus) DeepCopy() *MessagingTenantStatus {
	if in == nil {
		return nil
	}
	out := new(MessagingTenantStatus)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *NamespaceSelector) DeepCopyInto(out *NamespaceSelector) {
	*out = *in
	if in.MatchLabels != nil {
		in, out := &in.MatchLabels, &out.MatchLabels
		*out = make(map[string]string, len(*in))
		for key, val := range *in {
			(*out)[key] = val
		}
	}
	if in.MatchExpressions != nil {
		in, out := &in.MatchExpressions, &out.MatchExpressions
		*out = make([]metav1.LabelSelectorRequirement, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
	if in.MatchNames != nil {
		in, out := &in.MatchNames, &out.MatchNames
		*out = make([]string, len(*in))
		copy(*out, *in)
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new NamespaceSelector.
func (in *NamespaceSelector) DeepCopy() *NamespaceSelector {
	if in == nil {
		return nil
	}
	out := new(NamespaceSelector)
	in.DeepCopyInto(out)
	return out
}
