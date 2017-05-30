# Address controller

The address controller is actually more than just an address-controller. Here are the main tasks
performed by the address-controller:

    * API server for managing instances, addresses and flavors
    * API server for Open Service Broker API
    * Controller for taking action based on instances (create/destroy enmasse infrastructure and managing routing + certs)
      etc.)
    * Controller for taking action based on addresses (create/destroy broker deployments)
