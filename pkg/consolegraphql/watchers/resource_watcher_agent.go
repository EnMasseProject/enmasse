/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package watchers

import (
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/agent"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/util"
	routev1 "github.com/openshift/client-go/route/clientset/versioned/typed/route/v1"
	tp "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/watch"
	cp "k8s.io/client-go/kubernetes/typed/core/v1"
	"k8s.io/client-go/rest"
	"log"
	"math/rand"
	"reflect"
	"time"
)

type AgentCollectorCreator = func(host string, port int32, infraUuid string, addressSpace string, addressSpaceNamespace string, tlsConfig *tls.Config) agent.Delegate

type AgentWatcher struct {
	Namespace             string
	Cache                 cache.Cache
	ClientInterface       cp.CoreV1Interface
	AgentCollectorCreator AgentCollectorCreator
	collectors            map[string]agent.Delegate
	watching              chan struct{}
	watchingAgentsStarted bool
	stopchan              chan struct{}
	stoppedchan           chan struct{}
	developmentMode       bool
	RouteClientInterface  *routev1.RouteV1Client
	restartCounter        int32
	resyncInterval        *time.Duration
}

func NewAgentWatcher(c cache.Cache, resyncInterval *time.Duration, namespace string, creator AgentCollectorCreator, developmentMode bool, options ...WatcherOption) (*AgentWatcher, error) {

	clw := &AgentWatcher{
		Namespace:             namespace,
		Cache:                 c,
		watching:              make(chan struct{}),
		stopchan:              make(chan struct{}),
		stoppedchan:           make(chan struct{}),
		resyncInterval:        resyncInterval,
		AgentCollectorCreator: creator,
		collectors:            make(map[string]agent.Delegate),
		developmentMode:       developmentMode,
	}

	for _, option := range options {
		err := option(clw)
		if err != nil {
			return nil, err
		}
	}
	if clw.ClientInterface == nil {
		return nil, fmt.Errorf("client must be configured using the NamespaceWatcherConfig or NamespaceWatcherClient")
	}

	return clw, nil
}

func AgentWatcherServiceConfig(config *rest.Config) WatcherOption {
	return func(watcher ResourceWatcher) error {
		w := watcher.(*AgentWatcher)

		var cl interface{}
		cl, _ = cp.NewForConfig(config)

		client, ok := cl.(cp.CoreV1Interface)
		if !ok {
			return fmt.Errorf("unexpected type %T", cl)
		}

		w.ClientInterface = client
		return nil
	}
}

// Development only
func AgentWatcherRouteConfig(config *rest.Config) WatcherOption {
	return func(watcher ResourceWatcher) error {
		w := watcher.(*AgentWatcher)

		routeClient, err := routev1.NewForConfig(config)
		if err != nil {
			return err
		}

		w.RouteClientInterface = routeClient
		return nil
	}
}

func AgentWatcherClient(client cp.CoreV1Interface) WatcherOption {
	return func(watcher ResourceWatcher) error {
		w := watcher.(*AgentWatcher)
		w.ClientInterface = client
		return nil
	}
}

func (clw *AgentWatcher) Collector(infraUuid string) agent.Delegate {
	return clw.collectors[infraUuid]
}

func (clw *AgentWatcher) Watch() error {
	go func() {
		defer close(clw.stoppedchan)
		defer func() {
			if !clw.watchingAgentsStarted {
				close(clw.watching)
			}
		}()
		resource := clw.ClientInterface.Services(clw.Namespace)
		log.Printf("Agent - Watching")
		running := true
		for running {
			err := clw.doWatch(resource)
			if err != nil {
				log.Printf("Agent - Restarting watch - %v", err)
				atomicInc(&clw.restartCounter)
			} else {
				running = false
			}
		}

		for infraUuid, collector := range clw.collectors {
			delete(clw.collectors, infraUuid)
			collector.Shutdown()
		}

		log.Printf("Agent - Watching stopped")
	}()

	return nil
}

func (clw *AgentWatcher) AwaitWatching() {
	<-clw.watching
}

func (clw *AgentWatcher) Shutdown() {
	close(clw.stopchan)
	<-clw.stoppedchan
}

func (clw *AgentWatcher) GetRestartCount() int32 {
	return atomicGet(&clw.restartCounter)
}

