
## 1.0.0

## 0.32.1
* #4914: Bump react from 16.12.0 -> 16.13.1 and typescript from 3.7.2 -> 3.8.3
* #4915: Bump patternfly/react-core version 3.146.0 -> 3.153.13

## 0.32.0


* #3157: Add support for per-address limits in broker
* #3158: Setting per-address max TTL
* #3839: [IoT] Implement a JDBC based device registry
* #3922: [IoT] Allow to enable tracing
* #4044: Ensure agent uses bi-directional AMQP idle-time-out for all connections
* #4076: Avoid memory leak if connected router is disconnected
* #4117: Add support for configuring console to use impersonation
* #4142: [IoT] Refactor device registry, split-up connection and services
* #4172: [Kubernetes] Use headers for oauth-proxy to populate user information (for console/audit).
* #4224: oauth2_proxy is now hosted by a new organisation (https://oauth2-proxy.github.io/oauth2-proxy/)
* #4232: Allow configuring standard authentication service in HA mode
* #4299: Bump vertx from 3.8.3 to 3.9.0
* #4315: [IoT] Add the ability to configure TLS options
* #4314: [IoT] Add alerts for infrastructure and projects
* #4438: Address space connector - offer SASL ANONYMOUS if it cannot offer PLAIN or EXTERNAL.
* #4439: [IoT] Squash adapters and device registry implementations into a single container image each
* #4442: Disregard incoming remote target on receiving links initiated by the plugin (#4495)
* #4455: Qpid Dispatch Router upgraded to 1.12.0
* #4499: [IoT] Allow to use X509 client certificates for device authentication
* #4504: Bump gqlgen from v0.10.2 => v0.11.3
* #4510: Implement a GraphQL messaging endpoint API as a facade over addressspaces #4510
* #4546: Refactor GraphQL purgeAddress to permit multiple target addresses
* #4549: Implement GraphQL closeConnections for brokered and standard
* #4555: Enforce presence of routeServicePort in route endpoints definitions 
* #4574: Set globalMaxSize to 1/4 of broker JVM heap
* #4567: Refactor GraphQL deleteAddressSpace and deleteAddress to accept many target objects
* #4569: [IoT] Allowing configuring log levels of IoT infrastructure components
* #4576: [IoT] Rename the status section field `phaseReason` to `message` to better align with others
* #4592: [IoT] Externalize infrastructure secrets from `IoTConfig` to actual `Secrets`
* #4604: AddressSpaces CRD reported as NonStructuralSchema
* #4610: Extend addressspaceschema to enumerate endpoint types etc (#4590)
* #4656: Defining a connector before its referenced secret exists results in NPE and permanently unready address space
* #4600: [IoT] Allow configuring Pod affinities
* #4612: Prevent the router from racing with the agent and thus causing addresses/queues to be autocreated with wrong args.
* #4631: Allow configuring CPU limits for infra
* #4682: Avoid race when processing Artemis management responses when first connecting to broker
* #4721: Standard address space agent erroneously processes addresses of other addressspaces in the same namespace
* #4730: Enhance brokered address space's address controller (agent) to populate address plan status
* #4753: Resizing broker volumes broken on OpenShift 4
* #4823: Update snakeyaml/hibernate validation dependencies to latest micro releases.
* #4825: Bump Jackson 2.10.0 -> 2.10.4

## 0.31.4
* #4704: [OpenShift 3.11] Console returns 500 internal error when configured with custom certificate
* #4773: Error creating topic when address space contains connectors
* #4756: Increased severity levels on router alerts
* #4710: Add upgrade pre-check when installing via ansible
* #4741: Set global address space plan for create address buttons


## 0.31.3
* #4650: Install uncaught exception handler in address-space-controller and standard-controller

## 0.31.2
* #4098: Added alert for high broker address memory usage
* #4305: Inject OpenShift generated custom CA trust bundle into console pod so that console authentication works when a custom CA is in use.
* #4342: address stuck unready - failed to retrieve addresses: null (##4345)
* #4358: broker init.sh stop script execution of a script if a command has an error (#4359)

