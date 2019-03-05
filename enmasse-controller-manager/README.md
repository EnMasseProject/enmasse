# EnMasse operator

This is one operator for managing EnMasse specific components.

## Modules

The operator consists of several modules, focusing on different topics. They
can be activated or deactivated from the same binary. This can be done by
using a set of environment variables, listed in the order of processing:

* `CONTROLLER_ENABLE_ALL` – Enable all modules. Overrides everything. Defaults to `false`.
* `CONTROLLER_ENABLE_<MODULE>` – Enable or disable the module `MODULE`. The module is enabled
  if, and only if, the value is `true`. If the environment variable is not set, the check is ignored.
* `CONTROLLER_DISABLE_ALL` – Disable all remaining modules. Defaults to `false`.

By default this will simply enable all modules.

### IoT

The IoT modules has the name `IOT`, and it will process the CRDs for `IoTProject` and `IoTConfig`.
