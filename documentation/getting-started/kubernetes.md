# EnMasse on Kubernetes

This guide will walk through the process of setting up EnMasse on a Kubernetes
cluster together with clients for sending and receiving messages. Some of the concepts like
addresses and flavors are described in more detail [here](openshift.md). Currently, TLS is not
supported on Kubernetes.

## Preqrequisites

This guide assumes that you already have a Kubernetes cluster installed. If not, have a look at
[minikube](https://github.com/kubernetes/minikube), which is kind of kubernetes-in-a-box.

The address-controller and console is configured using ingress, so enable the ingress controller in
your minikube setup:

    minikube addons enable ingress

This will cause an nginx ingress controller to get launched.

## Setting up EnMasse

### Creating project and importing template

Download one of the releases from https://github.com/EnMasseProject/enmasse/releases and unpack it.
Once unpacked, you can either deploy EnMasse using an automated script or follow the below steps.

#### Deploying EnMasse automatically

The deployment script simplifies the process of deploying the enmasse cluster. You
can invoke it with `-h` to get a list of options. To deploy:

    ./deploy-kubernetes.sh -m "https://localhost:8443" -n enmasse

This will create the deployments required for running EnMasse. Starting up EnMasse will take a while,
usually depending on how fast it is able to download the docker images for the various components.
In the meantime, you can start to create your address configuration.

#### Deploying EnMasse manually

Create service account for address controller:

    kubectl create sa enmasse-service-account -n enmasse

Create self-signed certificate:

    openssl req -new -x509 -batch -nodes -out enmasse-controller.crt -keyout enmasse-controller.key

Create secret for controller certificate:

    cat <<EOF | kubectl create -n enmasse -f -
    {
        "apiVersion": "v1",
        "kind": "Secret",
        "metadata": {
            "name": "enmasse-controller-certs"
        },
        "type": "kubernetes.io/tls",
        "data": {
            "tls.key": "$(base64 -w 0 enmasse-controller.key)",
            "tls.crt": "$(base64 -w 0 enmasse-controller.crt)"
        }
    }
    EOF

Deploy EnMasse to enmasse:

    kubectl apply -f ./kubernetes/enmasse.yaml -n enmasse


### Deploying external load balancers

If you're running EnMasse in your own Kubernetes instance on any of the cloud providers, and if you don't have or don't want to deploy an ingress controller, you can deploy the external load balancer services to expose EnMasse ports:

	kubectl apply -f kubernetes/addons/external-lb.yaml -n enmasse

### Configuring addresses using the console

The EnMasse console should be available at `http://$(minikube ip)/`. You can create and
monitor queues and topics using the UI.

### Exposing messaging through Ingress resource

To open up for AMQP using minikube and the nginx ingress controller, follow [this](https://github.com/kubernetes/contrib/tree/master/ingress/controllers/nginx/examples/tcp) and [this](https://github.com/kubernetes/ingress/tree/master/controllers/nginx#exposing-tcp-services) guides, using the port `5672` and the service `default/messaging`.

### Sending and receiving messages

For sending and receiving messages, have a look at an example python [sender](http://qpid.apache.org/releases/qpid-proton-0.15.0/proton/python/examples/simple_send.py.html) and [receiver](http://qpid.apache.org/releases/qpid-proton-0.15.0/proton/python/examples/simple_recv.py.html).

To send and receive messages, you can use the minikube IP:

    ./simple_send.py -a "amqp://$(minikube ip):5672/myqueue" -m 10

This will send 10 messages. To receive:

    ./simple_recv.py -a "amqp://$(minikube ip):5672/myqueue" -m 10

You can use the client with the 'anycast' and 'broadcast' and 'mytopic' addresses as well.

## Conclusion

We have seen how to setup a messaging service in Kubernetes, and how to communicate with it using python example AMQP clients.
