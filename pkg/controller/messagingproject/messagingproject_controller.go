/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messagingproject

import (
	"context"
	"errors"
	"fmt"
	"reflect"
	"time"

	amqp "github.com/enmasseproject/enmasse/pkg/amqpcommand"
	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	"github.com/enmasseproject/enmasse/pkg/state"
	stateerrors "github.com/enmasseproject/enmasse/pkg/state/errors"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"
	"github.com/prometheus/prometheus/pkg/labels"

	corev1 "k8s.io/api/core/v1"

	k8errors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"

	"k8s.io/client-go/tools/record"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_messagingproject")
var _ reconcile.Reconciler = &ReconcileMessagingProject{}

type ReconcileMessagingProject struct {
	client         client.Client
	reader         client.Reader
	recorder       record.EventRecorder
	certController *cert.CertController
	clientManager  state.ClientManager
	namespace      string
}

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

/**
 * TODO - Referencing a MessagingPlan and applying router vhost configuration with settings from plan.
 * TODO - Referencing a AccessControlService and apply router configuration with settings.
 */

const (
	FINALIZER_NAME        = "enmasse.io/operator"
	PROJECT_RESOURCE_NAME = "default"
)

func newReconciler(mgr manager.Manager) *ReconcileMessagingProject {
	namespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")

	clientManager := state.GetClientManager()
	return &ReconcileMessagingProject{
		client:         mgr.GetClient(),
		reader:         mgr.GetAPIReader(),
		recorder:       mgr.GetEventRecorderFor("messagingproject"),
		certController: cert.NewCertController(mgr.GetClient(), mgr.GetScheme(), 24*30*time.Hour, 24*time.Hour),
		namespace:      namespace,
		clientManager:  clientManager,
	}
}

func add(mgr manager.Manager, r *ReconcileMessagingProject) error {

	c, err := controller.New("messagingproject-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &v1.MessagingProject{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	return err
}

func (r *ReconcileMessagingProject) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	logger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)

	ctx := context.Background()

	if request.Name != PROJECT_RESOURCE_NAME {
		logger.Info("Unsupported resource name")
		return reconcile.Result{}, nil
	}

	logger.Info("Reconciling MessagingProject")

	found := &v1.MessagingProject{}
	err := r.reader.Get(ctx, request.NamespacedName, found)
	if err != nil {
		if k8errors.IsNotFound(err) {
			logger.Info("MessagingProject resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		logger.Error(err, "Failed to get MessagingProject")
		return reconcile.Result{}, err
	}

	rc := resourceContext{
		project: found,
		status:  found.Status.DeepCopy(),
		ctx:     ctx,
		client:  r.client,
	}

	// Initialize phase and conditions
	rc.Process(func(project *v1.MessagingProject) (processorResult, error) {
		if project.Status.Phase == "" {
			project.Status.Phase = v1.MessagingProjectConfiguring
		}
		// TODO: Set based on plans
		project.Status.Capabilities = project.Spec.Capabilities

		project.Status.GetMessagingProjectCondition(v1.MessagingProjectBound)
		project.Status.GetMessagingProjectCondition(v1.MessagingProjectCaCreated)
		project.Status.GetMessagingProjectCondition(v1.MessagingProjectScheduled)
		project.Status.GetMessagingProjectCondition(v1.MessagingProjectPlanApplied)
		project.Status.GetMessagingProjectCondition(v1.MessagingProjectCreated)
		project.Status.GetMessagingProjectCondition(v1.MessagingProjectReady)
		return processorResult{}, nil
	})

	// Initialize and process finalizer
	result, err := rc.Process(func(project *v1.MessagingProject) (processorResult, error) {

		// Handle finalizing an deletion state first
		if project.DeletionTimestamp != nil && project.Status.Phase != v1.MessagingProjectTerminating {
			project.Status.Phase = v1.MessagingProjectTerminating
			err := r.client.Status().Update(ctx, project)
			return processorResult{Requeue: true}, err
		}

		original := project.DeepCopy()
		result, err := finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, project, []finalizer.Finalizer{
			finalizer.Finalizer{
				Name: FINALIZER_NAME,
				Deconstruct: func(c finalizer.DeconstructorContext) (reconcile.Result, error) {
					_, ok := c.Object.(*v1.MessagingProject)
					if !ok {
						return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports MessagingProject")
					}

					endpoints := &v1.MessagingEndpointList{}
					err := r.client.List(ctx, endpoints, client.InNamespace(project.Namespace))
					if err != nil {
						return reconcile.Result{}, err
					}

					addresses := &v1.MessagingAddressList{}
					err = r.client.List(ctx, addresses, client.InNamespace(project.Namespace))
					if err != nil {
						return reconcile.Result{}, err
					}

					if len(addresses.Items) > 0 || len(endpoints.Items) > 0 {
						return reconcile.Result{}, fmt.Errorf("unable to delete MessagingProject: waiting for %d addresses and %d endpoints to be deleted", len(addresses.Items), len(endpoints.Items))
					}

					if project.IsBound() {
						infra := &v1.MessagingInfrastructure{}
						err = r.client.Get(ctx, types.NamespacedName{Name: project.Status.MessagingInfrastructureRef.Name, Namespace: project.Status.MessagingInfrastructureRef.Namespace}, infra)
						if err != nil {
							return reconcile.Result{}, err
						}

						client := r.clientManager.GetClient(infra)
						err = client.DeleteProject(project)
						if err != nil {
							return reconcile.Result{}, err
						}

						secret := &corev1.Secret{
							ObjectMeta: metav1.ObjectMeta{Namespace: project.Status.MessagingInfrastructureRef.Namespace, Name: cert.GetProjectCaSecretName(project.Namespace)},
						}
						err = r.client.Delete(ctx, secret)
						if err != nil && k8errors.IsNotFound(err) {
							return reconcile.Result{}, nil
						}
						return reconcile.Result{}, err
					}
					return reconcile.Result{}, nil
				},
			},
		})
		if err != nil {
			return processorResult{}, err
		}

		if result.Requeue {
			// Update and requeue if changed
			if !reflect.DeepEqual(original, project) {
				err := r.client.Update(ctx, project)
				return processorResult{Return: true}, err
			}
		}
		return processorResult{Requeue: result.Requeue}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Lookup messaging infra
	infra := &v1.MessagingInfrastructure{}
	result, err = rc.Process(func(project *v1.MessagingProject) (processorResult, error) {
		if !project.IsBound() {
			// Find a suiting MessagingInfrastructure to bind to
			infras := &v1.MessagingInfrastructureList{}
			err := r.client.List(ctx, infras)
			if err != nil {
				logger.Info("Error listing infras")
				return processorResult{}, err
			}
			activeInfras := make([]v1.Selectable, 0)
			for _, item := range infras.Items {
				if item.Status.Phase == v1.MessagingInfrastructureActive {
					activeInfras = append(activeInfras, &item)
				}
			}
			bestMatch, err := r.findBestMatch(ctx, project.Namespace, activeInfras)
			if err != nil {
				return processorResult{}, err
			}
			infra = nil
			if bestMatch != nil {
				infra = bestMatch.(*v1.MessagingInfrastructure)
			}
			return processorResult{}, nil
		} else {
			err := r.client.Get(ctx, types.NamespacedName{Name: project.Status.MessagingInfrastructureRef.Name, Namespace: project.Status.MessagingInfrastructureRef.Namespace}, infra)
			if err != nil {
				if k8errors.IsNotFound(err) {
					msg := fmt.Sprintf("Infrastructure %s/%s not found!", project.Status.MessagingInfrastructureRef.Namespace, project.Status.MessagingInfrastructureRef.Name)
					project.Status.Message = msg
					project.Status.GetMessagingProjectCondition(v1.MessagingProjectBound).SetStatus(corev1.ConditionFalse, "", msg)
					return processorResult{RequeueAfter: 10 * time.Second}, nil
				} else {
					logger.Info("Error reconciling", err)
				}
			}
			return processorResult{}, err
		}
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Update infra reference
	result, err = rc.Process(func(project *v1.MessagingProject) (processorResult, error) {
		if infra != nil {
			project.Status.GetMessagingProjectCondition(v1.MessagingProjectBound).SetStatus(corev1.ConditionTrue, "", "")
			project.Status.MessagingInfrastructureRef = v1.ObjectReference{
				Name:      infra.Name,
				Namespace: infra.Namespace,
			}
			return processorResult{}, nil
		} else {
			msg := "Not yet bound to any infrastructure"
			project.Status.GetMessagingProjectCondition(v1.MessagingProjectBound).SetStatus(corev1.ConditionFalse, "", msg)
			project.Status.Message = msg
			return processorResult{RequeueAfter: 10 * time.Second}, err
		}
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Locate active plan for tenant
	result, err = rc.Process(func(project *v1.MessagingProject) (processorResult, error) {
		var plan *v1.MessagingPlan
		if project.Spec.MessagingPlanRef != nil {
			// Locate desired plan
			p := &v1.MessagingPlan{}
			err := r.client.Get(ctx, types.NamespacedName{Name: project.Spec.MessagingPlanRef.Name, Namespace: project.Spec.MessagingPlanRef.Namespace}, p)
			if err != nil {
				return processorResult{}, err
			}
			matches, err := r.matchesSelector(ctx, project.Namespace, p.Spec.NamespaceSelector)
			if err != nil {
				return processorResult{}, err
			}

			if !matches {
				err := fmt.Errorf("referenced plan does not match namespace of project")
				project.Status.Message = err.Error()
				project.Status.GetMessagingProjectCondition(v1.MessagingProjectPlanApplied).SetStatus(corev1.ConditionFalse, "", err.Error())
				return processorResult{RequeueAfter: 10 * time.Second}, nil
			}
			plan = p
		} else {
			// Find best matching plan
			plans := &v1.MessagingPlanList{}
			err := r.client.List(ctx, plans)
			if err != nil {
				logger.Info("Error listing plans")
				return processorResult{}, err
			}
			activePlans := make([]v1.Selectable, 0)
			for _, item := range plans.Items {
				if item.Status.Phase == v1.MessagingPlanActive {
					activePlans = append(activePlans, &item)
				}
			}
			bestMatch, err := r.findBestMatch(ctx, project.Namespace, activePlans)
			if err != nil {
				return processorResult{}, err
			}
			if bestMatch != nil {
				plan = bestMatch.(*v1.MessagingPlan)
			}
		}

		// No plan set is fine - defaults will be used
		if plan == nil {
			return processorResult{}, nil
		}

		logger.Info("Will apply plan", "plan", plan)
		// TODO: Apply plan - check differences
		project.Status.GetMessagingProjectCondition(v1.MessagingProjectPlanApplied).SetStatus(corev1.ConditionTrue, "", "")
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Schedule project with infra
	result, err = rc.Process(func(project *v1.MessagingProject) (processorResult, error) {
		transactional := false
		for _, capability := range project.Status.Capabilities {
			if capability == v1.MessagingCapabilityTransactional {
				transactional = true
				break
			}
		}

		if !transactional {
			project.Status.GetMessagingProjectCondition(v1.MessagingProjectScheduled).SetStatus(corev1.ConditionTrue, "", "")
			return processorResult{}, nil
		}

		// Already scheduled
		if project.Status.Broker != nil {
			project.Status.GetMessagingProjectCondition(v1.MessagingProjectScheduled).SetStatus(corev1.ConditionTrue, "", "")
			return processorResult{}, nil
		}

		client := r.clientManager.GetClient(infra)
		err := client.ScheduleProject(project)
		if err != nil {
			project.Status.GetMessagingProjectCondition(v1.MessagingProjectScheduled).SetStatus(corev1.ConditionFalse, "", err.Error())
			project.Status.Message = err.Error()
			if errors.Is(err, stateerrors.NotInitializedError) || errors.Is(err, amqp.RequestTimeoutError) || errors.Is(err, stateerrors.NotSyncedError) {
				return processorResult{RequeueAfter: 10 * time.Second}, nil
			}
			return processorResult{}, err
		}
		project.Status.GetMessagingProjectCondition(v1.MessagingProjectScheduled).SetStatus(corev1.ConditionTrue, "", "")
		return processorResult{Requeue: true}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Sync project with infra
	result, err = rc.Process(func(project *v1.MessagingProject) (processorResult, error) {
		client := r.clientManager.GetClient(infra)
		err := client.SyncProject(project)
		if err != nil {
			project.Status.GetMessagingProjectCondition(v1.MessagingProjectCreated).SetStatus(corev1.ConditionFalse, "", err.Error())
			project.Status.Message = err.Error()
			return processorResult{}, err
		}
		project.Status.GetMessagingProjectCondition(v1.MessagingProjectCreated).SetStatus(corev1.ConditionTrue, "", "")
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Reconcile Project CA
	result, err = rc.Process(func(project *v1.MessagingProject) (processorResult, error) {
		err := r.certController.ReconcileProjectCa(ctx, logger, infra, project.Namespace)
		if err != nil {
			project.Status.Message = err.Error()
			project.Status.GetMessagingProjectCondition(v1.MessagingProjectCaCreated).SetStatus(corev1.ConditionFalse, "", err.Error())
			return processorResult{}, err
		}
		project.Status.GetMessagingProjectCondition(v1.MessagingProjectCaCreated).SetStatus(corev1.ConditionTrue, "", "")
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Update project status
	result, err = rc.Process(func(project *v1.MessagingProject) (processorResult, error) {
		originalStatus := project.Status.DeepCopy()
		project.Status.Phase = v1.MessagingProjectActive
		project.Status.Message = ""
		project.Status.GetMessagingProjectCondition(v1.MessagingProjectReady).SetStatus(corev1.ConditionTrue, "", "")
		if !reflect.DeepEqual(originalStatus, project.Status) {
			logger.Info("Project has changed", "old", originalStatus, "new", project.Status,
				"originalBoundTransition", originalStatus.GetMessagingProjectCondition(v1.MessagingProjectBound).LastTransitionTime.UnixNano(),
				"originalReadyTransition", originalStatus.GetMessagingProjectCondition(v1.MessagingProjectReady).LastTransitionTime.UnixNano(),
				"boundTransition", project.Status.GetMessagingProjectCondition(v1.MessagingProjectBound).LastTransitionTime.UnixNano(),
				"readyTransition", project.Status.GetMessagingProjectCondition(v1.MessagingProjectReady).LastTransitionTime.UnixNano())
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			err := r.client.Status().Update(ctx, project)
			return processorResult{}, err
		}
		return processorResult{}, nil
	})
	return result.Result(), err
}

/**
 * Checks if a selector matches a given project.
 */
func (r *ReconcileMessagingProject) matchesSelector(ctx context.Context, namespace string, selector *v1.NamespaceSelector) (bool, error) {
	if selector == nil {
		return true, nil
	}

	for _, ns := range selector.MatchNames {
		if ns == namespace {
			return true, nil
		}
	}

	// Fall back to looking up namespace to verify labels if we have specified. This ensures that we only require namespace lookup permissions if
	// absolutely necessary.
	if len(selector.MatchLabels) > 0 || len(selector.MatchExpressions) > 0 {
		ns := &corev1.Namespace{}
		err := r.client.Get(ctx, types.NamespacedName{Name: namespace}, ns)
		if err != nil {
			return false, err
		}

		if len(selector.MatchLabels) > 0 {
			sel, err := metav1.LabelSelectorAsSelector(&metav1.LabelSelector{
				MatchLabels: selector.MatchLabels,
			})
			if err != nil {
				return false, err
			}
			if sel.Matches(labels.FromMap(ns.Labels)) {
				return true, nil
			}
		}

		if len(selector.MatchExpressions) > 0 {
			sel, err := metav1.LabelSelectorAsSelector(&metav1.LabelSelector{
				MatchExpressions: selector.MatchExpressions,
			})
			if err != nil {
				return false, err
			}
			return sel.Matches(labels.FromMap(ns.Labels)), nil
		}
	}
	return false, nil
}

/**
 * Find the best matching object among a list of objects whose selectors are matched against a messaging project.
 */
func (r *ReconcileMessagingProject) findBestMatch(ctx context.Context, namespace string, objects []v1.Selectable) (v1.Selectable, error) {
	var bestMatch v1.Selectable
	var bestMatchSelector *v1.NamespaceSelector

	if len(objects) == 0 {
		return nil, nil
	}

	// Try to find a selector matching a specific name before needing to lookup namespace
	for _, object := range objects {
		selector := object.GetSelector()
		// If there is a global one without a selector, use it
		if selector == nil {
			if bestMatch == nil {
				bestMatch = object
				bestMatchSelector = selector
			} else {
				// Prefer oldest
				if bestMatch.GetCreationTimestamp().Time.After(object.GetCreationTimestamp().Time) {
					bestMatch = object
					bestMatchSelector = selector
				}
			}
		} else if selector != nil {
			// If selector is applicable to this namespace
			matched := false
			for _, ns := range selector.MatchNames {
				if ns == namespace {
					matched = true
					break
				}
			}

			if matched {
				if bestMatch == nil || bestMatchSelector == nil {
					bestMatch = object
					bestMatchSelector = selector
				} else {
					// Prefer oldest
					if bestMatch.GetCreationTimestamp().Time.After(object.GetCreationTimestamp().Time) {
						bestMatch = object
						bestMatchSelector = selector
					}
				}
			}
		}
	}

	// Fall back to looking up namespace to verify labels if we have specified. This ensures that we only require namespace lookup permissions if
	// absolutely necessary.
	if bestMatch == nil {
		ns := &corev1.Namespace{}
		err := r.client.Get(ctx, types.NamespacedName{Name: namespace}, ns)
		if err != nil {
			return nil, err
		}

		for _, object := range objects {
			selector := object.GetSelector()
			// If there is a global one without a selector, use it
			if selector == nil && bestMatch == nil {
				bestMatch = object
			} else if selector != nil {
				matches := false
				if len(selector.MatchLabels) > 0 {
					sel, err := metav1.LabelSelectorAsSelector(&metav1.LabelSelector{
						MatchLabels: selector.MatchLabels,
					})
					if err != nil {
						return nil, err
					}
					matches = sel.Matches(labels.FromMap(ns.Labels))
				}

				if !matches && len(selector.MatchExpressions) > 0 {
					sel, err := metav1.LabelSelectorAsSelector(&metav1.LabelSelector{
						MatchExpressions: selector.MatchExpressions,
					})
					if err != nil {
						return nil, err
					}
					matches = sel.Matches(labels.FromMap(ns.Labels))
				}

				if matches {
					if bestMatch == nil || bestMatchSelector == nil {
						bestMatch = object
						bestMatchSelector = selector
					} else {
						// Prefer oldest
						if bestMatch.GetCreationTimestamp().Time.After(object.GetCreationTimestamp().Time) {
							bestMatch = object
							bestMatchSelector = selector
						}
					}
				}
			}
		}
	}
	return bestMatch, nil
}

/*
 * Automatically handle status update of the resource after running some reconcile logic.
 */
type resourceContext struct {
	ctx     context.Context
	client  client.Client
	status  *v1.MessagingProjectStatus
	project *v1.MessagingProject
}

type processorResult struct {
	Requeue      bool
	RequeueAfter time.Duration
	Return       bool
}

func (r *resourceContext) Process(processor func(project *v1.MessagingProject) (processorResult, error)) (processorResult, error) {
	result, err := processor(r.project)
	if !reflect.DeepEqual(r.status, r.project.Status) {
		if err != nil || result.Requeue || result.RequeueAfter > 0 {
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			statuserr := r.client.Status().Update(r.ctx, r.project)
			if statuserr != nil {
				// If this fails, report the status error if everything else whent ok, otherwise report the original error
				log.Error(statuserr, "Status update failed", "project", r.project.Name)
				if err == nil {
					err = statuserr
				}
			} else {
				r.status = r.project.Status.DeepCopy()
			}
			return result, err
		}
	}
	return result, err
}

func (r *processorResult) ShouldReturn(err error) bool {
	return err != nil || r.Requeue || r.RequeueAfter > 0 || r.Return
}

func (r *processorResult) Result() reconcile.Result {
	return reconcile.Result{Requeue: r.Requeue, RequeueAfter: r.RequeueAfter}
}
