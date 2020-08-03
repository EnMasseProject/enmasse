package crds

import (
	"fmt"
	"github.com/RHsyseng/operator-utils/pkg/validation"
	admin2 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta2"
	enmassev1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	enmasse "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	iot "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	user "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
	"github.com/stretchr/testify/assert"
	"io/ioutil"
	"strings"
	"testing"
)

func TestIotCompleteCRD(t *testing.T) {
	root := "../../../../templates/crds"
	crdStructMap := map[string]interface{}{
		"iotinfrastructure.crd.yaml": &iot.IoTInfrastructure{},
		"iottenants.crd.yaml":        &iot.IoTTenant{},
	}
	_, _ = root, crdStructMap
	for crd, obj := range crdStructMap {
		t.Run(crd, func(t2 *testing.T) {
			schema := getSchema(t2, fmt.Sprintf("%s/%s", root, crd))
			schema.GetMissingEntries(obj)
			//not validated - too many omissions
		})
	}
}

func TestEnmasseCompleteCRD(t *testing.T) {
	root := "../../../../templates/crds"
	crdStructMap := map[string]interface{}{
		"enmasse.io_messagingaddresses.yaml":       &enmassev1.MessagingAddress{},
		"enmasse.io_messagingaddressplans.yaml":    &enmassev1.MessagingAddressPlan{},
		"enmasse.io_messagingendpoints.yaml":       &enmassev1.MessagingEndpoint{},
		"enmasse.io_messaginginfrastructures.yaml": &enmassev1.MessagingInfrastructure{},
		"enmasse.io_messagingplans.yaml":           &enmassev1.MessagingPlan{},
		"enmasse.io_messagingprojects.yaml":        &enmassev1.MessagingProject{},
		"messagingusers.crd.yaml":                  &user.MessagingUser{},
	}
	_, _ = root, crdStructMap
	for crd, obj := range crdStructMap {
		t.Run(crd, func(t2 *testing.T) {
			schema := getSchema(t2, fmt.Sprintf("%s/%s", root, crd))

			missingEnt := schema.GetMissingEntries(obj)
			for _, missing := range missingEnt {
				if strings.HasPrefix(missing.Path, "/status") {
					//status not validated
				} else if strings.Contains(missing.Path, "/password") {
					//password not validated
				} else {
					assert.Fail(t, "Discrepancy between CRD and Struct", "Missing or incorrect schema validation at %v, expected type %v", missing.Path, missing.Type)
				}
			}
		})
	}
}

func TestCompleteCRD(t *testing.T) {
	root := "../../../../templates/crds"
	crdStructMap := map[string]interface{}{
		"addressplans.crd.yaml":           &admin2.AddressPlan{},
		"addressspaceplans.crd.yaml":      &admin2.AddressSpacePlan{},
		"addresses.crd.yaml":              &enmasse.Address{},
		//below are commented out - too many omissions
		//"addressspaces.crd.yaml":          &enmasse.AddressSpace{},
		//"addressspaceschemas.crd.yaml":    &enmasse.AddressSpaceSchema{},
		//"authenticationservices.crd.yaml": &admin1.AuthenticationService{},
		//"consoleservices.crd.yaml":        &admin1.ConsoleService{},
	}
	_, _ = root, crdStructMap
	for crd, obj := range crdStructMap {
		t.Run(crd, func(t2 *testing.T) {
			schema := getSchema(t2, fmt.Sprintf("%s/%s", root, crd))

			missingEnt := schema.GetMissingEntries(obj)
			for _, missing := range missingEnt {
				if strings.HasPrefix(missing.Path, "/status") {
					//status not validated
				} else if strings.HasPrefix(missing.Path,"/spec/addressSpace") {
					//not validated
				} else {
					assert.Fail(t, "Discrepancy between CRD and Struct", "Missing or incorrect schema validation at %v, expected type %v", missing.Path, missing.Type)
				}
			}
		})
	}
}

func getSchema(t *testing.T, crd string) validation.Schema {
	bytes, err := ioutil.ReadFile(crd)
	assert.NoError(t, err, "Error reading CRD yaml from %v", crd)
	schema, err := validation.New(bytes)
	assert.NoError(t, err)
	return schema
}
