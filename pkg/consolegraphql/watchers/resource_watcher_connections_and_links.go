/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package watchers

import (
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/agent"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/util"
	tp "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/watch"
	cp "k8s.io/client-go/kubernetes/typed/core/v1"
	"k8s.io/client-go/rest"
	"log"
	"os"
	"reflect"
	"strconv"
	"strings"
	time "time"
)

type AgentCollectorCreator = func() agent.AgentCollector

type ConnectionAndLinkWatcher struct {
	Namespace             string
	Cache                 cache.Cache
	ClientInterface       cp.CoreV1Interface
	AgentCollectorCreator AgentCollectorCreator
	collectors            map[string]agent.AgentCollector
	watching              chan struct{}
	watchingAgentsStarted bool
	stopchan              chan struct{}
	stoppedchan           chan struct{}
}


func NewConnectionAndLinkWatcher(c cache.Cache, namespace string, f func() agent.AgentCollector, options ...WatcherOption) (*ConnectionAndLinkWatcher, error) {

	clw := &ConnectionAndLinkWatcher{
		Namespace:             namespace,
		Cache:                 c,
		watching:              make(chan struct{}),
		stopchan:              make(chan struct{}),
		stoppedchan:           make(chan struct{}),
		AgentCollectorCreator: f,
		collectors:            make(map[string]agent.AgentCollector),
	}

	for _, option := range options {
		option(clw)
	}
	if clw.ClientInterface == nil {
		return nil, fmt.Errorf("Client must be configured using the NamespaceWatcherConfig or NamespaceWatcherClient")
	}

	return clw, nil
}

func ConnectionAndLinkWatcherConfig(config *rest.Config) WatcherOption {
	return func(watcher ResourceWatcher) error {
		w := watcher.(*ConnectionAndLinkWatcher)

		var cl interface{}
		cl, _  = cp.NewForConfig(config)

		client, ok := cl.(cp.CoreV1Interface)
		if !ok {
			return fmt.Errorf("unexpected type %T", cl)
		}

		w.ClientInterface = client
		return nil
	}
}

func ConnectionAndLinkWatcherClient(client cp.CoreV1Interface) WatcherOption {
	return func(watcher ResourceWatcher) error {
		w := watcher.(*ConnectionAndLinkWatcher)
		w.ClientInterface = client
		return nil
	}
}


func (clw *ConnectionAndLinkWatcher) Watch() error {
	go func() {
		defer close(clw.stoppedchan)
		defer func() {
			if !clw.watchingAgentsStarted {
				close(clw.watching)
			}
		}()
		resource := clw.ClientInterface.Services(clw.Namespace)
		log.Printf("Connections/Links - Watching")
		running := true
		for running {
			err := clw.doWatch(resource)
			if err != nil {
				log.Printf("Connections/Links - Restarting watch %v", err)
			} else {
				running = false
			}
		}

		for infraUuid, collector := range clw.collectors {
			delete(clw.collectors, infraUuid)
			collector.Shutdown()
		}

		log.Printf("Connections/Links - Watching stopped")
	}()

	return nil
}

func (clw *ConnectionAndLinkWatcher) NewClientForConfig(config *rest.Config) (interface{}, error) {
	return cp.NewForConfig(config)
}

func (clw *ConnectionAndLinkWatcher) AwaitWatching() {
	<-clw.watching
}

func (clw *ConnectionAndLinkWatcher) Shutdown() {
	close(clw.stopchan)
	<-clw.stoppedchan
}

