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
	"encoding/gob"
	"log"
	"net/http"
	"regexp"
	"strconv"

	"github.com/alexedwards/scs/v2"
	v1 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/accesscontroller"
	"github.com/enmasseproject/enmasse/pkg/util"
	userapiv1 "github.com/openshift/api/user/v1"
	userv1 "github.com/openshift/client-go/user/clientset/versioned/typed/user/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	restclient "k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
	"k8s.io/client-go/tools/clientcmd/api"
)

const accessControllerStateCookieName = "accessControllerState"
const sessionOwnerSessionAttribute = "sessionOwnerSessionAttribute"
const loggedOnUserSessionAttribute = "loggedOnUserSessionAttribute"

var bearerRegexp = regexp.MustCompile(`^Bearer +(.*)$`)

const forwardedHeader = "X-Forwarded-Access-Token"
const forwardedUserHeader = "X-Forwarded-User"
const forwardedEmailHeader = "X-Forwarded-Email"
const forwardedPreferredHeader = "X-Forwarded-Preferred-Username"
const authHeader = "Authorization"

type ImpersonationConfig struct {
	UserHeader *string
}

func getImpersonatedUser(req *http.Request, impersonationConfig *ImpersonationConfig) string {
	if impersonationConfig != nil {
		userHeader := forwardedUserHeader
		if impersonationConfig.UserHeader != nil {
			userHeader = *impersonationConfig.UserHeader
		}
		return req.Header.Get(userHeader)
	}
	return ""
}

func AuthHandler(next http.Handler, sessionManager *scs.SessionManager, impersonationConfig *ImpersonationConfig) http.Handler {
	gob.Register(userapiv1.User{})

	return http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		var state *RequestState

		useSession := true
		if healthProbe, _ := strconv.ParseBool(req.Header.Get("X-Health")); healthProbe {
			useSession = false
		}

		accessToken := getAccessToken(req)
		if accessToken == "" {
			http.Error(rw, "No access token", http.StatusUnauthorized)
			rw.WriteHeader(401)
			return
		}

		impersonatedUser := getImpersonatedUser(req, impersonationConfig)

		sessionSha, err := getShaSum(accessToken, impersonatedUser)

		if err != nil {
			http.Error(rw, "Error computing SHA256 digest", http.StatusInternalServerError)
			rw.WriteHeader(500)
		}

		if useSession {
			if sessionManager.Exists(req.Context(), sessionOwnerSessionAttribute) {
				sessionOwnerAccessTokenSha := sessionManager.Get(req.Context(), sessionOwnerSessionAttribute).([]byte)
				if !bytes.Equal(sessionOwnerAccessTokenSha, sessionSha) {
					// This session must have belonged to a different accessToken, destroy it.
					// New session created automatically.
					_ = sessionManager.Destroy(req.Context())
					sessionManager.Put(req.Context(), sessionOwnerSessionAttribute, sessionSha)
				}
			} else {
				sessionManager.Put(req.Context(), sessionOwnerSessionAttribute, sessionSha)
			}
		}

		var kubeConfig clientcmd.ClientConfig
		if impersonationConfig != nil {
			kubeConfig = clientcmd.NewNonInteractiveDeferredLoadingClientConfig(
				clientcmd.NewDefaultClientConfigLoadingRules(),
				&clientcmd.ConfigOverrides{},
			)
		} else {
			kubeConfig = clientcmd.NewNonInteractiveDeferredLoadingClientConfig(
				clientcmd.NewDefaultClientConfigLoadingRules(),
				&clientcmd.ConfigOverrides{
					AuthInfo: api.AuthInfo{
						Token: accessToken,
					},
				},
			)
		}

		config, err := kubeConfig.ClientConfig()

		// Set impersonation configuration options if they are provided.
		if impersonatedUser != "" {
			config.Impersonate = restclient.ImpersonationConfig{
				UserName: impersonatedUser,
			}
		}

		// config.WrapTransport = func(rt http.RoundTripper) http.RoundTripper {
		// 	return &Tracer{RoundTripper: rt}
		// }

		kubeClient, err := kubernetes.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		userClient, err := userv1.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		coreClient, err := v1.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		var loggedOnUser userapiv1.User
		if useSession {
			if sessionManager.Exists(req.Context(), loggedOnUserSessionAttribute) {
				loggedOnUser = sessionManager.Get(req.Context(), loggedOnUserSessionAttribute).(userapiv1.User)
			} else {
				loggedOnUser = getLoggedOnUser(req, userClient, impersonatedUser)
				sessionManager.Put(req.Context(), loggedOnUserSessionAttribute, loggedOnUser)
			}
		} else {
			loggedOnUser = getLoggedOnUser(req, userClient, impersonatedUser)
		}

		var accessControllerState interface{}
		if useSession {
			accessControllerState = sessionManager.Get(req.Context(), accessControllerStateCookieName)
		}

		controller := accesscontroller.NewKubernetesRBACAccessController(kubeClient, accessControllerState)
		state = &RequestState{
			EnmasseV1Client:  coreClient,
			AccessController: controller,
			User:             loggedOnUser,
			UserAccessToken:  config.BearerToken,
			UseSession:       useSession,
			ImpersonatedUser: impersonatedUser,
		}

		ctx := ContextWithRequestState(state, req.Context())

		next.ServeHTTP(rw, req.WithContext(ctx))
	})
}