func (clw *AgentWatcher) doWatch(resource cp.ServiceInterface) error {
	resourceList, err := resource.List(v1.ListOptions{
		LabelSelector: "app=enmasse,component=agent",
	})
	if err != nil {
		return err
	}

	current := make(map[string]agent.Delegate)
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
			host, port, err := clw.getHostPortFrom(service)
			if err != nil {
				return err
			}

			if clw.developmentMode && clw.RouteClientInterface != nil {
				host, port, err = clw.tryRouteForHostPort(service, infraUuid)
				if err != nil {
					continue
				}
			}

			collector, err := clw.createCollector(host, port, infraUuid, addressSpace, addressSpaceNamespace)
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

	watchOptions := v1.ListOptions{
		ResourceVersion: resourceList.ResourceVersion,
		LabelSelector:   "app=enmasse,component=agent",
	}
	if clw.resyncInterval != nil {
		ts := int64(clw.resyncInterval.Seconds() * (rand.Float64() + 1.0))
		watchOptions.TimeoutSeconds = &ts
	}
	resourceWatch, err := resource.Watch(watchOptions)
	if err != nil {
		return err
	}
	defer resourceWatch.Stop()

	if !clw.watchingAgentsStarted {
		close(clw.watching)
		clw.watchingAgentsStarted = true
	}

	ch := resourceWatch.ResultChan()
	for {
		select {
		case event, chok := <-ch:
			if !chok {
				return fmt.Errorf("watch ended due to channel error")
			} else if event.Type == watch.Error {
				return fmt.Errorf("watch ended in error")
			}

			var err error
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
						host, port, err := clw.getHostPortFrom(*service)
						if err != nil {
							return err
						}

						if clw.developmentMode && clw.RouteClientInterface != nil {
							host, port, err = clw.tryRouteForHostPort(*service, infraUuid)
							if err != nil {
								break
							}
						}

						collector, err := clw.createCollector(host, port, infraUuid, addressSpace, addressSpaceNamespace)
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
			if err != nil {
				return err
			}
		case <-clw.stopchan:
			log.Printf("Connections/Links - Shutdown received")
			return nil
		}
	}
}

func (clw *AgentWatcher) createCollector(host string, port int32, infraUuid *string, addressSpace *string, addressSpaceNamespace *string) (agent.Delegate, error) {
	pem, err := clw.getCaSecret(*addressSpace, *infraUuid)
	if err != nil {
		return nil, err
	}

	// Create collector for this service
	collector := clw.AgentCollectorCreator(host, port, *infraUuid, *addressSpace, *addressSpaceNamespace, buildTlsConfig(pem))
	err = collector.Collect(clw.handleEvent)
	if err != nil {
		return nil, err
	}
	return collector, nil
}

func (clw *AgentWatcher) getCaSecret(addressSpace string, infraUuid string) ([]byte, error) {
	secretName := fmt.Sprintf("ca-%s%s", addressSpace, infraUuid)
	secret, err := clw.ClientInterface.Secrets(clw.Namespace).Get(secretName, v1.GetOptions{})
	if err != nil {
		return nil, err
	}
	const key = "tls.crt"
	pem := secret.Data[key]

	if pem == nil {
		return nil, fmt.Errorf("expected key '%s' not found in secret '%s'", key, secretName)
	}

	return pem, err
}

func buildTlsConfig(pem []byte) *tls.Config {

	rootCAs := x509.NewCertPool()
	rootCAs.AppendCertsFromPEM(pem)

	verifyWithoutHostNameCheck := func(rootCAs *x509.CertPool) func(certificates [][]byte, _ [][]*x509.Certificate) error {
		return func(certificates [][]byte, _ [][]*x509.Certificate) error {
			certs := make([]*x509.Certificate, len(certificates))
			for i, asn1Data := range certificates {
				cert, err := x509.ParseCertificate(asn1Data)
				if err != nil {
					return fmt.Errorf("tls: failed to parse certificate from server: %s", err.Error())
				}
				certs[i] = cert
			}

			// VerifyOptions configure to avoid the hostname check
			opts := x509.VerifyOptions{
				Roots:         rootCAs,
				Intermediates: x509.NewCertPool(),
			}
			for _, cert := range certs[1:] {
				opts.Intermediates.AddCert(cert)
			}
			_, err := certs[0].Verify(opts)
			return err
		}
	}

	tlsConfig := &tls.Config{
		InsecureSkipVerify:    true, // Forces the use of our custom VerifyPeerCertificate instead
		RootCAs:               rootCAs,
		VerifyPeerCertificate: verifyWithoutHostNameCheck(rootCAs),
	}
	return tlsConfig
}

