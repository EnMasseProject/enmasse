/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package watchers

import (
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/agent"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/util"
	tp "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/watch"
	"os"
	"strconv"
	"strings"
	//"k8s.io/apimachinery/pkg/watch"
	cp "k8s.io/client-go/kubernetes/typed/core/v1"
	"k8s.io/client-go/rest"
	"log"
)

type AgentCollectorCreator = func() agent.AgentCollector

type ConnectionAndLinkWatcher struct {
	Cache                 cache.Cache
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

func (clw *ConnectionAndLinkWatcher) Watch(namespace string) error {
	go func() {
		defer close(clw.stoppedchan)
		defer func() {
			if !clw.watchingAgentsStarted {
				close(clw.watching)
			}
		}()
		resource := clw.ClientInterface.Services(namespace)
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

	copy := make(map[string]agent.AgentCollector)
	for k, v := range clw.collectors {
		copy[k] = v
	}

	for _, service := range resourceList.Items {
		infraUuid, addressSpace, addressSpaceNamespace, err := getServiceDetails(service)
		if err != nil {
			log.Printf("Failed to find required labels on agent service, skipped agent, %v", err)
			continue
		}

		if _, present := copy[*infraUuid]; !present {
			collector, err := clw.createCollectorForService(service, infraUuid, addressSpace, addressSpaceNamespace)
			if err != nil {
				return err
			}
			clw.collectors[*infraUuid] = collector
		} else {
			delete(copy, *infraUuid)
		}
	}

	// Shutdown any slate collectors
	for k, v := range copy {
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

func (clw *ConnectionAndLinkWatcher) handleEvent(event agent.AgentConnectionEvent) error {
	switch event.Type {
	case agent.AgentConnectionEventTypeRestart:
		curr, _ := clw.Cache.GetMap(fmt.Sprintf("Connection/%s/%s", event.AddressSpaceNamespace, event.AddressSpace))
		for _, stale := range curr {
			err := clw.Cache.Delete(stale)
			if err != nil {
				return err
			}
		}
	case agent.AgentConnectionEventTypeAdd:
		connection := agent.ToK8Style(event.Object)
		connection.Namespace = event.AddressSpaceNamespace
		connection.Spec.AddressSpace = event.AddressSpace

		err := clw.Cache.Add(connection)
		if err != nil {
			return err
		}
	case agent.AgentConnectionEventTypeDelete:
		connection := agent.ToK8Style(event.Object)
		connection.Namespace = event.AddressSpaceNamespace
		connection.Spec.AddressSpace = event.AddressSpace

		err := clw.Cache.Delete(connection)
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
