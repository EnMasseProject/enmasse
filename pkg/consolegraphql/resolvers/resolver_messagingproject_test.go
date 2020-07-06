/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/99designs/gqlgen/graphql"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/fake"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/accesscontroller"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"testing"
)

func newTestMessagingProjectResolver(t *testing.T) (*Resolver, context.Context) {
	objectCache, err := cache.CreateObjectCache()
	assert.NoError(t, err)

	clientset := fake.NewSimpleClientset(&v1beta1.MessagingProject{})

	resolver := Resolver{}
	resolver.Cache = objectCache

	requestState := &server.RequestState{
		AccessController:     accesscontroller.NewAllowAllAccessController(),
		EnmasseV1beta1Client: clientset.EnmasseV1beta1(),
	}

	ctx := graphql.WithResponseContext(server.ContextWithRequestState(requestState, context.TODO()),
		graphql.DefaultErrorPresenter,
		graphql.DefaultRecover)

	return &resolver, ctx
}

func TestQueryMessagingProject(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	as := createMessagingProject("mynamespace", "myaddressspace")
	err := r.Cache.Add(as)
	assert.NoError(t, err)

	objs, err := r.Query().MessagingProjects(ctx, nil, nil, nil, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of address spaces")

	assert.Equal(t, as.Spec, objs.MessagingProjects[0].Spec, "Unexpected address space spec")
	assert.Equal(t, as.ObjectMeta, objs.MessagingProjects[0].ObjectMeta, "Unexpected address space object meta")
}

func TestQueryMessagingProjectFilter(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	as1 := createMessagingProject("mynamespace1", "myaddressspace")
	as2 := createMessagingProject("mynamespace2", "myaddressspace")
	err := r.Cache.Add(as1, as2)
	assert.NoError(t, err)

	filter := fmt.Sprintf("`$.ObjectMeta.Name` = '%s'", as1.ObjectMeta.Name)
	objs, err := r.Query().MessagingProjects(ctx, nil, nil, &filter, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of address spaces")

	assert.Equal(t, as1.ObjectMeta, objs.MessagingProjects[0].ObjectMeta, "Unexpected address space object meta")
}

func TestQueryMessagingProjectOrder(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	as1 := createMessagingProject("mynamespace1", "myaddressspace")
	as2 := createMessagingProject("mynamespace2", "myaddressspace")
	err := r.Cache.Add(as1, as2)
	assert.NoError(t, err)

	orderby := "`$.ObjectMeta.Name` DESC"
	objs, err := r.Query().MessagingProjects(ctx, nil, nil, nil, &orderby)
	assert.NoError(t, err)

	expected := 2
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of address spaces")

	assert.Equal(t, as2.ObjectMeta, objs.MessagingProjects[0].ObjectMeta, "Unexpected address space object meta")
	assert.Equal(t, as1.ObjectMeta, objs.MessagingProjects[1].ObjectMeta, "Unexpected address space object meta")
}

func TestQueryMessagingProjectPagination(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	as1 := createMessagingProject("mynamespace1", "myaddressspace")
	as2 := createMessagingProject("mynamespace2", "myaddressspace")
	as3 := createMessagingProject("mynamespace3", "myaddressspace")
	as4 := createMessagingProject("mynamespace4", "myaddressspace")
	err := r.Cache.Add(as1, as2, as3, as4)
	assert.NoError(t, err)

	objs, err := r.Query().MessagingProjects(ctx, nil, nil, nil, nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of address spaces")

	one := 1
	two := 2
	objs, err = r.Query().MessagingProjects(ctx, nil, &one, nil, nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of address spaces")
	assert.Equal(t, 3, len(objs.MessagingProjects), "Unexpected number of address spaces in page")
	assert.Equal(t, as2.ObjectMeta, objs.MessagingProjects[0].ObjectMeta, "Unexpected address space object meta")

	objs, err = r.Query().MessagingProjects(ctx, &one, &two, nil, nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of address spaces")
	assert.Equal(t, 1, len(objs.MessagingProjects), "Unexpected number of address spaces in page")
	assert.Equal(t, as3.ObjectMeta, objs.MessagingProjects[0].ObjectMeta, "Unexpected address space object meta")
}

func TestQueryMessagingProjectConnections(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"
	con1 := createConnection("host:1234", namespace, addressspace)
	// Different address space, should not be found
	con2 := createConnection("host:1234", namespace, "myaddressspace1")
	err := r.Cache.Add(con1, con2)
	assert.NoError(t, err)

	ash := &consolegraphql.MessagingProjectHolder{
		MessagingProject: v1beta1.MessagingProject{
			ObjectMeta: metav1.ObjectMeta{
				Name:      addressspace,
				Namespace: namespace,
			},
		},
	}
	objs, err := r.MessagingProject_consoleapi_enmasse_io_v1beta1().Connections(ctx, ash, nil, nil, nil, nil)
	assert.NoError(t, err)
	assert.Equal(t, 1, objs.Total, "Unexpected number of connections associated with the addressspace")
}

func TestQueryMessagingProjectConnectionFilter(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"
	con1 := createConnection("host:1234", namespace, addressspace)
	con2 := createConnection("host:1235", namespace, addressspace)
	err := r.Cache.Add(con1, con2)
	assert.NoError(t, err)

	ash := &consolegraphql.MessagingProjectHolder{
		MessagingProject: v1beta1.MessagingProject{
			ObjectMeta: metav1.ObjectMeta{
				Name:      addressspace,
				Namespace: namespace,
			},
		},
	}

	filter := fmt.Sprintf("`$.ObjectMeta.Name` = '%s'", con2.ObjectMeta.Name)
	objs, err := r.MessagingProject_consoleapi_enmasse_io_v1beta1().Connections(ctx, ash, nil, nil, &filter, nil)
	assert.NoError(t, err)
	assert.Equal(t, 1, objs.Total, "Unexpected number of filtered connections associated with the addressspace")
	assert.Equal(t, con2, objs.Connections[0], "Unexpected connection")
}

func TestQueryMessagingProjectConnectionOrder(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"
	con1 := createConnection("host:1234", namespace, addressspace)
	con2 := createConnection("host:1235", namespace, addressspace)
	err := r.Cache.Add(con1, con2)
	assert.NoError(t, err)

	ash := &consolegraphql.MessagingProjectHolder{
		MessagingProject: v1beta1.MessagingProject{
			ObjectMeta: metav1.ObjectMeta{
				Name:      addressspace,
				Namespace: namespace,
			},
		},
	}

	orderby := "`$.ObjectMeta.Name` DESC"
	objs, err := r.MessagingProject_consoleapi_enmasse_io_v1beta1().Connections(ctx, ash, nil, nil, nil, &orderby)
	assert.NoError(t, err)
	assert.Equal(t, 2, objs.Total, "Unexpected number of connections associated with the addressspace")
	assert.Equal(t, con2, objs.Connections[0], "Unexpected connection")
}

func TestQueryMessagingProjectAddress(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"
	addr := createAddress(namespace, fmt.Sprintf("%s.addr1", addressspace))
	err := r.Cache.Add(addr)
	assert.NoError(t, err)

	ash := &consolegraphql.MessagingProjectHolder{
		MessagingProject: v1beta1.MessagingProject{
			ObjectMeta: metav1.ObjectMeta{
				Name:      addressspace,
				Namespace: namespace,
			},
		},
	}
	objs, err := r.MessagingProject_consoleapi_enmasse_io_v1beta1().Addresses(ctx, ash, nil, nil, nil, nil)
	assert.NoError(t, err)
	assert.Equal(t, 1, objs.Total, "Unexpected number of address associated with the addressspace")
}

func TestQueryMessagingProjectAddressFilter(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"
	addr1 := createAddress(namespace, fmt.Sprintf("%s.addr1", addressspace))
	addr2 := createAddress(namespace, fmt.Sprintf("%s.addr2", addressspace))
	err := r.Cache.Add(addr1, addr2)
	assert.NoError(t, err)

	ash := &consolegraphql.MessagingProjectHolder{
		MessagingProject: v1beta1.MessagingProject{
			ObjectMeta: metav1.ObjectMeta{
				Name:      addressspace,
				Namespace: namespace,
			},
		},
	}
	filter := fmt.Sprintf("`$.ObjectMeta.Name` = '%s'", addr2.ObjectMeta.Name)
	objs, err := r.MessagingProject_consoleapi_enmasse_io_v1beta1().Addresses(ctx, ash, nil, nil, &filter, nil)
	assert.NoError(t, err)
	assert.Equal(t, 1, objs.Total, "Unexpected number of address associated with the addressspace")
	assert.Equal(t, 1, objs.Total, "Unexpected number of filtered addresses associated with the addressspace")
	assert.Equal(t, addr2, objs.Addresses[0], "Unexpected connection")
}

func TestQueryMessagingProjectCommand(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	as := createMessagingProject("myaddressspace", "mynamespace")
	auth := "auth"
	as.Spec = v1beta1.MessagingProjectSpec{
		AuthenticationService: &v1beta1.AuthenticationService{
			Name: auth,
		},
		Plan: "standard-small-queue",
		Type: "queue",
	}
	obj, err := r.Query().MessagingProjectCommand(ctx, as.MessagingProject)
	assert.NoError(t, err)

	expectedMetaData := `kind: MessagingProject
metadata:
  name: myaddressspace`
	expectedSpec := `
spec:
  authenticationService:
    name: auth
  plan: standard-small-queue
  type: queue`
	assert.Contains(t, obj, expectedMetaData, "Expect name and namespace to be set")
	assert.Contains(t, obj, expectedSpec, "Expect spec to be set")
}

func TestQueryMessagingProjectCommandWithEndpoint(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	as := createMessagingProject("myaddressspace", "mynamespace")
	auth := "auth"
	as.Spec = v1beta1.MessagingProjectSpec{
		Plan: "standard-small-queue",
		Type: "queue",
		AuthenticationService: &v1beta1.AuthenticationService{
			Name: auth,
		},
		Endpoints: []v1beta1.EndpointSpec{
			{
				Name:    "myendpoint",
				Service: v1beta1.EndpointServiceTypeMessaging,
				Certificate: &v1beta1.CertificateSpec{
					Provider: v1beta1.CertificateProviderTypeCertSelfsigned,
				},
				Expose: &v1beta1.ExposeSpec{
					Type:                v1beta1.ExposeTypeRoute,
					RouteHost:           "myhost.example.com",
					RouteServicePort:    v1beta1.RouteServicePortAmqps,
					RouteTlsTermination: v1beta1.RouteTlsTerminationPassthrough,
				},
			},
		},
	}
	obj, err := r.Query().MessagingProjectCommand(ctx, as.MessagingProject)
	assert.NoError(t, err)

	expectedMetaData := `kind: MessagingProject
metadata:
  name: myaddressspace`
	expectedSpec := `
spec:
  authenticationService:
    name: auth
  endpoints:
  - cert:
      provider: selfsigned
    expose:
      routeHost: myhost.example.com
      routeServicePort: amqps
      routeTlsTermination: passthrough
      type: route
    name: myendpoint
    service: messaging
  plan: standard-small-queue
  type: queue`
	assert.Contains(t, obj, expectedMetaData, "Expect name and namespace to be set")
	assert.Contains(t, obj, expectedSpec, "Expect spec to be set")
}

func TestDeleteMessagingProjects(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	namespace := "mynamespace"
	as1 := createMessagingProject("myaddressspace1", namespace)
	as2 := createMessagingProject("myaddressspace2", namespace)

	addrClient := server.GetRequestStateFromContext(ctx).EnmasseV1beta1Client.MessagingProjects(namespace)
	_, err := addrClient.Create(&as1.MessagingProject)
	assert.NoError(t, err)
	_, err = addrClient.Create(&as2.MessagingProject)
	assert.NoError(t, err)

	_, err = r.Mutation().DeleteMessagingProjects(ctx, []*metav1.ObjectMeta{&as1.ObjectMeta, &as2.ObjectMeta})
	assert.NoError(t, err)
	assert.Equal(t, 0, len(graphql.GetErrors(ctx)))

	list, err := addrClient.List(metav1.ListOptions{})
	assert.NoError(t, err)
	assert.Equal(t, 0, len(list.Items))
}

func TestDeleteMessagingProjectsOneMessagingProjectNotFound(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	namespace := "mynamespace"
	as1 := createMessagingProject("myaddressspace1", namespace)
	as2 := createMessagingProject("myaddressspace2", namespace)
	absent := createMessagingProject("absent", namespace)

	addrClient := server.GetRequestStateFromContext(ctx).EnmasseV1beta1Client.MessagingProjects(namespace)
	_, err := addrClient.Create(&as1.MessagingProject)
	assert.NoError(t, err)
	_, err = addrClient.Create(&as2.MessagingProject)
	assert.NoError(t, err)

	_, err = r.Mutation().DeleteMessagingProjects(ctx, []*metav1.ObjectMeta{&as1.ObjectMeta, &absent.ObjectMeta, &as2.ObjectMeta})
	assert.NoError(t, err)
	assert.Equal(t, 1, len(graphql.GetErrors(ctx)))
	assert.Contains(t, graphql.GetErrors(ctx)[0].Message, "failed to delete address space: 'absent' in namespace: 'mynamespace'")

	list, err := addrClient.List(metav1.ListOptions{})
	assert.NoError(t, err)
	assert.Equal(t, 0, len(list.Items))
}

func TestCreateMessagingProject(t *testing.T) {
	r, ctx := newTestMessagingProjectResolver(t)
	as := createMessagingProject("myaddressspace", "mynamespace")
	auth := "auth"
	as.Spec = v1beta1.MessagingProjectSpec{
		Plan: "standard-small-queue",
		Type: "queue",
		AuthenticationService: &v1beta1.AuthenticationService{
			Name: auth,
		},
		Endpoints: []v1beta1.EndpointSpec{
			{
				Name:    "myendpoint",
				Service: v1beta1.EndpointServiceTypeMessaging,
				Certificate: &v1beta1.CertificateSpec{
					Provider: v1beta1.CertificateProviderTypeCertSelfsigned,
				},
				Expose: &v1beta1.ExposeSpec{
					Type:                v1beta1.ExposeTypeRoute,
					RouteHost:           "myhost.example.com",
					RouteServicePort:    v1beta1.RouteServicePortAmqps,
					RouteTlsTermination: v1beta1.RouteTlsTerminationPassthrough,
				},
			},
		},
	}
	obj, err := r.Mutation().CreateMessagingProject(ctx, as.MessagingProject)
	assert.NoError(t, err)
	assert.Equal(t, as.Namespace, obj.Namespace)
	assert.Equal(t, as.Name, obj.Name)

	addrClient := server.GetRequestStateFromContext(ctx).EnmasseV1beta1Client.MessagingProjects(as.Namespace)
	retrieved, err := addrClient.Get(as.Name, metav1.GetOptions{})
	assert.NoError(t, err)
	assert.Equal(t, "standard-small-queue", retrieved.Spec.Plan)
	assert.Equal(t, 1, len(retrieved.Spec.Endpoints))
	assert.Equal(t, "myendpoint", retrieved.Spec.Endpoints[0].Name)
	assert.NotNil(t, retrieved.Spec.Endpoints[0].Expose)
	assert.Equal(t, v1beta1.ExposeTypeRoute, retrieved.Spec.Endpoints[0].Expose.Type)
}