func (clw *AgentWatcher) handleEvent(event agent.AgentEvent) error {

	switch event.Type {
	case agent.AgentEventTypeRestart:
		conKey := fmt.Sprintf("Connection/%s/%s/", event.AddressSpaceNamespace, event.AddressSpace)
		linkKey := fmt.Sprintf("Link/%s/%s/", event.AddressSpaceNamespace, event.AddressSpace)

		err := clw.Cache.DeleteByPrefix(cache.PrimaryObjectIndex, conKey)
		if err != nil {
			return err
		}

		err = clw.Cache.DeleteByPrefix(cache.PrimaryObjectIndex, linkKey)
		if err != nil {
			return err
		}

	case agent.AgentEventInsertOrUpdateType:
		now := time.Now()
		switch target := event.Object.(type) {
		case *agent.AgentConnection:
			objs, err := clw.Cache.Get(cache.PrimaryObjectIndex, fmt.Sprintf("Connection/%s/%s/%s", target.AddressSpaceNamespace, target.AddressSpace, target.Uuid), nil)
			if err != nil {
				return err
			}

			var con *consolegraphql.Connection
			if len(objs) == 0 {
				con = toConnectionObject(target)
			} else {
				con = objs[0].(*consolegraphql.Connection)
			}

			err = updateConnectionMetrics(con, target, now)
			if err != nil {
				return err
			}

			err = clw.Cache.Add(con)
			if err != nil {
				return err
			}

			currentLinks := toLinkObjects(target)

			linkKey := fmt.Sprintf("Link/%s/%s/%s", con.Namespace, con.Spec.AddressSpace, con.Name)
			existingLinks, err := clw.Cache.Get(cache.PrimaryObjectIndex, linkKey, nil)
			if err != nil {
				return err
			}

			orphans, newLinks, updatingLinks := resolveLinks(currentLinks, existingLinks)

			// Remove orphan links from cache
			if len(orphans) > 0 {
				err := clw.Cache.Delete(orphans...)
				if err != nil {
					return err
				}
			}

			agentSendingLinks, agentReceivingLinks := buildLinkMaps(target)

			pending, err := updateAllLinks(target.AddressSpaceType, updatingLinks, agentSendingLinks, agentReceivingLinks, now)
			if err != nil {
				return err
			}

			more, err := updateAllLinks(target.AddressSpaceType, newLinks, agentSendingLinks, agentReceivingLinks, now)
			if err != nil {
				return err
			}
			pending = append(pending, more...)

			err = clw.Cache.Add(pending...)
			if err != nil {
				return err
			}

		case *agent.AgentAddress:
			objs, err := clw.Cache.Get(cache.PrimaryObjectIndex, fmt.Sprintf("Address/%s/%s", target.AddressSpaceNamespace, target.Name), nil)
			if err != nil {
				return err
			}

			if len(objs) > 0 {
				addr := objs[0].(*consolegraphql.AddressHolder)
				err = updateAddressMetrics(addr, target, now)
				if err != nil {
					return err
				}
				err = clw.Cache.Add(addr)
				if err != nil {
					return err
				}
			}
		default:
			panic(fmt.Errorf("unrecognised type %T", target))
		}
	case agent.AgentEventTypeDelete:
		switch target := event.Object.(type) {
		case *agent.AgentConnection:
			// Remove the connection itself
			err := clw.Cache.DeleteByPrefix(cache.PrimaryObjectIndex, fmt.Sprintf("Connection/%s/%s/%s", target.AddressSpaceNamespace, target.AddressSpace, target.Uuid))
			if err != nil {
				return err
			}

			// Remove links belonging to this connection
			linkKey := fmt.Sprintf("Link/%s/%s/%s", target.AddressSpaceNamespace, target.AddressSpace, target.Uuid)
			err = clw.Cache.DeleteByPrefix(cache.PrimaryObjectIndex, linkKey)
			if err != nil {
				return err
			}
		case *agent.AgentAddress:
			// Nothing to do - kubernetes address watcher will remove the address record from the cache
		default:
			panic(fmt.Errorf("unrecognised type %T", target))
		}
	default:
		panic(fmt.Errorf("unrecognised event type %s", event.Type))
	}
	return nil
}

func (clw *AgentWatcher) getHostPortFrom(service tp.Service) (string, int32, error) {
	prt, err := util.GetPortForService(service.Spec.Ports, "amqps")
	if err != nil {
		return "", 0, err
	}
	host := fmt.Sprintf("%s.%s.svc", service.ObjectMeta.Name, clw.Namespace)
	port := *prt
	return host, port, nil
}