## 0.31.1
* #4046: add network-only as fetch policy for all queries 
* #4090: Provide per-address space support credentials facilitating support access to the broker(s) (#4276)
* #4184: Sometimes controller crashes when reconciling authentication service 
* #4241: Fix a race in upgrader logic that prevented upgrade from continuing
* #4242: #4246: Delegate authentication vrtx worker thread pool making authentication multi threaded.  Improve error handling.
* #4245: standard-auth - use AMQP port probe rather than keycloak HTTP endpoint
* #4252: Add idleTimeout, role and maxFrameSize settings for connectors
* #4266: Add broker.globalMaxSize to the infraconfig CRDs 
* #4269: bump Netty dependency from netty-4.1.45.Final to netty-4.1.48.Final
* #4279: handled null check for address space plan (#4277)
* #4280: Edit Address Space dialogue subtitle misleading 
* #4282: add null check for links in address links page (#4262)
* #4287: Address links not displayed if address resource name suffix does not match address name
* #4292: Escape forbidden chars in filters
* #4317: Addresses with different casing is not becoming ready 
* #4331: Issues installing monitoring resources (#4200) (#4222)
* #4332: Unable to drain kube node when broker pod is deployed there


## 0.31.0
*  Adding example partitioned/sharded queue example plans
*  #3686: Add metrics endpoint to controller-managerxs
*  #2714: Allow setting security context of pods using persistent volumes
*  #3622: Manage iot projects in enmasse-operator and remove iot-operator deployment
*  #3407: AddressSpace, Address and MessagingUser resources are now backed by Custom Resource Definitions (CRD) rather than api-server:
   * On upgrade, existing address spaces, addresses and users in the standard authservices will be converted to CRDs.
   * The api-server is removed from the install. The enmasse-operator will automatically remove it when upgrading.
   * Some of the resource validation that was taking place in the api-server has been replaced with a resource status check and error message.
   * The address space schema resource has been changed to a cluster-wide CRD. Users will not be able to read/list it without addition permissions.

*  #3621: Add status section for plans and authentication services
*  Qpid Dispatch Router upgraded to 1.10.0
*  ActiveMQ Artemis upgraded to 2.11.0
*  #3657 Configure router pod anti-affinity by default
*  Upgrade to Infinispan 9.4.16
* #3729: Add relatedImages to operator manifest for disconnected install
* #3824: Bump netty to 4.1.45.Final
* Change default behavior of handling REJECT outcome in broker from putting on DLQ to retrying
  indefinitely.
* Change default behavior of responding with MODIFIED outcome for transient delivery errors.
* Change the service admin example role so it can modify authenticationservices custom resources.
* #4005: Perform additional checks on broker to ensure addresses and queues are present

* #3673: Add support for pod disruption budgets
* #3764: validate the topic refenced by a subscription actually exists
* #3868: Add support for specifying maxConsumers for a subscription
* #3873: Require TLSv1.2 or newer for external TLS endpoints
* #3867: Add addition checks for router mesh status: Address CRD now validates the spec.address field comprises permitted characters.
* #3949: Address unsafe publication to volatiles used to form the schema
* #3981: Timeout when deleting address space
* #3541: Allow addressspace spec changes
* #4131: Ensure the watch is restarted on all paths (#4132)
* #4134: Ensure address watch respects 600s timeout (#4134)

## 0.30.3
* #4049: (Agent) Apply HTTP timeout to kubernetes list, create, update, delete requests (#4050)
* #4119: Use @PreMatching annotation of the api-server request log to ensure that 404 are processed without NPE.
* #4131: (Agent) Ensure the watch is restarted on all paths (#4132)
* #4147: Perform basic address validation for both address space types (#4147)

## 0.30.2
*  #2714: Allow setting security context of pods using persistent volumes
*  #3547: Network policies not deleted when address space is deleted
*  #3542: Add resync period for agent watches
*  #3463: Fix permission typo on 0.30 