func DevelopmentHandler(next http.Handler, sessionManager *scs.SessionManager, accessToken string) http.Handler {
	gob.Register(userapiv1.User{})

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

		userclient, err := userv1.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		coreClient, err := v1.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		var loggedOnUser userapiv1.User
		if sessionManager.Exists(req.Context(), loggedOnUserSessionAttribute) {
			loggedOnUser = sessionManager.Get(req.Context(), loggedOnUserSessionAttribute).(userapiv1.User)
		} else {
			loggedOnUser = getLoggedOnUser(req, userclient, "")
			sessionManager.Put(req.Context(), loggedOnUserSessionAttribute, loggedOnUser)
		}

		requestState := &RequestState{
			EnmasseV1Client:  coreClient,
			AccessController: accesscontroller.NewAllowAllAccessController(),
			User:             loggedOnUser,
			UserAccessToken:  accessToken,
			UseSession:       true,
			ImpersonatedUser: "",
		}

		ctx := ContextWithRequestState(requestState, req.Context())
		next.ServeHTTP(rw, req.WithContext(ctx))
	})
}

func UpdateAccessControllerState(ctx context.Context, loggedOnUser string, sessionManager *scs.SessionManager) string {
	requestState := GetRequestStateFromContext(ctx)
	if requestState != nil {
		loggedOnUser = requestState.User.Name
		if requestState.UseSession {
			if updated, accessControllerState := requestState.AccessController.GetState(); updated {
				if accessControllerState == nil {
					sessionManager.Remove(ctx, accessControllerStateCookieName)
				} else {
					sessionManager.Put(ctx, accessControllerStateCookieName, accessControllerState)
				}
			}
		}
	}
	return loggedOnUser
}

func getLoggedOnUser(req *http.Request, userClient *userv1.UserV1Client, impersonatedUser string) userapiv1.User {
	createUser := func(userId string) userapiv1.User {
		return userapiv1.User{
			ObjectMeta: metav1.ObjectMeta{
				Name: userId,
			},
			FullName:   userId,
			Identities: []string{userId},
		}
	}

	if util.HasApi(util.UserGVK) {
		usr, err := userClient.Users().Get("~", metav1.GetOptions{})
		if err == nil {
			return *usr
		}
	} else if impersonatedUser != "" {
		return createUser(impersonatedUser)
	}

	userId := "unknown"
	if req.Header.Get(forwardedPreferredHeader) != "" {
		userId = req.Header.Get(forwardedPreferredHeader)
	} else if req.Header.Get(forwardedEmailHeader) != "" {
		userId = req.Header.Get(forwardedEmailHeader)
	} else if req.Header.Get(forwardedUserHeader) != "" {
		userId = req.Header.Get(forwardedUserHeader)
	}
	return createUser(userId)
}

func getShaSum(accessToken string, impersonationUser string) ([]byte, error) {
	d := sha256.New()
	_, err := d.Write([]byte(accessToken))
	if err != nil {
		return nil, err
	}
	_, err = d.Write([]byte(impersonationUser))
	if err != nil {
		return nil, err
	}

	shaSum := d.Sum(nil)
	return shaSum[:], nil
}

func getAccessToken(req *http.Request) string {
	accessToken := req.Header.Get(forwardedHeader)
	if accessToken == "" {
		accessToken = req.Header.Get(authHeader)
		accessToken = bearerRegexp.ReplaceAllString(accessToken, "$1")
	}
	return accessToken
}
