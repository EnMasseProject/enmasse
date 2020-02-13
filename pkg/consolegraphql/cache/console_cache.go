/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package cache

import (
	"fmt"
	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	v1 "k8s.io/api/core/v1"
	"strings"
)

const PrimaryObjectIndex = "id"
const AddressLinkObjectIndex = "addressLinkHierarchy"

func CreateObjectCache() (*MemdbCache, error) {
	c := &MemdbCache{}
	err := c.Init(
		IndexSpecifier{
			Name: PrimaryObjectIndex,
			Indexer: &hierarchyIndex{
				keyCreator: primaryUniqueKeyCreator,
			},
		},
		IndexSpecifier{
			Name:         AddressLinkObjectIndex,
			AllowMissing: true,
			Indexer: &hierarchyIndex{
				keyCreator: addressLinkKeyCreator,
			},
		})
	return c, err
}

func primaryUniqueKeyCreator(obj interface{}) (b bool, s string, err error) {
	switch o := obj.(type) {
	case *v1.Namespace:
		return true, o.Kind + "/" + o.Name, nil
	case *consolegraphql.AddressSpaceHolder:
		return true, o.Kind + "/" + o.Namespace + "/" + o.Name, nil
	case *consolegraphql.AddressHolder:
		i := strings.Index(o.Name, ".")
		if i < 0 {
			return false, "", fmt.Errorf("unexpected address name formation '%s', expected dot separator", o.Name)
		}
		addressSpaceName := o.Name[:i]
		return true, o.Kind + "/" + o.Namespace + "/" + addressSpaceName + "/" + o.Name, nil
	case *consolegraphql.Connection:
		return true, o.Kind + "/" + o.Namespace + "/" + o.Spec.AddressSpace + "/" + o.Name, nil
	case *consolegraphql.Link:
		return true, o.Kind + "/" + o.Namespace + "/" + o.Spec.AddressSpace + "/" + o.Spec.Connection + "/" + o.ObjectMeta.Name, nil
	case *v1beta2.AddressPlan:
		return true, o.Kind + "/" + o.Name, nil
	case *v1beta2.AddressSpacePlan:
		return true, o.Kind + "/" + o.Name, nil
	case *v1beta1.AddressSpaceSchema:
		return true, o.Kind + "/" + o.Name, nil
	case *adminv1beta1.AuthenticationService:
		return true, o.Kind + "/" + o.Name, nil
	}
	return false, "", nil
}

func addressLinkKeyCreator(obj interface{}) (b bool, s string, err error) {
	switch o := obj.(type) {
	case *consolegraphql.Link:
		return true, o.Kind + "/" + o.Namespace + "/" + o.Spec.AddressSpace + "/" + o.Spec.Address + "/" + o.ObjectMeta.Name, nil
	}
	return false, "", nil
}
