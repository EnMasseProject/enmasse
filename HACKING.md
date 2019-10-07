# Developing EnMasse

## Build requirements

To build EnMasse, you need

   * [JDK](http://openjdk.java.net/) >= 11
   * [Apache Maven](https://maven.apache.org/) >= 3.5.4
   * [Docker](https://www.docker.com/)
   * [GNU Make](https://www.gnu.org/software/make/)
   * [Asciidoctor](https://asciidoctor.org/) >= 1.5.7
   * [Go](https://golang.org/) > 1.10.0

*Note*: On OSX, make sure you have [Coreutils](https://www.gnu.org/software/coreutils/) installed, e.g. `brew install coreutils`

The EnMasse java and node modules are built using maven. All docker images are built using make.

## Runtime requirements

To run EnMasse you need a Kubernetes cluster. Most EnMasse developers use [OKD](https://www.okd.io/)
for running tests on their machine.

## Checking out for Go

If you want to work with the Go parts of this repository, you will need to perform the
following steps.

### Create a Go workspace

Create a new directory and set the `GOPATH` environment variable:

    export GOPATH=/home/user/my-enmasse-gobase
    mkdir -p $GOPATH/src/github.com/enmasseproject

### Clone the git repository into this workspace

    cd $GOPATH/src/github.com/enmasseproject
    git clone https://github.com/enmasseproject/enmasse

## Building

### Pre-installation

*Note*: Make sure docker daemon is in running state.

#### Full build, run unit tests

    make

This can be run at the top level or within each module. You can also run the 'build', 'test', and 'package' targets individually.

#### Building docker images

    make docker_build

#### Full build and pushing docker images to a registry

    export DOCKER_ORG=myorg
    export DOCKER_REGISTRY=quay.io
    export TAG=v1.0.3 # Optional: 'latest' by default

    docker login -u myuser -p mypassword $DOCKER_REGISTRY

    make buildpush

*Note*: If you are using OKD and 'oc cluster up', you can push images directly to the builtin registry
by setting `DOCKER_ORG=myproject` and `DOCKER_REGISTRY=172.30.1.1:5000` instead.

#### Deploying to a Kubernetes instance assuming already logged in with cluster-admin permissions

*Note*: This assumes you have [OpenSSL](https://www.openssl.org) installed.

```
kubectl create namespace enmasse-infra
kubectl config set-context $(kubectl config current-context) --namespace=enmasse-infra

mkdir -p api-server-cert
openssl req -new -x509 -batch -nodes -days 11000 -subj "/O=io.enmasse/CN=api-server.enmasse-infra.svc.cluster.local" -out api-server-cert/tls.crt -keyout api-server-cert/tls.key
kubectl create secret tls api-server-cert --cert=api-server-cert/tls.crt --key=api-server-cert/tls.key

kubectl apply -f templates/build/enmasse-latest/install/bundles/enmasse
kubectl apply -f templates/build/enmasse-latest/install/components/example-plans
kubectl apply -f templates/build/enmasse-latest/install/components/example-authservices
```

#### Deploying to an OKD instance assuming already logged in with cluster-admin permissions

```
oc new-project enmasse-infra || oc project enmasse-infra
oc apply -f templates/build/enmasse-latest/install/bundles/enmasse
oc apply -f templates/build/enmasse-latest/install/components/example-plans
oc apply -f templates/build/enmasse-latest/install/components/example-authservices
```

#### Running smoketests against a deployed instance

    make SYSTEMTEST_ARGS=SmokeTest systemtests

### Running full systemtest suite

#### Running the systemtests

    make systemtests

#### Run single system test

    make SYSTEMTEST_ARGS="shared.standard.QueueTest#testCreateDeleteQueue" systemtests

### Adding / Updating go dependencies

This project currently uses "glide" to vendor go sources. Change dependencies in the file `glide.yaml` and then run:

    glide up -v
    git add --all vendor
    git add glide.lock
    git commit

## Reference

This is a reference of the different make targets and options that can be set when building an
individual module:

#### Make targets

   * `build`        - build
   * `test`         - run tests
   * `package`      - create artifact bundle
   * `docker_build` - build docker image
   * `docker_tag`   - tag docker image
   * `docker_push`  - push docker image
   * `buildpush`    - build, test, package, docker build, docker_tag and docker_push
   * `systemtests`  - run systemtests

Some of these tasks can be configured using environment variables as listed below.

#### Debugging Java Code on OpenShift or Kubernetes

To enable debug mode for the Java based components, it's necessary to setup following environment variables:

   * `JAVA_DEBUG` - set to true to enable or false to disable
   * `JAVA_DEBUG_PORT` - 8787 by default and can be any value above 1000 if need to change it

Use this command to change environment variables values for the deployment

    $CMD set env deployments/<deployment-name> JAVA_DEBUG=true

Where $CMD is `oc` or `kubectl` command depends of the environment.

The following deployment names are available depending on their types and EnMasse configuration:

   * `address-space-controller`
   * `admin`
   * `api-server`
   * `keycloak-controller`
   * `standard-controller`
   * `service-broker`
   * `topic-forwarder`
   * `mqtt-gateway`
   * `mqtt-lwt`

For forwarding port from the remote pod to the local host invoke following command (it will lock terminal) and then
connect with development tool to the forwarded port on localhost

   $CMD port-forward $(oc get pods | grep <deployment-name> | awk '{print $1}') $JAVA_DEBUG_PORT:$JAVA_DEBUG_PORT

#### Go unit tests

Go unit tests output can be converted into xUnit compatible format by a tool named `go2xunit`. This is
being used by the build to have a combined test result of Java and Go parts.  You can enable the conversion process by setting the make variable `GO2XUNIT` to the go2xunit binary. In this case the build will execute `go test` and convert the results.

#### Environment variables

There are several environment variables that control the behavior of the build. Some of them are
only consumed by some tasks:

   * `KUBERNETES_API_URL`   - URL to Kubernetes master. Consumed by `systemtests` target
   * `KUBERNETES_API_TOKEN` - Kubernetes API token. Consumed by `systemtests` target
   * `KUBERNETES_NAMESPACE` - Kubernetes namespace for EnMasse. Consumed by `systemtests` targets
   * `DOCKER_ORG`           - Docker organization for EnMasse images. Consumed by `build`, `package`, `docker*` targets. tasks. Defaults to `enmasse`
   * `DOCKER_REGISTRY`      - Docker registry for EnMasse images. Consumed by `build`, `package`, `docker_tag` and `docker_push` targets. Defaults to `quay.io`
   * `TAG`                  - Tag used as docker image tag in snapshots and in the generated templates. Consumed by `build`, `package`, `docker_tag` and `docker_push` targets.

## Debugging

### Remote Debugging

In order to remote debug an EnMasse component deployed to a pod within the cluster, you first need to enable the remote
debugging options of the runtime, and the forward port from the host to the target pod.  You then connect your IDE's
debugger to the host/port.

The instructions vary depending on whether the component is written in Java or NodeJS.  The precise steps vary by
developer tooling you use.  The below is just offered as a guide.

#### Java components

If you have a Java component running in a pod that you wish to debug, temporarily edit the deployment
(`oc edit deployment` etc.) and add the Java debug options to the standard `_JAVA_OPTIONS` environment variable to the
container.

```yaml
- env:
 - name: _JAVA_OPTIONS
   value: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
```

#### NodeJS components

If you have a NodeJS component running in a pod that you wish to debug, temporarily edit the deployment and add the
NodeJS debug option `--inspect` to a `_NODE_OPTIONS` environment variable to the container.  By default, NodeJS
will listen on port 9229.

```yaml
- env:
 - name: _NODE_OPTIONS
   value: --inspect
```

#### Port Forwarding

On OpenShift, you can then issue a `oc port-forward <pod> <LOCAL_PORT>:<REMOTE_PORT>` command to conveniently route
traffic to the pod's bound port.  Attach your IDE debugger the host/port.

# Releasing EnMasse

When releasing EnMasse, be sure to enable the `-Prelease` profile so that third-party license information
and source bundle is created.
