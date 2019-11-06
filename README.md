<a href="https://github.com/EnMasseProject/enmasse"><img src="https://raw.githubusercontent.com/EnMasseProject/enmasse/master/documentation/_images/logo/enmasse_logo.png" width="50%" /></a>

[![Systemtest Status](https://travis-ci.org/EnMasseProject/enmasse.svg?branch=master)](https://travis-ci.org/EnMasseProject/enmasse)
[![GitHub release](https://img.shields.io/github/release/EnMasseProject/enmasse.svg)](https://github.com/EnMasseProject/enmasse/releases/latest)
[![Twitter Follow](https://img.shields.io/twitter/follow/enmasseio.svg?style=social&label=Follow&style=for-the-badge)](https://twitter.com/enmasseio)
[![Gitter](https://badges.gitter.im/EnMasseProject/community.svg)](https://gitter.im/EnMasseProject/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

----

EnMasse provides a self-service messaging platform on Kubernetes and OpenShift with a uniform interface to manage different messaging infrastructure.

#### Features

* **Self-service messaging for applications** - The service admin deploys and manages the messaging infrastructure, while applications can request messaging resources without caring about the messaging infrastructure.

* **Supports a wide variety of messaging patterns** - Choose between JMS-style messaging with strict guarantees, or messaging that supports a larger number of connections and higher throughput.

* **Great protocol support** - Support protocols available in the underlying messaging infrastructure: AMQP 1.0, MQTT, OpenWire, CORE and STOMP.

* **Built-in authentication and authorization** - Use the built-in or plug in your own authentication service for authentication and authorization of messaging clients.

* **Uniform interface to manage messaging infrastructure** - Manage standalone broker instances or a scale-on-demand AMQP message bus using the same cloud-native APIs.

See our [website] for more details about the project.

---- 

![EnMasse Intro 1](https://raw.githubusercontent.com/enmasseproject/enmasse/master/documentation/_images/enmasse-intro-1.gif)

## To start using EnMasse

See the documentation on our [website].

## To start developing EnMasse

See [developing] for information on how to get started building EnMasse.

See [contributing] for information on how to contribute to EnMasse.

## Support

If you need support, reach out to us via the [mailinglist] or on [Gitter].

See [contributing] for more info on how to get help from the community.

If you run into issues, don't hesitate to raise an [issue].

[website]: https://enmasse.io
[contributing]: CONTRIBUTING.md
[developing]: HACKING.md
[mailinglist]: https://www.redhat.com/mailman/listinfo/enmasse
[Gitter]: https://gitter.im/EnMasseProject/community
[issue]: https://github.com/EnMasseProject/enmasse/issues/new/choose
