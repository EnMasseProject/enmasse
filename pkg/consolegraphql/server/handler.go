/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package server

import (
	"bytes"
	"context"
	"crypto/sha256"
	"github.com/alexedwards/scs/v2"
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/accesscontroller"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/openshift/client-go/user/clientset/versioned/typed/user/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/tools/clientcmd"
	"k8s.io/client-go/tools/clientcmd/api"
	"log"
	"net/http"
	"strconv"
)

const accessControllerStateCookieName = "accessControllerState"
const sessionOwnerSessionAttribute = "sessionOwnerSessionAttribute"
const loggedOnUserSessionAttribute = "loggedOnUserSessionAttribute"

func AuthHandler(next http.Handler, sessionManager *scs.SessionManager) http.Handler {

	return http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		var state *RequestState

		accessToken := req.Header.Get("X-Forwarded-Access-Token")
		useSession := true
		if healthProbe, _ := strconv.ParseBool(req.Header.Get("X-Health")); healthProbe {
			useSession = false
		}

		if accessToken == "" {
			http.Error(rw, "No access token", http.StatusUnauthorized)
			rw.WriteHeader(401)
			return
		}

		accessTokenSha := getShaSum(accessToken)

		if useSession {
			if sessionManager.Exists(req.Context(), sessionOwnerSessionAttribute) {
				sessionOwnerAccessTokenSha := sessionManager.Get(req.Context(), sessionOwnerSessionAttribute).([]byte)
				if !bytes.Equal(sessionOwnerAccessTokenSha, accessTokenSha) {
					// This session must have belonged to a different accessToken, destroy it.
					// New session created automatically.
					_ = sessionManager.Destroy(req.Context())
					sessionManager.Put(req.Context(), sessionOwnerSessionAttribute, accessTokenSha)
				}
			} else {
				sessionManager.Put(req.Context(), sessionOwnerSessionAttribute, accessTokenSha)
			}
		}

		kubeConfig := clientcmd.NewNonInteractiveDeferredLoadingClientConfig(
			clientcmd.NewDefaultClientConfigLoadingRules(),
			&clientcmd.ConfigOverrides{
				AuthInfo: api.AuthInfo{
					Token: accessToken,
				},
			},
		)

		config, err := kubeConfig.ClientConfig()

		//config.WrapTransport = func(rt http.RoundTripper) http.RoundTripper {
		//	return &server.Tracer{RoundTripper: rt}
		//}

		kubeClient, err := kubernetes.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		userClient, err := v1.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		coreClient, err := v1beta1.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		loggedOnUser := getLoggedOnUser(useSession, sessionManager, req, userClient)

		var accessControllerState interface{}
		if useSession {
			accessControllerState = sessionManager.Get(req.Context(), accessControllerStateCookieName)
		}

		controller := accesscontroller.NewKubernetesRBACAccessController(kubeClient, accessControllerState)

		state = &RequestState{
			UserInterface:        userClient.Users(),
			EnmasseV1beta1Client: coreClient,
			AccessController:     controller,
			User:                 loggedOnUser,
			UserAccessToken:      accessToken,
			UseSession:           useSession,
		}

		ctx := ContextWithRequestState(state, req.Context())

		next.ServeHTTP(rw, req.WithContext(ctx))
	})
}

func DevelopmentHandler(next http.Handler, sessionManager *scs.SessionManager, accessToken string) http.Handler {
	return http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {

		kubeconfig := clientcmd.NewNonInteractiveDeferredLoadingClientConfig(
			clientcmd.NewDefaultClientConfigLoadingRules(),
			&clientcmd.ConfigOverrides{},
		)

		config, err := kubeconfig.ClientConfig()

		if err != nil {
			log.Printf("Failed to build config : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		userclient, err := v1.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		coreClient, err := v1beta1.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		loggedOnUser := getLoggedOnUser(true, sessionManager, req, userclient)

		requestState := &RequestState{
			UserInterface:        userclient.Users(),
			EnmasseV1beta1Client: coreClient,
			AccessController:     accesscontroller.NewAllowAllAccessController(),
			User:                 loggedOnUser,
			UserAccessToken:      accessToken,
			UseSession:           true,
		}

		ctx := ContextWithRequestState(requestState, req.Context())
		next.ServeHTTP(rw, req.WithContext(ctx))
	})
}

func UpdateAccessControllerState(ctx context.Context, loggedOnUser string, sessionManager *scs.SessionManager) string {
	requestState := GetRequestStateFromContext(ctx)
	if requestState != nil && requestState.UseSession {
		loggedOnUser = requestState.User
		if updated, accessControllerState := requestState.AccessController.GetState(); updated {
			if accessControllerState == nil {
				sessionManager.Remove(ctx, accessControllerStateCookieName)
			} else {
				sessionManager.Put(ctx, accessControllerStateCookieName, accessControllerState)
			}
		}
	}
	return loggedOnUser
}

func getLoggedOnUser(useSession bool, sessionManager *scs.SessionManager, req *http.Request, userClient *v1.UserV1Client) string {
	loggedOnUser := "<unknown>"
	if useSession {
		if sessionManager.Exists(req.Context(), loggedOnUserSessionAttribute) {
			loggedOnUser = sessionManager.GetString(req.Context(), loggedOnUserSessionAttribute)
		} else {
			if util.HasApi(util.UserGVK) {
				usr, err := userClient.Users().Get("~", metav1.GetOptions{})
				if err == nil {
					loggedOnUser = usr.ObjectMeta.Name
					sessionManager.Put(req.Context(), loggedOnUserSessionAttribute, loggedOnUser)
				}
			}
		}
	} else {
		if util.HasApi(util.UserGVK) {
			usr, err := userClient.Users().Get("~", metav1.GetOptions{})
			if err == nil {
				loggedOnUser = usr.ObjectMeta.Name
			}
		}
	}
	return loggedOnUser
}

func getShaSum(accessToken string) []byte {
	accessTokenSha := sha256.Sum256([]byte(accessToken))
	return accessTokenSha[:]
}
