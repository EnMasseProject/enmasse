## scope

### address controller api

Authenticates against openshift/kubernetes userbase.

### service broker/service catalogue

As above.

### enmasse console

The console is a per address-space service and will authenticate
against the address-spaces userbase. For users with appropriate
permission it will allow creation or deletion of addresses via the
address-controller API for which it will use a service account it has
been configured with.

### router & gateways

These are responsible for authenticating AMQP and MQTT connections. At
present all enforcement is actually done at the router level. The MQTT
gateway merely passes on the credentials it receives for each
connection.

### internal components

Internal components authenticated using client certificates signed by
an internal CA. Verify: this is a per address-space CA?

## sasl delegating mechanism

TODO: Rob

## console authorisation

## configuring authentication for an address-space

Three options presented to user:

1. No authentication
2. Use enmasse authentication service
3. Use external authentication service

Deploy two authentication services along with address controller:

1. simple null service that always allows access
  * used for no authentication option above
2. keycloak instance with plugins
  * used for enmasse authentication option above
  * need to setup keycloak realm per address space with admin user

## testing

TODO: Rob
