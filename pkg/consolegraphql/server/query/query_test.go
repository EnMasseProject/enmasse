/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package query

import (
	"github.com/alexedwards/scs/v2"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/fake"
	v1beta1api "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/resolvers"
	userv1 "github.com/openshift/client-go/user/clientset/versioned/typed/user/v1"
	"github.com/stretchr/testify/assert"
	"k8s.io/client-go/kubernetes"
	restclient "k8s.io/client-go/rest"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func setUp() (http.Handler, v1beta1api.EnmasseV1beta1Interface) {
	clientset := fake.NewSimpleClientset(&v1beta1.Address{})
	enmasseClientSet := clientset.EnmasseV1beta1()

	sessionManager := scs.New()

	queryTime, queryError, _ := server.CreateMetrics()

	setCreator := func(config *restclient.Config) (kubeClient kubernetes.Interface, userClient userv1.UserV1Interface, coreClient v1beta1api.EnmasseV1beta1Interface, err error) {

		return nil, nil, enmasseClientSet, nil
	}

	queryServer := CreateQueryServer(resolvers.Resolver{},
		false, false, sessionManager, queryError, queryTime,
		nil, nil, setCreator)

	return sessionManager.LoadAndSave(queryServer), enmasseClientSet
}

func TestWhoAmI(t *testing.T) {
	queryServer, _ := setUp()

	resp := post(queryServer, nil, `{"query": "query whoami { whoami { metadata { name } } }"}`)
	assert.NotNil(t, resp)
	assert.Equal(t, http.StatusOK, resp.Code)
	assert.Equal(t, `{"data":{"whoami":{"metadata":{"name":"foouser"}}}}`, resp.Body.String())
	assert.Equal(t, 1, len(resp.Result().Cookies()))
}

func TestMutationWithoutExistingSessionRejected(t *testing.T) {
	queryServer, _ := setUp()

	resp := post(queryServer, nil, `{"query": "mutation delAddr($addrs:[ObjectMeta_v1_Input!]!) { deleteAddresses(input:$addrs) }", "variables" : { "addrs": [{"name": "cbf3d7c5-e39a-54c5-8328-2bb6f24d3010", "namespace": "enmasse-infra" }] }}`)
	assert.NotNil(t, resp)
	assert.Equal(t, http.StatusOK, resp.Code)
	assert.Equal(t, `{"errors":[{"message":"unable to invoke mutation delAddr at this time"}],"data":null}`, resp.Body.String())
	assert.Equal(t, 1, len(resp.Result().Cookies()))
}

func TestMutationWithExistingSessionAllowed(t *testing.T) {
	queryServer, client := setUp()

	_, err := client.Addresses("myns").Create(&v1beta1.Address{
		ObjectMeta: v1.ObjectMeta{
			Name: "foo",
		},
	})
	assert.NoError(t, err)

	// first non mutation request will establish the session
	resp := post(queryServer, nil, `{"query": "query whoami { whoami { metadata { name } } }"}`)
	assert.NotNil(t, resp)
	assert.Equal(t, http.StatusOK, resp.Code)
	cookies := resp.Result().Cookies()
	assert.Equal(t, 1, len(cookies))

	resp = post(queryServer, cookies, `{"query": "mutation del($addrs:[ObjectMeta_v1_Input!]!) { deleteAddresses(input:$addrs) }", "variables" : { "addrs": [{"name": "foo", "namespace": "myns" }] }}`)
	assert.NotNil(t, resp)
	assert.Equal(t, http.StatusOK, resp.Code)
	assert.Equal(t, `{"data":{"deleteAddresses":true}}`, resp.Body.String())
}

func post(handler http.Handler, cookies []*http.Cookie, body string) *httptest.ResponseRecorder {
	r := httptest.NewRequest("POST", "/graphql/query", strings.NewReader(body))
	r.Header.Set("Content-Type", "application/json")
	r.Header.Set("Authorization", "Bearer: foo")
	r.Header.Set("X-Forwarded-Preferred-Username", "foouser")
	if cookies != nil {
		for _, c := range cookies {
			r.AddCookie(c)
		}
	}
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, r)
	return w
}