func (clw *ConnectionAndLinkWatcher) doWatch(resource cp.ServiceInterface) error {
	resourceList, err := resource.List(v1.ListOptions{
		LabelSelector: "app=enmasse,component=agent",
	})
	if err != nil {
		return err
	}

	current := make(map[string]agent.AgentCollector)
	for k, v := range clw.collectors {
		current[k] = v
	}

	for _, service := range resourceList.Items {
		infraUuid, addressSpace, addressSpaceNamespace, err := getServiceDetails(service)
		if err != nil {
			log.Printf("Failed to find required labels on agent service, skipped agent, %v", err)
			continue
		}

		if _, present := current[*infraUuid]; !present {
			collector, err := clw.createCollectorForService(service, infraUuid, addressSpace, addressSpaceNamespace)
			if err != nil {
				return err
			}
			clw.collectors[*infraUuid] = collector
		} else {
			delete(current, *infraUuid)
		}
	}

	// Shutdown any stale collectors
	for k, v := range current {
		delete(clw.collectors, k)
		v.Shutdown()
	}

	resourceWatch, err := resource.Watch(v1.ListOptions{
		ResourceVersion: resourceList.ResourceVersion,
		LabelSelector:   "app=enmasse,component=agent",
	})

	if !clw.watchingAgentsStarted {
		close(clw.watching)
		clw.watchingAgentsStarted = true
	}

	ch := resourceWatch.ResultChan()
	for {
		select {
		case event := <-ch:
			var err error
			if event.Type == watch.Error {
				err = fmt.Errorf("watch ended in error")
			} else {
				service, ok := event.Object.(*tp.Service)
				if !ok {
					err = fmt.Errorf("watch error - object of unexpected type received")
				} else {
					infraUuid, addressSpace, addressSpaceNamespace, err := getServiceDetails(*service)
					if err != nil {
						log.Printf("Failed to find required labels on agent service, skipped agent %s, %v", service.Name, err)
						break
					}

					switch event.Type {
					case watch.Added:
						if _, present := clw.collectors[*infraUuid]; !present {
							collector, err := clw.createCollectorForService(*service, infraUuid, addressSpace, addressSpaceNamespace)
							if err != nil {
								return err
							}
							clw.collectors[*infraUuid] = collector
						}
					case watch.Deleted:
						if collector, present := clw.collectors[*infraUuid]; present {
							delete(clw.collectors, *infraUuid)
							collector.Shutdown()
						}
					}
				}
			}
			if err != nil {
				return err
			}
		case <-clw.stopchan:
			log.Printf("Connections/Links - Shutdown received")
			return nil
		}
	}
}

func (clw *ConnectionAndLinkWatcher) createCollectorForService(service tp.Service, infraUuid *string, addressSpace *string, addressSpaceNamespace *string) (agent.AgentCollector, error) {
	port, err := util.GetPortForService(service.Spec.Ports, "amqps")
	if err != nil {
		return nil, err
	}
	host := service.ObjectMeta.Name
	// For testing purposes only
	if v, ok := os.LookupEnv("AGENT_HOST"); ok {
		split := strings.Split(v, ":")
		if len(split) == 2 {
			host = split[0]
			i, _ := strconv.Atoi(split[1])
			i2 := int32(i)
			port = &i2
		}
	}
	// Create collector for this service
	collector := clw.AgentCollectorCreator()
	err = collector.Collect(*addressSpaceNamespace, *addressSpace, *infraUuid, host, *port, clw.handleEvent)
	if err != nil {
		return nil, err
	}
	return collector, nil
}

