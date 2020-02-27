/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package watchers

import (
	"crypto/tls"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/agent"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"k8s.io/client-go/rest"
	"log"
	"sync"
	"time"
)

type WatcherManager interface {
	// starts the watcher manager but does not begin watching.  must not called more than once.
	Start()
	// initiates watching if not currently watching (idempotent).
	BeginWatching()
	// ceases watching if currently watching (idempotent).
	EndWatching()
	// shuts down the watcher manager.  If watchers are watching they will be stopped.  must not called more than once.
	Shutdown()
	GetCollector(infra string) agent.Delegate
}

type command int

const (
	beginWatching command = iota
	endWatching   command = iota
)

type resourceManager struct {
	creators                         []func() (ResourceWatcher, error)
	resourcewatchers                 []ResourceWatcher
	command                          chan command
	stopped                          chan struct{}
	objectCache                      *cache.MemdbCache
	resyncInterval                   time.Duration
	config                           *rest.Config
	infraNamespace                   string
	developmentMode                  *bool
	agentCommandDelegateExpiryPeriod time.Duration
	agentAmqpConnectTimeout          time.Duration
	agentAmqpMaxFrameSize            uint32
	collectorMux                     sync.Mutex
	collector                        func(infraUuid string) agent.Delegate
}

func New(objectCache *cache.MemdbCache, resyncInterval time.Duration, config *rest.Config, infraNamespace string, developmentMode *bool, agentCommandDelegateExpiryPeriod time.Duration, agentAmqpConnectTimeout time.Duration, agentAmqpMaxFrameSize uint32) WatcherManager {
	manager := &resourceManager{
		command:                          make(chan command),
		stopped:                          make(chan struct{}),
		objectCache:                      objectCache,
		resyncInterval:                   resyncInterval,
		config:                           config,
		infraNamespace:                   infraNamespace,
		developmentMode:                  developmentMode,
		agentCommandDelegateExpiryPeriod: agentCommandDelegateExpiryPeriod,
		agentAmqpConnectTimeout:          agentAmqpConnectTimeout,
		agentAmqpMaxFrameSize:            agentAmqpMaxFrameSize,
	}
	return manager
}

func (rm *resourceManager) Start() {

	rm.creators = []func() (ResourceWatcher, error){
		func() (ResourceWatcher, error) {
			return NewAddressSpaceWatcher(rm.objectCache, &rm.resyncInterval, AddressSpaceWatcherConfig(rm.config),
				AddressSpaceWatcherFactory(AddressSpaceCreate, AddressSpaceUpdate))
		},
		func() (ResourceWatcher, error) {
			return NewAddressWatcher(rm.objectCache, &rm.resyncInterval, AddressWatcherConfig(rm.config),
				AddressWatcherFactory(AddressCreate, AddressUpdate))
		},
		func() (ResourceWatcher, error) {
			return NewNamespaceWatcher(rm.objectCache, &rm.resyncInterval, NamespaceWatcherConfig(rm.config))
		},
		func() (ResourceWatcher, error) {
			return NewAddressSpacePlanWatcher(rm.objectCache, &rm.resyncInterval, rm.infraNamespace, AddressSpacePlanWatcherConfig(rm.config))
		},
		func() (ResourceWatcher, error) {
			return NewAddressPlanWatcher(rm.objectCache, &rm.resyncInterval, rm.infraNamespace, AddressPlanWatcherConfig(rm.config))
		},
		func() (ResourceWatcher, error) {
			return NewAuthenticationServiceWatcher(rm.objectCache, &rm.resyncInterval, rm.infraNamespace, AuthenticationServiceWatcherConfig(rm.config))
		},
		func() (ResourceWatcher, error) {
			return NewAddressSpaceSchemaWatcher(rm.objectCache, &rm.resyncInterval, AddressSpaceSchemaWatcherConfig(rm.config))
		},
		func() (ResourceWatcher, error) {
			watcherConfigs := make([]WatcherOption, 0)
			watcherConfigs = append(watcherConfigs, AgentWatcherServiceConfig(rm.config))
			if *rm.developmentMode {
				watcherConfigs = append(watcherConfigs, AgentWatcherRouteConfig(rm.config))
			}
			watcher, err := NewAgentWatcher(rm.objectCache, &rm.resyncInterval, rm.infraNamespace,
				func(host string, port int32, infraUuid string, addressSpace string, addressSpaceNamespace string, tlsConfig *tls.Config) agent.Delegate {
					return agent.NewAmqpAgentDelegate(rm.config.BearerToken,
						host, port, tlsConfig,
						addressSpaceNamespace, addressSpace, infraUuid,
						rm.agentCommandDelegateExpiryPeriod, rm.agentAmqpConnectTimeout, rm.agentAmqpMaxFrameSize)
				}, *rm.developmentMode, watcherConfigs...)
			rm.collectorMux.Lock()
			defer rm.collectorMux.Unlock()
			rm.collector = watcher.Collector
			return watcher, err
		},
	}

	go func() {
		defer close(rm.stopped)
		var watching = false
		var cancelTask func()
		for {
			select {
			case event, chok := <-rm.command:
				if cancelTask != nil {
					cancelTask()
					cancelTask = nil
				}

				if !chok {
					// command channel closed
					if watching {
						rm.endAllWatchers()
						watching = false
					}
					return
				}
				switch event {
				case beginWatching:
					if !watching {
						err := rm.beginAllWatchers()
						if err != nil {
							log.Printf("failed to begin watchers (will retry in 1m): %s", err)
							cancelTask = server.RunAfter(time.Minute*1, func() {
								rm.BeginWatching()
							})
						} else {
							watching = true
						}
					}
				case endWatching:
					if watching {
						rm.endAllWatchers()
						watching = false
					}
				}
			}
		}
	}()

}

func (rm *resourceManager) BeginWatching() {
	rm.command <- beginWatching
}

func (rm *resourceManager) EndWatching() {
	rm.command <- endWatching
}

func (rm *resourceManager) Shutdown() {
	close(rm.command)
	<-rm.stopped
}

func (rm *resourceManager) GetCollector(s string) agent.Delegate {
	// TODO: message passing to the manager would be better
	rm.collectorMux.Lock()
	defer rm.collectorMux.Unlock()
	return rm.collector(s)
}

func (rm *resourceManager) beginAllWatchers() error {
	rm.resourcewatchers = make([]ResourceWatcher, 0)

	for _, f := range rm.creators {
		watcher, err := f()
		if err != nil {
			return err
		}
		rm.resourcewatchers = append(rm.resourcewatchers, watcher)

		err = watcher.Watch()
		if err != nil {
			return err
		}
	}
	return nil
}

func (rm *resourceManager) endAllWatchers() {
	for _, w := range rm.resourcewatchers {
		w.Shutdown()
	}

	rm.resourcewatchers = make([]ResourceWatcher, 0)
}
