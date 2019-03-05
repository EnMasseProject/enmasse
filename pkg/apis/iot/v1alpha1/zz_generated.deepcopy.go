// +build !ignore_autogenerated

/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

// Code generated by deepcopy-gen. DO NOT EDIT.

package v1alpha1

import (
	v1 "k8s.io/api/core/v1"
	runtime "k8s.io/apimachinery/pkg/runtime"
)

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *AdapterSpec) DeepCopyInto(out *AdapterSpec) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new AdapterSpec.
func (in *AdapterSpec) DeepCopy() *AdapterSpec {
	if in == nil {
		return nil
	}
	out := new(AdapterSpec)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *Credentials) DeepCopyInto(out *Credentials) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new Credentials.
func (in *Credentials) DeepCopy() *Credentials {
	if in == nil {
		return nil
	}
	out := new(Credentials)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *DownstreamStrategy) DeepCopyInto(out *DownstreamStrategy) {
	*out = *in
	if in.ExternalDownstreamStrategy != nil {
		in, out := &in.ExternalDownstreamStrategy, &out.ExternalDownstreamStrategy
		*out = new(ExternalDownstreamStrategy)
		(*in).DeepCopyInto(*out)
	}
	if in.ProvidedDownstreamStrategy != nil {
		in, out := &in.ProvidedDownstreamStrategy, &out.ProvidedDownstreamStrategy
		*out = new(ProvidedDownstreamStrategy)
		(*in).DeepCopyInto(*out)
	}
	if in.ManagedDownstreamStrategy != nil {
		in, out := &in.ManagedDownstreamStrategy, &out.ManagedDownstreamStrategy
		*out = new(ManagedDownstreamStrategy)
		**out = **in
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new DownstreamStrategy.
func (in *DownstreamStrategy) DeepCopy() *DownstreamStrategy {
	if in == nil {
		return nil
	}
	out := new(DownstreamStrategy)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *ExternalDownstreamStrategy) DeepCopyInto(out *ExternalDownstreamStrategy) {
	*out = *in
	out.Credentials = in.Credentials
	if in.Certificate != nil {
		in, out := &in.Certificate, &out.Certificate
		*out = make([]byte, len(*in))
		copy(*out, *in)
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new ExternalDownstreamStrategy.
func (in *ExternalDownstreamStrategy) DeepCopy() *ExternalDownstreamStrategy {
	if in == nil {
		return nil
	}
	out := new(ExternalDownstreamStrategy)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *ImageProperties) DeepCopyInto(out *ImageProperties) {
	*out = *in
	if in.Repository != nil {
		in, out := &in.Repository, &out.Repository
		*out = new(string)
		**out = **in
	}
	if in.UseImageStream != nil {
		in, out := &in.UseImageStream, &out.UseImageStream
		*out = new(bool)
		**out = **in
	}
	if in.PullPolicy != nil {
		in, out := &in.PullPolicy, &out.PullPolicy
		*out = new(v1.PullPolicy)
		**out = **in
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new ImageProperties.
func (in *ImageProperties) DeepCopy() *ImageProperties {
	if in == nil {
		return nil
	}
	out := new(ImageProperties)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *IoTConfig) DeepCopyInto(out *IoTConfig) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ObjectMeta.DeepCopyInto(&out.ObjectMeta)
	in.Spec.DeepCopyInto(&out.Spec)
	out.Status = in.Status
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new IoTConfig.
func (in *IoTConfig) DeepCopy() *IoTConfig {
	if in == nil {
		return nil
	}
	out := new(IoTConfig)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *IoTConfig) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *IoTConfigList) DeepCopyInto(out *IoTConfigList) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	out.ListMeta = in.ListMeta
	if in.Items != nil {
		in, out := &in.Items, &out.Items
		*out = make([]IoTConfig, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new IoTConfigList.
func (in *IoTConfigList) DeepCopy() *IoTConfigList {
	if in == nil {
		return nil
	}
	out := new(IoTConfigList)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *IoTConfigList) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *IoTConfigSpec) DeepCopyInto(out *IoTConfigSpec) {
	*out = *in
	if in.Adapters != nil {
		in, out := &in.Adapters, &out.Adapters
		*out = make([]AdapterSpec, len(*in))
		copy(*out, *in)
	}
	in.DefaultImageProperties.DeepCopyInto(&out.DefaultImageProperties)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new IoTConfigSpec.
func (in *IoTConfigSpec) DeepCopy() *IoTConfigSpec {
	if in == nil {
		return nil
	}
	out := new(IoTConfigSpec)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *IoTConfigStatus) DeepCopyInto(out *IoTConfigStatus) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new IoTConfigStatus.
func (in *IoTConfigStatus) DeepCopy() *IoTConfigStatus {
	if in == nil {
		return nil
	}
	out := new(IoTConfigStatus)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *IoTProject) DeepCopyInto(out *IoTProject) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ObjectMeta.DeepCopyInto(&out.ObjectMeta)
	in.Spec.DeepCopyInto(&out.Spec)
	in.Status.DeepCopyInto(&out.Status)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new IoTProject.
func (in *IoTProject) DeepCopy() *IoTProject {
	if in == nil {
		return nil
	}
	out := new(IoTProject)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *IoTProject) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *IoTProjectList) DeepCopyInto(out *IoTProjectList) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	out.ListMeta = in.ListMeta
	if in.Items != nil {
		in, out := &in.Items, &out.Items
		*out = make([]IoTProject, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new IoTProjectList.
func (in *IoTProjectList) DeepCopy() *IoTProjectList {
	if in == nil {
		return nil
	}
	out := new(IoTProjectList)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *IoTProjectList) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *IoTProjectSpec) DeepCopyInto(out *IoTProjectSpec) {
	*out = *in
	in.DownstreamStrategy.DeepCopyInto(&out.DownstreamStrategy)
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new IoTProjectSpec.
func (in *IoTProjectSpec) DeepCopy() *IoTProjectSpec {
	if in == nil {
		return nil
	}
	out := new(IoTProjectSpec)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *IoTProjectStatus) DeepCopyInto(out *IoTProjectStatus) {
	*out = *in
	if in.DownstreamEndpoint != nil {
		in, out := &in.DownstreamEndpoint, &out.DownstreamEndpoint
		*out = new(ExternalDownstreamStrategy)
		(*in).DeepCopyInto(*out)
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new IoTProjectStatus.
func (in *IoTProjectStatus) DeepCopy() *IoTProjectStatus {
	if in == nil {
		return nil
	}
	out := new(IoTProjectStatus)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *ManagedDownstreamStrategy) DeepCopyInto(out *ManagedDownstreamStrategy) {
	*out = *in
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new ManagedDownstreamStrategy.
func (in *ManagedDownstreamStrategy) DeepCopy() *ManagedDownstreamStrategy {
	if in == nil {
		return nil
	}
	out := new(ManagedDownstreamStrategy)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *ProvidedDownstreamStrategy) DeepCopyInto(out *ProvidedDownstreamStrategy) {
	*out = *in
	out.Credentials = in.Credentials
	if in.EndpointMode != nil {
		in, out := &in.EndpointMode, &out.EndpointMode
		*out = new(EndpointMode)
		**out = **in
	}
	if in.TLS != nil {
		in, out := &in.TLS, &out.TLS
		*out = new(bool)
		**out = **in
	}
	return
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new ProvidedDownstreamStrategy.
func (in *ProvidedDownstreamStrategy) DeepCopy() *ProvidedDownstreamStrategy {
	if in == nil {
		return nil
	}
	out := new(ProvidedDownstreamStrategy)
	in.DeepCopyInto(out)
	return out
}
