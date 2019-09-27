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
	"strconv"
	"strings"
)

type AgentCollectorCreator = func() agent.AgentCollector

type ConnectionAndLinkWatcher struct {
	Namespace             string
	Cache                 cache.Cache
	MetricCache           cache.Cache
	ClientInterface       cp.CoreV1Interface
	AgentCollectorCreator AgentCollectorCreator
	BearerToken           string
	collectors            map[string]agent.AgentCollector
	watching              chan struct{}
	watchingAgentsStarted bool
	stopchan              chan struct{}
	stoppedchan           chan struct{}
}

func (clw *ConnectionAndLinkWatcher) Init(c cache.Cache, cl interface{}) error {
	client, ok := cl.(cp.CoreV1Interface)
	if !ok {
		return fmt.Errorf("unexpected type %T", cl)
	}
	clw.Cache = c
	clw.ClientInterface = client
	clw.collectors = make(map[string]agent.AgentCollector)
	clw.watching = make(chan struct{})
	clw.stopchan = make(chan struct{})
	clw.stoppedchan = make(chan struct{})
	return nil
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

		err = clw.MetricCache.DeleteByPrefix("id", conKey)
		if err != nil {
			return err
		}

		err = clw.MetricCache.DeleteByPrefix("connectionLink", linkKey)
		if err != nil {
			return err
		}

	case agent.AgentConnectionEventType:
		con, links, metrics := agent.ToConnectionK8Style(event.Object.(*agent.AgentConnection))

		// TODO Currently removes all links/link metrics for this connection - ought to remove only that relate to link that have gone.
		linkKey := fmt.Sprintf("Link/%s/%s/%s", con.Namespace, con.Spec.AddressSpace, con.Name)
		err := clw.Cache.DeleteByPrefix("hierarchy", linkKey)
		if err != nil {
			return err
		}
		err = clw.MetricCache.DeleteByPrefix("connectionLink", linkKey)
		if err != nil {
			return err
		}


		err = clw.Cache.Add(con)
		if err != nil {
			return err
		}

		{
			lcopy := make([]interface{}, len(links))
			for i, v := range links {
				lcopy[i] = v
			}
			err = clw.Cache.Add(lcopy...)
			if err != nil {
				return err
			}
		}

		{
			mcopy := make([]interface{}, len(metrics))
			for i, v := range metrics {
				mcopy[i] = v
			}
			err = clw.MetricCache.Add(mcopy...)
			if err != nil {
				return err
			}
		}
	case agent.AgentConnectionEventTypeDelete:
		// we don't really need the full object, only the skeletal form to get the indexes.
		con, _, _ := agent.ToConnectionK8Style(event.Object.(*agent.AgentConnection))

		err := clw.Cache.Delete(con)
		if err != nil {
			return err
		}

		// Remove links belonging to this connection
		linkKey := fmt.Sprintf("Link/%s/%s/%s", con.Namespace, con.Spec.AddressSpace, con.Name)
		conKey := fmt.Sprintf("Connection/%s/%s/%s/", con.Namespace, con.Spec.AddressSpace, con.Name)
		err = clw.Cache.DeleteByPrefix("hierarchy", linkKey)
		if err != nil {
			return err
		}

		// Remove the connection metrics
		err = clw.MetricCache.DeleteByPrefix("id", conKey)
		if err != nil {
			return err
		}

		// Delete all link metrics belonging to this connection.
		err = clw.MetricCache.DeleteByPrefix("connectionLink", linkKey)
		if err != nil {
			return err
		}

	case agent.AgentAddressEventType:
		addr := event.Object.(*agent.AgentAddress)

		storedMetric := &consolegraphql.Metric{
			Kind:         "Address",
			Namespace:    addr.AddressSpaceNamespace,
			AddressSpace: addr.AddressSpace,
			Name:         addr.Address,
			MetricName:   "enmasse_messages_stored",
			MetricValue:  float64(addr.Depth),
			MetricType:   "gauge",
		}
		messagesInMetric := &consolegraphql.Metric{
			Kind:         "Address",
			Namespace:    addr.AddressSpaceNamespace,
			AddressSpace: addr.AddressSpace,
			Name:         addr.Address,
			MetricName:   "enmasse_messages_in",
			MetricValue:  float64(addr.MessagesIn),
			MetricType:   "gauge",
		}
		messagesOutMetric := &consolegraphql.Metric{
			Kind:         "Address",
			Namespace:    addr.AddressSpaceNamespace,
			AddressSpace: addr.AddressSpace,
			Name:         addr.Address,
			MetricName:   "enmasse_messages_out",
			MetricValue:  float64(addr.MessagesOut),
			MetricType:   "gauge",
		}

		err := clw.MetricCache.Add(storedMetric, messagesInMetric, messagesOutMetric)
		if err != nil {
			return err
		}
	case agent.AgentAddressEventTypeDelete:
		addr := event.Object.(*agent.AgentAddress)

		err := clw.MetricCache.DeleteByPrefix("id", fmt.Sprintf("Address/%s/%s/%s/", addr.AddressSpaceNamespace, addr.AddressSpace, addr.Address))
		if err != nil {
			return err
		}
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