## 0.30.0
* Fix go tool vet on newer versions of go
* #3247: Fix path to probe endpoint used when running on Kubernetes
* Add support for LoRaWAN and Sigfox
* Deploy IoT operator automatically when the IoTConfig is created
* Drop IoT garbage collector deployment in favor of using finalizers to clean up
* Implement device registry authentication based on Kubernetes user
* Upgrade to Apache ActiveMQ Artemis 2.10.1
* Upgrade to Apache Qpid Dispatch Router 1.9.0
* Add support for connecting address spaces to external AMQP endpoints (standard address space)
* Add support for forwarding messages between external endpoints and local queues (standard address space)
* Add scripts for performing controlled rolling restart

## 0.29.0
* Upgrade to Apache ActiveMQ Artemis 2.9.0
* Upgrade to Apache Qpid Dispatch Router 1.8.0
* Upgrade to Hono 1.0-M5
* Updated Address Go type to add Status

## 0.28.2

* Respect custom routes when building the redirect list for the oauthclient (#2855) (fixes regression introduced at 0.28.0)
* Fix a possible NPE, having an empty spec section in the infra configs
* Turn off Artemis DLQ/max-delivery-attempts (#2927)
* Ensure that imagePullPolicy to properly propagated to standard address space resources (#2921)
* Allow liveness/readiness probe timeouts to be configured from the infraconfigs (#2864)
* Allow broker/router container environment to be overridden from the podspec on the infraconfig. (#2882)

## 0.28.1
* Improve the console operator, add readiness/liveness probes to Console's Apache HTTPD
* Improve Kubernetes/OpenShift detection in Go-based components
* Guard against unexpected queue depth values (ARTEMIS-1982) when determining if a Broker is drained
* Corrected an Agent defect that could leave a address in an permanently unready state
* Improve upgrade of pre-0.27 authentication services
* Console UI corrections (typos/UI improvements)

## 0.28.0
* New global console for managing address spaces that uses OAUTH/OIDC for console authentication and Kubernetes RBAC for authorization.
* Change per-address space console to use the same authentication and authorization mechanisms as the global console
* Support for shard multiple queues across the same set of brokers
* Support for specifying router policies
* Upgrade to qpid dispatch router 1.7.0
* Support for adjusting idle timeouts, worker threads of router and broker connections

## 0.27.0
* Support for deploying via Operator Lifecycle Manager 
* Experimental support for integrated IoT service based on Eclipse Hono
* Support for creating authentication services via CRDs
* Support for affinity, tolerations and priority classes for messaging infrastructure
* Support for exporting endpoint info to configmap, secret or service
* Support for PATCH operation for AddressSpace, Address and MessagingUser resources 
* Upgrade to Apache ActiveMQ Artemis 2.6.4
* Upgrade to Apache Qpid Dispatch Router 1.6.0
* Upgrade to Keycloak 4.8.3

## 0.26.3
* Fixes an issue where the storage class name would not get set for persistent volume claims in the standard address space.
* Upgrade to Apache ActiveMQ Artemis 2.6.4.
* Upgrade the Qpid Dispatch Router base image (to qdrouterd-base:1.5-DISPATCH-1273).  This fixes a Router defect whereby the 'to' field of a message is not authorized against valid targets for anonymous senders.

## 0.26.2
* Fix issue in infra config versions
* Prevent use of user API to throw errors when using none authservice

## 0.26.0
* Support for switching address space plans
* Support for switching address plans for `anycast`, `multicast`, and `queue` address types in standard address space
* Readiness on initial configuration for routers in standard address space. This ensures that routers can only be connected to once they have receive initial configuration, and will allow a HA configuration (> 1) routers to upgrade without downtime for the service.
* Router configuration now enforces authenticatePeer: yes, requiring clients to perform SASL handshake
* Allow querying all EnMasse resources cluster wide
* Allow listing connections by container id by clicking on link in Addresses pane in the console
* Update all API versions from v1alpha1 to v1beta1. v1alpha1 is now deprecated for all EnMasse custom resources and will get removed.
* Bugfix: allow address full policy to be set for brokers in standard address space
* Allow clients to authenticate as service account users by supplying @@serviceaccount@@ as the username and the token as the password.

## 0.25.0
* Prometheus metrics and alerts for address health are now exposed
* Pooled broker deployments now use N deployments with 1 broker instead of 1 deployment with N brokers. This is to handle a resize bug in previous versions. When upgrading, take care to drain all queues first to avoid message loss.
* Improved command line output of addresses and address spaces with more tabular information about the resource(s)
* Allow configuring storageClass used for broker volumes
* Add support for serviceaccount authentication type for users
* Allow resizing volumes if configured (and underlying provider supports it)
* Add support for nodeAffinity preferred labels for operators and messaging infra
* Add support for configuring network network policies for address space infrastructure
* Add support for changing address space plans
* Split broker image into a 'broker' and 'broker-plugin' image run as initContainer image
* Bugfixes in address handling in standard-controller

## 0.24.0 (...)
* Support automatic upgrades of EnMasse based on infrastructure configuration versions
* Update default set of address space plans and address plans to better show their use
* Allow changing router and broker configuration using infrastructure configuration CRDs
* Simplify console and use consistent wording + fixes to console UI filtering and stats reporting
* Support configuring load balancer endpoints when creating address space
* Support generating certs signed by OpenShift Cluster CA
* Support providing certs for endpoints when creating address space
* Improve monitoring using prometheus alertmanager for alerting on components down or address space not ready.
* Upgrade to Artemis 2.6.3
* Upgrade to Qpid Dispatch Router 1.4.0

## 0.23.2 (October 25, 2018)
* Allow overriding INFRA_UUID annotation on AddressSpace
* Update to Qpid Dispatch Router image with memory leak fix

## 0.23.0 (October 2, 2018)
* Deploy EnMasse to a single namespace. This removes the need to create new namespaces/projects.
* Simplify installation process. Release tarball now comes with OpenShift templates and simple to use bundles. Ansible still supported, but not required to performaed advanced install.
* Use reencrypt routes for http(s) services on OpenShift (signed by OpenShift cluster CA)
* Allow CA cert to be omitted for external authentication services (using trusted root CAs instead)
* Bugfixes and improvements to console UI such as improving stats reporting, removing unneeded UI elements, improved validation of user input
* Improvements to documentation. Documentation now included in release tarball.
* Simplify Console -> Keycloak -> OpenShift login flow. No longer showing keycloak login if using OpenShift and standard authentication service (by default)

## 0.22.0 (September 10, 2018)
* New address type ‘subscription’ available in standard address space. This can be used to create durable subscription queues.
* New API for managing users in standard authentication service using custom resources (kubectl/oc).
* Routers static config from configmap
* Several bugfixes to mqtt components

## 0.21.1 (August 15, 2018)
* Upgrade to Apache Qpid Dispatch Router 1.3.0
* Upgrade to Apache ActiveMQ Artemis 2.6.2
* Enable native OpenSSL support in Artemis
* Use one connection in each direction between router and broker
* Split Ansible playbooks into a project admin and cluster admin playbook

## 0.21.0 (June 19, 2018)
* Upgrade to dispatch router 1.1.0 + some extra bugfixes
* Add support for web sockets in standard address space
* Set common name and subject alternative names for self-signed route certs on OpenShift
* Bug fixes to allow console and rest api to work better on addresses created by the other
* Bug fix to topic-forwarder allowing queues and topics with multiple shards to work again
* Allow kubernetes - api server connection to be secured using mutual TLS

## 0.20.0 (May 23, 2018)
* REST API now served by a new component, api-server, code moved from the old address-controller.  The intention is to have this act as a kubernetes API server, allowing custom resource support for address spaces and addresses. This is experimental at present and not ready for production use[2]. An example ansible inventory file that can be used with the ansible install procedure[3] is provided.
* REST API paths have changed in preparation for custom resources support. An API reference available at [4]. A short summary:

     * The `/apis/enmasse.io/v1` prefix has changed to `/apis/enmasse.io/v1alpha1` which better reflects the state of our APIs in terms of breaking changes.
     * Address spaces are now namespaced, which means that you can create address spaces with the same name in different namespaces. This means the path to address space resources are now `/apis/enmasse.io/v1alpha1/namespaces/[:namespace]/addressspaces`
     * The path `/apis/enmasse.io/v1/addresses` API has 'moved' to /apis/enmasse.io/v1alpha/addressspaces/[:addressspace]/addresses`.
     * A new API under `/apis/enmasse/v1alpha1/namespaces/[:namespace]/addresses` have been introduced to support custom resources for addresses.
* address-controller renamed to address-space-controller
* Kubernetes and OpenShift resources/templates are no longer split into separate folders, but now categorized by component. This means that the different components like authentication service, address space controller, rest api etc. can be managed and upgraded individually if desired which simplifies operations in cases where you want more control.
* Performance improvements to handling large number of addresses in agent
* Removal of per-address resource limits in broker as there was a lower limit than expected on how many addresses and limit settings a broker could handle
* Bug fixes and enhancements to console, address space controller, agent and more

## 0.19.0 (April 23, 2018)
* Support OpenShift identity brokering in Keycloak. This allows you to authenticate an address space
  admin user using OpenShift credentials.
* Create common groups for authorization and have realm admin user join these groups
* Reworked service broker implementation to support deploying address spaces
* Add support for service broker bind requests to create user and credentials
* Upgraded to Artemis 2.5.0 broker

## 0.18.0 (April 10, 2018)
* Support OAUTH (when used with standard or external address space) in messaging console
* Performance improvements to reduce load on the Kubernetes/OpenShift API server
* Per-address memory usage limits on queues/topics in standard address space. This is based on the amount of memory configured for the broker and the credits used in the address plans. 
* Allow configuring standard authentication service using configmap

## 0.17.1 (February 22, 2018)
* Bug fixes to console UI
* Update router image with bugfixes
* Improve status checks to cover pooled brokers
* Added standard address space plan without MQTT components
* Support using wildcard certificates for external routes

## 0.17.0 (February 22, 2018)
* Support for deploying EnMasse on OpenShift using Ansible
* Add support for address space plans, which provides a way to configure address space quotas.
* Add support for address plans, which provides a way to configure resources requirements for a
  given address.
* Automatically scale router and broker based on address resource requirements (in standard address
  space only)
* Add resource definitions, which allows a higher degree of configurability of routers and brokers
* Add support for colocated topics (allows a minimum footprint of 1 router and 1 broker handling many queues and topics)
* Change address model to require 'address' and 'plan' fields to be set, and make 'name' field optional
* Expose status of addresses in console

## 0.16.0 (January 30, 2018)
* Support for authorization at address level. To enable this, create groups in keycloak on the form 'send_*' and 'recv_*', and have users join a particular group to allow sending or receiving from a particular address (wildcards if you want to allow on all addresses). More detailed docs will follow
* Ability to deploy keycloak-controller standalone in order to automatically manage an  external keycloak instance
* Enable hawtio console for brokers for easier debugging broker state. The console is authenticated against keycloak.
* Configserv is removed from standard address space. This lowers the footprint and complexity.
* Add prometheus and grafana addons that can be used to monitor the messaging cluster, including routers and brokers
* Add prometheus endpoint to broker and remove hawkular support
* Use statefulsets for brokers in standard address space

## 0.15.3 (December 8, 2017)
* Bug fixes to most components found in testing
* The router in the standard address space now rejects unknown addresses rather than defaulting to
  'anycast' behavior
* Tune roles and permissions required to run in a shared OpenShift cluster so that it doesn't
  require access to all projects in a cluster in order to work.
* Update JDK version in Keycloak image
* Improvements to console documentation
* Added manual deployment process to online documentation
* Version artifacts and pom files, rewrite on release
* Report k8s events in address-controller and agent controller loops
* Automatically create keycloak realm admin user
* Add OpenWire and CORE port to brokered address space

## 0.15.0 (November 22, 2017)
* Add cluster roles that limit privileges required to run address controller
* Add HTTPS support for standard authentication service
* Use persistent volumes for standard authentication service
* Support multiple address controllers on the same cluster
* Enable HTTPS for REST API and console
* Authenticate REST API using RBAC
* Move REST API path from /v1 to /apis/enmasse.io/v1 to support aggregated API service
* Upgrade broker to Apache Artemis 2.4.0
* Upgrade keycloak to 3.3.0
* Lots of bug fixes to console
* Replace use of Ingress with K8S LoadBalancer Service

## 0.14.0 (November 3, 2017)
* New address space type: brokered
    * A single ActiveMQ Artemis broker
    * Supports JMS features such as transactions, message groups, queue selectors etc.
    * Integrates with the authentication service
    * A lightweight agent managing addresses + the EnMasse console
* Add support for multitenant mode when running in Kubernetes
* A ton of bug fixes to all EnMasse components found with an expanded test suite
* Initial version of OpenAPI specification
* Lots of new documentation at enmasse.io/documentation
* Console renamed to agent in preparation for it to adopt multiple 'admin' functions
* Support keycloak groups in the standard authentication service

41 [issues](https://github.com/EnMasseProject/enmasse/milestone/4?closed=1) has been resolved for
this release.

## 0.13.0 (September 22, 2017)
* Added support for authentication. Users can now choose from 'none', 'standard' and 'external' as
  authentication services. See [authentication design doc](https://github.com/EnMasseProject/enmasse/blob/master/documentation/design/authentication.adoc)
  for details. The authentication services are used by both AMQP, MQTT and console endpoints.
* Secure *all* internal communication using TLS with server + client authentication. This ensures
  that no other component than those trusted by the EnMasse infrastructure can access services
  inside EnMasse.
* REST API for creating/deleting/listing address spaces, see [resource
  definitions](https://github.com/EnMasseProject/enmasse/blob/master/documentation/address-model/resource-definitions.adoc)
  for details.
* Use Apache ActiveMQ Artemis 2.2.0 as broker

35 [issues](https://github.com/EnMasseProject/enmasse/milestone/3?closed=1&page=1) has been resolved
for this release.

## 0.12.0 (August 4, 2017)
* Introduction of the [new address model](documentation/address-model/model.md), which changes how
  addresses are configured in EnMasse. This change comes from design work that has been done by
  the developer team in the past months, which made it apparent that we need to make the model more
  flexible in order to support other topologies than the pure router-based we have today.
* Integration with the OpenShift Service Catalog as an easy way to provision messaging on OpenShift.
* Improved status reporting for addresses. When querying the address controller for an Address,
  the status will now report ready: false if the routers are not yet configured with that address.
* Add support for external load balancers when running on Kubernetes (see templates/install/kubernetes/addons/external-lb.yaml)
* Use Apache Artemis 2.1.0 release as base for the broker
* Bugfixes to address-controller, configserv, console, queue-scheduler and router agent.

## 0.11.2 (June 2, 2017)
* Minor bugfixes to console and templates

## 0.11.0 (May 29, 2017)
* Remove restrictions on address names. These were earlier restructed by k8s labels
* Add simple logging library to router agent and subscription service
* Update Artemis broker flavor to 2.1.0
* New install 'bundle' containing both scripts and templates for deploying EnMasse
* Simple template for exposing Kubernetes services on cloud providers without ingress controllers

## 0.10.0 (May 3, 2017)
* Merged non-TLS and TLS-based templates into one, making EnMasse TLS-enabled by default. This
  simplifies deployment of EnMasse but also the maintenance of the templates.
* Guide for deploying EnMasse on AWS

## 0.9.0 (April 27, 2017)
* Various UI fixes to the console for demo purposes
* Improved documentation for deploying EnMasse with Open Service Broker API
* Dashboard for router metrics

## 0.8.0 (April 21, 2017)
* Support for pushing router metrics to [Hawkular](http://www.hawkular.org/) using the [Hawkular OpenShift Agent](https://github.com/hawkular/hawkular-openshift-agent)

## 0.7.0 (April 18, 2017)
* Support for pushing broker metrics to [Hawkular](http://www.hawkular.org/) using the [Hawkular OpenShift Agent](https://github.com/hawkular/hawkular-openshift-agent)
* A messaging console providing an overview of addresses and per-address metrics
* Support for deploying EnMasse on Kubernetes

## 0.5.0 (March 23, 2017)

* MQTT Last Will and Testament Service
* Basic support for multiple isolated address spaces with their own routers and brokers
* Support for the [Open Service Broker API](https://www.openservicebrokerapi.org/)
