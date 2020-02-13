# mock-console-server

EnMasse mock console server that serves a static data-set comprising two namespaces, three address spaces, and many
addresses of different types. Address spaces and address can be created, patched or deleted.
Addresses can be purged. Connections can be closed.

TODO:

- How do we signal to the console, in a data driven way, if the user is able to purge an address?
  This may be due to restrictions on the type of object itself (ie. cast address can't be purged), or
  this user does not have permission.

# Running

`node mock-console-server.js`

Navigate to http://localhost:4000/ to use the GraphQL playground. This lets you explore the schema and run queries
dynamically.

# Filters

Most queries accept a `filter` argument. This allows filtering of the results. The filter is specified by a
SQL-92 style `where` clause. JSON-path operands are supported thus allowing filtering of any leaf node of the result
object. A JSON-path operand in the expression are enclosed in backticks.

e.g.

```
 `$.Spec.AddressSpace` = 'jupiter_as1' AND `$.ObjectMeta.Namespace` = 'app1_ns'
```

# Sorting

Most queries accept a `orderBy` argument. This allows the sorting of the results by one or more fields.
The sort clause is specified by a SQL-92 like `order by` clause with JSON-paths identifying the leaf node in the
result that is to be subjected to the sort. Sort order can be `ASC` (ascending - default) or `DESC` (descending).

An ascending sort:

```
"`$.ObjectMeta.Name`"
```

Multiple sort clauses are supported. Separate each clause with a comma.

A two clause sort:

```
"`$.Spec.Type` ,`$.ObjectMeta.Name` desc"
```

# Paging

The queries that may return a large result accept pagination. The pagination
arguments are `first` which specifies the number of rows to be returned and `offset`
the starting index within the result set. The object return provides a count
of the number of rows in the result set in total.


# Environment

The following environment variables are understood:

STATE_CHANGE_TIMEOUT - length of time in ms between state transitions of new address space and addres objects

# Example Queries

## all address space types

```
query addressSpaceTypes {
  addressSpaceTypes_v2 {
    metadata {
      name
    }
    spec {
      displayName
      longDescription
      shortDescription
    }
  }
}


```

## all address types

```
query addressTypes {
  addressTypes_v2(addressSpaceType: standard) {
    metadata {
      name
    }
    spec {
      displayName
      longDescription
      shortDescription
    }
  }
}
```


## all_address_spaces

```
query all_address_spaces {
  addressSpaces {
    total
    addressSpaces {
      metadata {
        namespace
        name
        creationTimestamp
      }
      spec {
        type
        plan {
          spec {
            displayName
          }
        }
      }
      status {
        phase
        isReady
        messages
      }
    }
  }
}```

## all_addresses_for_addressspace_view

```
query all_addresses_for_addressspace_view {
  addresses(
    filter: "`$.spec.addressSpace` = 'jupiter_as1' AND `$.metadata.namespace` = 'app1_ns'"
  ) {
    total
    addresses {
      metadata {
        namespace
        name
      }
      spec {
        address
        plan {
          spec {
            displayName
          }
        }
      }
      status {
        isReady
        messages
        phase
        planStatus {
          partitions
        }
      }
      metrics {
        name
        type
        value
        units
      }
    }
  }
}
```

## addresses with ordering

Illustrates order on a metric (note the uses a JSON path filter to identify the metric of interest by name):

```
query addr {
 addresses(orderBy: "`$.metrics[?(@.Name=='enmasse_messages_stored')].Value` ASC"){
    total
    addresses
    {
      metadata {
        name
      }
      metrics {
        name
        value
      }
    }
  }
}
```

And a two-column order:

```
query addr {
 addresses(orderBy: "`$.status.phase` DESC, `$.metadata.name` ASC"){
    total
    addresses
    {
      metadata {
        name
      }
      status {
        phase
      }
    }
  }
}
```

## all_connections_for_addressspace_view

```
query all_connections_for_addressspace_view {
  connections(
    filter: "`$.spec.addressSpace` = 'jupiter_as1' AND `$.metadata.namespace` = 'app1_ns'"
  ) {
    total
    connections {
      metadata {
        name
      }
      spec {
        hostname
        containerId
        protocol
        encrypted
      }
    }
  }
}
```

## all_address_plans

```
query all_address_plans {
  addressPlans (
    addressSpacePlan:"standard-small"
  ) {
    spec {
      addressType
      displayName
      longDescription
      shortDescription
      displayOrder
    }
  }
}
```

## filtered_address_plans

address plans can be filtered by address space plan and/or address type.

```
query filtered_address_plans {
  addressPlans(addressSpacePlan: "standard-medium", addressType: queue) {
    metadata {
      name
    }
    spec {
      addressType
      displayName
      longDescription
      shortDescription
    }
  }
}
```

## all_authentication_services (from the address space schema)

```
query addressspace_schema {
  addressSpaceSchema_v2  {
    metadata {
      name
    }
    spec {
      authenticationServices
    }
  }
}
```

## filtered_authentication_services

schema authentication services can be filtered by address space type (from the address space schema)

```
query filtered_addressspace_schema($t:AddressSpaceType = standard){
  addressSpaceSchema_v2(addressSpaceType:$t)  {
    metadata {
      name
    }
    spec {
      authenticationServices
    }
  }
}
```

## all_authentication_services

all authentication services, including those that have no yet been validated

```
query authentication_services {
  authenticationServices {
    spec {
      type
    }
    metadata {
      name
    }
  }
}


```

## all_link_names_for_connection

```
query all_link_names_for_connection {
  connections(
    filter: "`$.metadata.name` LIKE 'juno:%' AND `$.spec.addressSpace` = 'jupiter_as1' AND `$.metadata.namespace` = 'app1_ns' "
  ) {
    connections {
      links {
        total
        links {
          metadata {
            name
          }
        }
      }
    }
  }
}
```



# single_address_with_links_and_metrics
```
query single_address_with_links_and_metrics {
  addresses(
    filter: "`$.metadata.name` = 'jupiter_as1.ganymede' AND `$.metadata.namespace` = 'app1_ns'"
  ) {
    total
    addresses {
      metadata {
        name
      }
      spec {
        addressSpace
      }
      links {
        total
        links {
          metadata {
            name
          }
          spec {
            role
            connection {
              spec {
                containerId
              }
            }
          }
          metrics {
            name
            type
            value
            units
          }
        }
      }
    }
  }
}
```

# Example Mutations

## Create address space

To create an address space, pass an input object describing the address space
to be created. The return value is the new address space's metadata.

```
mutation create_as($as: AddressSpace_enmasse_io_v1beta1_Input!) {
  createAddressSpace(input: $as) {
    name
    uid
    creationTimestamp
  }
}
```

args:

```
{
  "as": { "metadata": {"name": "wibx", "namespace": "app1_ns" },
    "spec": {"type": "standard", "plan": "standard-small"}}
}
```

## Patch address space

To patch an address space, pass the input object corresponding to the address space's
metadata and a JSON patch of the resource's spec describing the update
to be made.

The mock server currently implements RFC 6902 application/json-patch+json.

```
mutation patch_as(
  $a: ObjectMeta_v1_Input!
  $jsonPatch: String!
  $patchType: String!
) {
  patchAddressSpace(input: $a, jsonPatch: $jsonPatch, patchType: $patchType)
}
```

args:

(patching a plan)
```
{
  "a": {"name": "jupiter_as1", "namespace": "app1_ns" },
  "jsonPatch": "[{\"op\":\"replace\",\"path\":\"/spec/plan\",\"value\":\"standard-medium\"}]",
  "patchType": "application/json-patch+json"
}
```

(patching a authentication service name)
```
{
  "a": {"name": "jupiter_as1", "namespace" : "app1_ns" },
  "jsonPatch": "[{\"op\":\"replace\",\"path\":\"/spec/authenticationService/name\",\"value\":\"foo\"}]",
  "patchType": "application/json-patch+json"
}
```

## Delete address space

To delete an address space, call `deleteAddressSpace` passing the ObjectMeta
object associated with the address to delete.

```
mutation delete_as($a:ObjectMeta_v1_Input!) {
  deleteAddressSpace(input:$a)
}
```

args:

```
{
  "a": {"name": "jupiter_as1", "namespace": "app1_ns" }
}
```

## Create address

```
mutation create_addr($a:Address_enmasse_io_v1beta1_Input!) {
  createAddress(input: $a) {
      Name
      Namespace
      Uid
  }
}
```

args:

```
{
  "a": { "metadata": {"name": "jupiter_as1.wiby1", "namespace": "app1_ns" },
    "spec": {"type": "queue", "plan": "standard-small-queue", "address": "wiby1", "addressSpace": "jupiter_as1"}}
}
```

It is also possible to create an address without an ObjectMeta.name.  In this case the ObjectMeta.Name is defaulted
from the Spec.Address:

```
mutation create_addr($a:Address_enmasse_io_v1beta1_Input!, $as:String) {
  createAddress(input: $a, addressSpace: $as) {
      Name
      Namespace
      Uid
  }
}
```

args:

```
{
  "a": { "metadata": {"namespace": "app1_ns" },
    "spec": {"type": "queue", "plan": "standard-small-queue", "address": "foo2", "addressSpace": "jupiter_as1"}}
}
```

# Patch address

To patch an address, pass the input object corresponding to the address
metadata and a JSON patch of the resource's spec describing the update
to be made.

```
mutation patch_addr(
  $a: ObjectMeta_v1_Input!
  $jsonPatch: String!
  $patchType: String!
) {
  patchAddress(input: $a, jsonPatch: $jsonPatch, patchType: $patchType)
}
```

args:

```
{
  "a": {"name": "jupiter_as1.ganymede", "namespace": "app1_ns" },
  "jsonPatch": "[{\"op\":\"replace\",\"path\":\"/spec/plan\",\"value\":\"standard-medium-queue\"}]",
  "patchType": "application/json-patch+json"
}
```

# Delete address

To delete an address, call `deleteAddress` passing the ObjectMeta
object associated with the address to delete.

```
mutation delete_addr($a:ObjectMeta_v1_Input!) {
  deleteAddress(input:$a)
}
```

args:

```
{
  "a": {"name": "jupiter_as1.wiby1", "namespace": "app1_ns" }
}
```

# Purging address

To purge an address (i.e clear it of its messages), call `purgeAddress` passing the ObjectMeta
object associated with the address to purge.

```
mutation purge_addr($a:ObjectMeta_v1_Input!) {
  purgeAddress(input:$a)
}
```

args:

```
{
  "a": {"name": "jupiter_as1.wiby1", "namespace": "app1_ns" }
}
```

# Closing connection

To close a connection, call `closeConnection` passing the ObjectMeta
object associated with the connection to close.

```
mutation close_conn($c:ObjectMeta_v1_Input!) {
  closeConnection(input:$c)
}
```

args:

```
{
  "c": {"name": "cassini:55596", "namespace": "app1_ns" }
}
```

# Messaging Certificate For Address Space

To get the messaging certificate for an existing address space.

```
query messagingCertificateChain($as:ObjectMeta_v1_Input!) {
  messagingCertificateChain(input :$as)
}
```

```
{
  "as": {"name": "cassini:55596", "namespace": "app1_ns" }
}
```

# Address Space / Address Command

To get the equivalent command line that, if run, would cause the given address space or address to be created.

```
query cmd($as: AddressSpace_enmasse_io_v1beta1_Input!) {
  addressSpaceCommand(input:$as)
}
```

args:

```
{
  "as": { "metadata": {"name": "wibx", "namespace": "app1_ns" },
    "spec": {"type": "standard", "plan": "standard-small"}}
}
```

For addresses, it is also possible to create an address without an ObjectMeta.name.  In this case the ObjectMeta.Name
is defaulted from the Spec.Address:


```
query cmd($a: Address_enmasse_io_v1beta1_Input!, $as:String) {
  addressCommand(input:$a, addressSpace: $as)
}
```

args:

```
{
  "as": "jupiter_as",
  "a": { "metadata": {"namespace": "app1_ns" },
    "spec": {"type": "standard", "plan": "standard-small", "address":"foo"}}
}
```