// Development mode
func (clw *AgentWatcher) tryRouteForHostPort(service tp.Service, infraUuid *string) (string, int32, error) {
	routes := clw.RouteClientInterface.Routes(clw.Namespace)
	route, err := routes.Get(service.Name, v1.GetOptions{})
	if err != nil {
		log.Printf("Development mode - can't find route '%s', skipping this agent: %v", service.Name, err)
		log.Printf(`Maybe you want to run:
oc create route passthrough --service=agent-%s
and restart this program.`, *infraUuid)
		return "", 0, err
	}
	return route.Spec.Host, int32(443), nil
}

func updateConnectionMetrics(con *consolegraphql.Connection, agentCon *agent.AgentConnection, now time.Time) error {
	metrics := con.Metrics

	in, metrics := consolegraphql.FindOrCreateRateCalculatingMetric(metrics, "enmasse_messages_in", "gauge", "messages/sec")
	in.Update(float64(agentCon.MessagesIn), now)
	out, metrics := consolegraphql.FindOrCreateRateCalculatingMetric(metrics, "enmasse_messages_out", "gauge", "messages/sec")
	out.Update(float64(agentCon.MessagesOut), now)
	senders, metrics := consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_senders", "gauge")
	senders.Update(float64(len(agentCon.Senders)), now)
	receivers, metrics := consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_receivers", "gauge")
	receivers.Update(float64(len(agentCon.Receivers)), now)

	con.Metrics = metrics
	return nil
}

func updateAddressMetrics(addr *consolegraphql.AddressHolder, agentAddr *agent.AgentAddress, now time.Time) error {
	metrics := addr.Metrics

	var sm *consolegraphql.SimpleMetric
	var rm *consolegraphql.RateCalculatingMetric

	sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_messages_stored", "gauge")
	sm.Update(float64(agentAddr.Depth), now)

	if addr.Spec.Type != "subscription" {
		sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_senders", "gauge")
		sm.Update(float64(agentAddr.Senders), now)

		sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_receivers", "gauge")
		sm.Update(float64(agentAddr.Receivers), now)

		rm, metrics = consolegraphql.FindOrCreateRateCalculatingMetric(metrics, "enmasse_messages_in", "gauge", "messages/sec")
		rm.Update(float64(agentAddr.MessagesIn), now)

		rm, metrics = consolegraphql.FindOrCreateRateCalculatingMetric(metrics, "enmasse_messages_out", "gauge", "messages/sec")
		rm.Update(float64(agentAddr.MessagesOut), now)
	} else {
		consumers := 0
		messagesIn := 0
		messagesOut := 0
		for _, shard := range agentAddr.Shards {
			consumers += shard.Consumers
			messagesIn += shard.Enqueued
			messagesOut += shard.Acknowledged + shard.Killed
		}

		sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_receivers", "gauge")
		sm.Update(float64(consumers), now)

		rm, metrics = consolegraphql.FindOrCreateRateCalculatingMetric(metrics, "enmasse_messages_in", "gauge", "messages/sec")
		rm.Update(float64(messagesIn), now)

		rm, metrics = consolegraphql.FindOrCreateRateCalculatingMetric(metrics, "enmasse_messages_out", "gauge", "messages/sec")
		rm.Update(float64(messagesOut), now)
	}

	addr.Metrics = metrics
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

func toConnectionObject(connection *agent.AgentConnection) *consolegraphql.Connection {

	con := &consolegraphql.Connection{
		TypeMeta: v1.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: v1.ObjectMeta{
			Name:              connection.Uuid,
			Namespace:         connection.AddressSpaceNamespace,
			UID:               types.UID(connection.Uuid),
			CreationTimestamp: v1.Unix(connection.CreationTimestamp, 0),
		},

		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: connection.AddressSpace,
			Hostname:     connection.Host,
			ContainerId:  connection.Container,
			Protocol:     "amqp",
			Encrypted:    connection.Encrypted,
			Properties:   connection.Properties,
			Principal:    connection.User,
		},
	}
	if connection.Encrypted {
		con.Spec.Protocol = "amqps"
	}

	return con
}

func resolveLinks(currentLinks []*consolegraphql.Link, existingLinks []interface{}) ([]interface{}, []*consolegraphql.Link, []*consolegraphql.Link) {
	orphans := make([]interface{}, 0)
	newLinks := make([]*consolegraphql.Link, 0)
	updatingLinks := make([]*consolegraphql.Link, 0)
	for _, currentLink := range currentLinks {
		newLinks = append(newLinks, currentLink)
	}
	for i := range existingLinks {
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
			remove := func(s []*consolegraphql.Link, i int) []*consolegraphql.Link {
				s[len(s)-1], s[i] = s[i], s[len(s)-1]
				return s[:len(s)-1]
			}

			for i := range newLinks {
				if reflect.DeepEqual(newLinks[i].UID, existingLink.UID) {
					newLinks = remove(newLinks, i)
					break
				}
			}
			updatingLinks = append(updatingLinks, existingLink)
		}
	}
	return orphans, newLinks, updatingLinks
}

