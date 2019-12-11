
The example configuration for Infinispan comes in three different flavors:

* Tiny
* Small
* Medium

In order to deploy Infinispan, you will need to choose on of those profiles,
and then deploy the `common` folder, as well as the profile specific folder.

For each folder, you also have the ability to manually deploy Infinispan, or use
the operator. So you need to choose either `manual` or `operator` as a sub-folder.
*Do not* deploy both `manual` and `operator`.

**Note:** At the moment the "operator" deployment doesn't work due to restrictions on
          the Infinispan side. That is why currently it is only possible to use the
          "manual" deployment.

## Example

Assume you want to deploy the `small` profile manually:

    kubectl apply -f common/common
    kubectl apply -f common/manual
    kubectl apply -f small/common
    kubectl apply -f small/manual
