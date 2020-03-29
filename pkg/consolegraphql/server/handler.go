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
	userv1 "github.com/openshift/client-go/user/clientset/versioned/typed/user/v1"
	authv1 "k8s.io/api/authentication/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	authtv1 "k8s.io/client-go/kubernetes/typed/authentication/v1"
	restclient "k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
	"k8s.io/client-go/tools/clientcmd/api"
	"log"
	"net/http"
	"regexp"
	"strconv"
)

const accessControllerStateCookieName = "accessControllerState"
const sessionOwnerSessionAttribute = "sessionOwnerSessionAttribute"
const loggedOnUserSessionAttribute = "loggedOnUserSessionAttribute"

var bearerRegexp = regexp.MustCompile(`^Bearer +(.*)$`)

const forwardedHeader = "X-Forwarded-Access-Token"
const forwardedUserHeader = "X-Forwarded-User"
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

		coreClient, err := v1beta1.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		var loggedOnUser string
		if useSession && sessionManager.Exists(req.Context(), loggedOnUserSessionAttribute) {
			loggedOnUser = sessionManager.GetString(req.Context(), loggedOnUserSessionAttribute)
		} else {
			if impersonationConfig != nil {
				loggedOnUser = impersonatedUser
			} else {
				loggedOnUser = getLoggedOnUser(&accessToken, userClient.Users(), kubeClient.AuthenticationV1())
			}
			if useSession {
				sessionManager.Put(req.Context(), loggedOnUserSessionAttribute, loggedOnUser)
			}
		}

		var accessControllerState interface{}
		if useSession {
			accessControllerState = sessionManager.Get(req.Context(), accessControllerStateCookieName)
		}

		controller := accesscontroller.NewKubernetesRBACAccessController(kubeClient, accessControllerState)
		state = &RequestState{
			AuthenticationInterface: kubeClient.AuthenticationV1(),
			UserInterface:           userClient.Users(),
			EnmasseV1beta1Client:    coreClient,
			AccessController:        controller,
			User:                    loggedOnUser,
			UserAccessToken:         config.BearerToken,
			UseSession:              useSession,
			ImpersonateUser:         impersonationConfig != nil,
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

		kubeClient, err := kubernetes.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		userclient, err := userv1.NewForConfig(config)
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

		var loggedOnUser string
		if sessionManager.Exists(req.Context(), loggedOnUserSessionAttribute) {
			loggedOnUser = sessionManager.GetString(req.Context(), loggedOnUserSessionAttribute)
		} else {
			loggedOnUser = getLoggedOnUser(nil, userclient.Users(), kubeClient.AuthenticationV1())
			sessionManager.Put(req.Context(), loggedOnUserSessionAttribute, loggedOnUser)
		}

		requestState := &RequestState{
			AuthenticationInterface: kubeClient.AuthenticationV1(),
			UserInterface:           userclient.Users(),
			EnmasseV1beta1Client:    coreClient,
			AccessController:        accesscontroller.NewAllowAllAccessController(),
			User:                    loggedOnUser,
			UserAccessToken:         accessToken,
			UseSession:              true,
			ImpersonateUser:         false,
		}

		ctx := ContextWithRequestState(requestState, req.Context())
		next.ServeHTTP(rw, req.WithContext(ctx))
	})
}

func UpdateAccessControllerState(ctx context.Context, loggedOnUser string, sessionManager *scs.SessionManager) string {
	requestState := GetRequestStateFromContext(ctx)
	if requestState != nil {
		loggedOnUser = requestState.User
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

func getLoggedOnUser(accessToken *string, userInterface userv1.UserInterface, authenticationInterface authtv1.AuthenticationV1Interface) string {
	loggedOnUser := "<unknown>"

	if util.HasApi(util.UserGVK) {
		usr, err := userInterface.Get("~", metav1.GetOptions{})
		if err == nil {
			loggedOnUser = usr.ObjectMeta.Name
		}
	} else if accessToken != nil {
		res, err := authenticationInterface.TokenReviews().Create(&authv1.TokenReview{
			Spec: authv1.TokenReviewSpec{
				Token: *accessToken,
			},
		})
		if err == nil && res != nil && res.Status.Authenticated && res.Status.User.Username != "" {
			loggedOnUser = res.Status.User.Username
		}
	}
	return loggedOnUser
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