func toLinkObjects(connection *agent.AgentConnection) []*consolegraphql.Link {

	links := make([]*consolegraphql.Link, 0)
	for _, sender := range connection.Senders {
		link := toLinkObject(sender, "sender", connection)
		links = append(links, link)
	}
	for _, receiver := range connection.Receivers {
		link := toLinkObject(receiver, "receiver", connection)
		links = append(links, link)
	}
	return links
}

func toLinkObject(l agent.AgentAddressLink, role string, connection *agent.AgentConnection) *consolegraphql.Link {
	link := &consolegraphql.Link{
		TypeMeta: v1.TypeMeta{
			Kind: "Link",
		},
		ObjectMeta: v1.ObjectMeta{
			Name:      l.Uuid,
			Namespace: connection.AddressSpaceNamespace,
			UID:       types.UID(l.Uuid),
		},
		Spec: consolegraphql.LinkSpec{
			Connection:   connection.Uuid,
			AddressSpace: connection.AddressSpace,
			Address:      l.Address,
			Role:         role,
		},
	}
	return link
}

func buildLinkMaps(agentCon *agent.AgentConnection) (map[string]agent.AgentAddressLink, map[string]agent.AgentAddressLink) {
	sendingLinks := make(map[string]agent.AgentAddressLink)
	receivingLinks := make(map[string]agent.AgentAddressLink)
	for _, l := range agentCon.Senders {
		sendingLinks[l.Uuid] = l
	}
	for _, l := range agentCon.Receivers {
		receivingLinks[l.Uuid] = l
	}

	return sendingLinks, receivingLinks
}

func updateAllLinks(addressSpaceType string, targetLinks []*consolegraphql.Link, agentSendingLinks map[string]agent.AgentAddressLink, agentReceivingLinks map[string]agent.AgentAddressLink, now time.Time) ([]interface{}, error) {
	pending := make([]interface{}, 0)

	for _, targetLink := range targetLinks {
		var agentLinks map[string]agent.AgentAddressLink
		var rateMetricName string
		if targetLink.Spec.Role == "sender" {
			agentLinks = agentSendingLinks
			rateMetricName = "enmasse_messages_in"
		} else {
			agentLinks = agentReceivingLinks
			rateMetricName = "enmasse_messages_out"
		}

		err := updateLinkMetrics(addressSpaceType, agentLinks, targetLink, rateMetricName, now)
		if err != nil {
			return nil, err
		}

		pending = append(pending, targetLink)
	}
	return pending, nil
}

func updateLinkMetrics(addressSpaceType string, agentLinks map[string]agent.AgentAddressLink, targetModelLink *consolegraphql.Link, rateMetricName string, now time.Time) error {

	if l, present := agentLinks[targetModelLink.Name]; present {
		metrics := targetModelLink.Metrics

		var sm *consolegraphql.SimpleMetric
		sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_deliveries", "counter")
		sm.Update(float64(l.Deliveries), now)

		var rm *consolegraphql.RateCalculatingMetric
		rm, metrics = consolegraphql.FindOrCreateRateCalculatingMetric(metrics, rateMetricName, "gauge", "messages/sec")
		rm.Update(float64(l.Deliveries), now)

		switch addressSpaceType {
		case "standard":
			sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_accepted", "counter")
			sm.Update(float64(l.Accepted), now)
			sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_modified", "counter")
			sm.Update(float64(l.Modified), now)
			sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_presettled", "counter")
			sm.Update(float64(l.Presettled), now)
			sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_unsettled", "counter")
			sm.Update(float64(l.Unsettled), now)
			sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_undelivered", "counter")
			sm.Update(float64(l.Undelivered), now)
			sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_rejected", "counter")
			sm.Update(float64(l.Rejected), now)
			sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_released", "counter")
			sm.Update(float64(l.Released), now)

			// backlog - agent calculates this field to be the sum of undelivered/unsettled metrics
			backlog := 0
			for _, ld := range l.Links {
				backlog += ld.Backlog
			}
			sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_messages_backlog", "counter")
			sm.Update(float64(backlog), now)

		case "brokered":
			// No address space specific metrics
		default:
			panic(fmt.Sprintf("unexpected address space type : %s", addressSpaceType))
		}
		targetModelLink.Metrics = metrics
	}

	return nil
}