func (clw *ConnectionAndLinkWatcher) handleEvent(event agent.AgentEvent) error {

	switch event.Type {
	case agent.AgentEventTypeRestart:

		conKey := fmt.Sprintf("Connection/%s/%s/", event.AddressSpaceNamespace, event.AddressSpace)
		linkKey := fmt.Sprintf("Link/%s/%s/", event.AddressSpaceNamespace, event.AddressSpace)

		err := clw.Cache.DeleteByPrefix("hierarchy", conKey)
		if err != nil {
			return err
		}

		err = clw.Cache.DeleteByPrefix("hierarchy", linkKey)
		if err != nil {
			return err
		}

	case agent.AgentConnectionEventType:
		now := time.Now()
		agentcon := event.Object.(*agent.AgentConnection)

		objs, err := clw.Cache.Get("hierarchy", fmt.Sprintf("Connection/%s/%s/%s", agentcon.AddressSpaceNamespace, agentcon.AddressSpace, agentcon.Uuid), nil)
		if err != nil {
			return err
		}

		var con *consolegraphql.Connection
		if len(objs) == 0 {
			con, _ = agent.ToConnectionK8Style(agentcon)
		} else {
			con = objs[0].(*consolegraphql.Connection)
		}

		// Update the connection metrics
		metrics := con.Metrics
		in, metrics := consolegraphql.FindOrCreateRateCalculatingMetric(metrics, "enmasse_messages_in", "gauge")
		err = in.Update(float64(agentcon.MessagesIn), now)
		if err != nil {
			return err
		}
		out, metrics := consolegraphql.FindOrCreateRateCalculatingMetric(metrics, "enmasse_messages_out", "gauge")
		err = out.Update(float64(agentcon.MessagesOut), now)
		if err != nil {
			return err
		}
		senders, metrics := consolegraphql.FindOrCreateSimpleMetric(metrics,"enmasse_senders", "gauge" )
		senders.Update(float64(len(agentcon.Senders)), now)
		if err != nil {
			return err
		}
		receivers, metrics := consolegraphql.FindOrCreateSimpleMetric(metrics,"enmasse_receivers", "gauge" )
		receivers.Update(float64(len(agentcon.Receivers)), now)
		if err != nil {
			return err
		}

		con.Metrics = metrics

		err = clw.Cache.Add(con)
		if err != nil {
			return err
		}


		_, currentLinks := agent.ToConnectionK8Style(agentcon)

		for i, _ := range currentLinks {
			log.Printf("Current %s", currentLinks[i].Name)
		}


		linkKey := fmt.Sprintf("Link/%s/%s/%s", con.Namespace, con.Spec.AddressSpace, con.Name)
		existingLinks, err := clw.Cache.Get("hierarchy", linkKey, nil)

		orphans := make([]interface{}, 0)
		newLinks := make([]*consolegraphql.Link, 0)
		updatingLinks := make([]*consolegraphql.Link, 0)
		for _, currentLink := range currentLinks {
			newLinks = append(newLinks, currentLink)
		}
		for i, _ := range existingLinks {
			existingLink := existingLinks[i].(*consolegraphql.Link)
			orphan := true
			for _, currentLink := range currentLinks {
				if reflect.DeepEqual(currentLink.UID, existingLink.UID) {
					orphan = false
					break
				}
			}
			if orphan {
				orphans = append(orphans, existingLink)
			} else {
				remove := func (s []*consolegraphql.Link, i int) []*consolegraphql.Link {
					s[len(s)-1], s[i] = s[i], s[len(s)-1]
					return s[:len(s)-1]
				}

				for i, _ := range newLinks {
					if reflect.DeepEqual(newLinks[i].UID, existingLink.UID) {
						newLinks = remove(newLinks, i)
						break
					}
				}
				updatingLinks = append(updatingLinks, existingLink)
			}
		}

		// Remove orphan links from cache
		if len(orphans) > 0 {
			err := clw.Cache.Delete(orphans...)
			if err != nil {
				return err
			}

		}

		pending := make([]interface{}, 0)

		for _, updatingLink := range updatingLinks {
			metrics := updatingLink.Metrics
			metrics = agent.UpdateLinkMetrics(agentcon, metrics, now, updatingLink)

			updatingLink.Metrics = metrics
			pending = append(pending, updatingLink)
		}

		for _, newLink := range newLinks {
			metrics := newLink.Metrics
			metrics = agent.UpdateLinkMetrics(agentcon, metrics, now, newLink)

			newLink.Metrics = metrics
			pending = append(pending, newLink)
		}

		err = clw.Cache.Add(pending...)
		if err != nil {
			return err
		}

	case agent.AgentConnectionEventTypeDelete:
		con := event.Object.(*agent.AgentConnection)

		err := clw.Cache.DeleteByPrefix("hierarchy", fmt.Sprintf("Connection/%s/%s/%s", con.AddressSpaceNamespace, con.AddressSpace, con.Uuid))
		if err != nil {
			return err
		}

		// Remove links belonging to this connection
		linkKey := fmt.Sprintf("Link/%s/%s/%s", con.AddressSpaceNamespace, con.AddressSpace, con.Uuid)
		err = clw.Cache.DeleteByPrefix("hierarchy", linkKey)
		if err != nil {
			return err
		}


	case agent.AgentAddressEventType:
		agentAddr := event.Object.(*agent.AgentAddress)
		fmt.Printf("name %s  address %s" , agentAddr.Name, agentAddr.Address)

		now := time.Now()


		objs, err := clw.Cache.Get("hierarchy", fmt.Sprintf("Address/%s/%s/%s", agentAddr.AddressSpaceNamespace, agentAddr.AddressSpace, agentAddr.Name), nil)
		if err != nil {
			return err
		}

		if len(objs) > 0 {
			addr := objs[0].(*consolegraphql.AddressHolder)
			metrics := addr.Metrics

			var sm *consolegraphql.SimpleMetric
			var rm *consolegraphql.RateCalculatingMetric

			sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_messages_stored", "gauge")
			err := sm.Update(float64(agentAddr.Depth), now)
			if err != nil {
				return err
			}

			if addr.Spec.Type != "subscription" {
				sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics,"enmasse_senders", "gauge" )
				err := sm.Update(float64(agentAddr.Senders), now)
				if err != nil {
					return err
				}

				sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics,"enmasse_receivers", "gauge" )
				err = sm.Update(float64(agentAddr.Receivers), now)
				if err != nil {
					return err
				}

				rm, metrics = consolegraphql.FindOrCreateRateCalculatingMetric(metrics, "enmasse_messages_in", "gauge")
				err = rm.Update(float64(agentAddr.MessagesIn), now)
				if err != nil {
					return err
				}

				rm, metrics = consolegraphql.FindOrCreateRateCalculatingMetric(metrics, "enmasse_messages_out", "gauge")
				err = rm.Update(float64(agentAddr.MessagesOut), now)
				if err != nil {
					return err
				}
			} else {
				consumers := 0
				messagesIn := 0
				messagesOut := 0
				for _, shard := range agentAddr.Shards {
					consumers += shard.Consumers
					messagesIn += shard.Enqueued
					messagesOut += shard.Acknowledged + shard.Killed
				}

				sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics,"enmasse_receivers", "gauge" )
				err = sm.Update(float64(consumers), now)
				if err != nil {
					return err
				}

				rm, metrics = consolegraphql.FindOrCreateRateCalculatingMetric(metrics, "enmasse_messages_in", "gauge")
				err = rm.Update(float64(messagesIn), now)
				if err != nil {
					return err
				}

				rm, metrics = consolegraphql.FindOrCreateRateCalculatingMetric(metrics, "enmasse_messages_out", "gauge")
				err = rm.Update(float64(messagesOut), now)
				if err != nil {
					return err
				}
			}


			addr.Metrics = metrics
			err = clw.Cache.Add(addr)
			if err != nil {
				return err
			}
		}
	case agent.AgentAddressEventTypeDelete:
		_ = event.Object.(*agent.AgentAddress)
		// TODO handle temporary subscription queues.

	}
	return nil
}

func getServiceDetails(service tp.Service) (*string, *string, *string, error) {
	infraUuid, err := getLabel(service, "infraUuid")
	if err != nil {
		return nil, nil, nil, err
	}
	addressSpace, err := getAnnotation(service, "addressSpace")
	if err != nil {
		return nil, nil, nil, err
	}
	addressSpaceNamespace, err := getAnnotation(service, "addressSpaceNamespace")
	if err != nil {
		return nil, nil, nil, err
	}
	return infraUuid, addressSpace, addressSpaceNamespace, nil
}

func getLabel(service tp.Service, label string) (*string, error) {
	if value, exists := service.Labels[label]; exists {
		return &value, nil
	} else {
		return nil, fmt.Errorf("agent service %v lacks an %s label", service, label)
	}
}

func getAnnotation(service tp.Service, annotation string) (*string, error) {
	if value, exists := service.Annotations[annotation]; exists {
		return &value, nil
	} else {
		return nil, fmt.Errorf("agent service %v lacks an %s annotation", service, annotation)
	}
}


