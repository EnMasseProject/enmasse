# EnMasse on Kubernetes

This guide will walk through the process of setting up EnMasse on a Kubernetes
cluster together with clients for sending and receiving messages. Some of the concepts like
addresses and flavors are described in more detail [here](e2e-example.md). This guide does not
explain how to setup a TLS-enabled EnMasse cluster. See the [OpenShift E2E example](e2e-example.md)
for how to do that.

## Preqrequisites

This guide assumes that you already have a Kubernetes cluster installed. If not, have a look at
[minikube](https://github.com/kubernetes/minikube), which is kind of kubernetes-in-a-box.

## Setting up EnMasse

### Creating project and importing template

Before deploying EnMasse, the enmasse-service-account must be created:
    
    kubectl create sa enmasse-service-account

Then, to deploy EnMasse:
    
    kubectl create -f https://raw.githubusercontent.com/EnMasseProject/enmasse/master/generated/enmasse-kubernetes.yaml

This will create the deployments required for running EnMasse. Starting up EnMasse will take a while,
usually depending on how fast it is able to download the docker images for the various components.

### Configuring addresses using the console

The EnMasse console should be available at `http://$(minikube ip)/console`. You can create and
monitor queues and topics using the UI.

### Exposing messaging through Ingress resource

If you are using minikube, the IP addresses of the internal services will not be accessible from outside of the minikube VM. To open up for AMQP using minikube and the nginx ingress controller, follow [this](https://github.com/kubernetes/contrib/tree/master/ingress/controllers/nginx/examples/tcp) and [this](https://github.com/kubernetes/ingress/tree/master/controllers/nginx#exposing-tcp-services) guides, using the port `5672` and the service `default/messaging`.

### Sending and receiving messages

For sending and receiving messages, have a look at an example python [sender](http://qpid.apache.org/releases/qpid-proton-0.15.0/proton/python/examples/simple_send.py.html) and [receiver](http://qpid.apache.org/releases/qpid-proton-0.15.0/proton/python/examples/simple_recv.py.html).

To send and receive messages, you can use the minikube IP:

    ./simple_send.py -a "amqp://$(minikube ip):5672/myqueue" -m 10

This will send 10 messages. To receive:

    ./simple_recv.py -a "amqp://$(minikube ip):5672/myqueue" -m 10

You can use the client with the 'anycast' and 'broadcast' and 'mytopic' addresses as well.

## Conclusion

We have seen how to setup a messaging service in Kubernetes, and how to communicate with it using python example AMQP clients.
