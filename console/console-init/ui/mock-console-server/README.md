# mock-console-server

EnMasse mock console server that serves a static data-set comprising two namespaces, three address spaces, and many
addresses of different types.  Address spaces and address can be created, patched or deleted.
Addresses can be purged.  Connections can be closed.

TODO:
* How do we signal to the console, in a data driven way, if the user is able to purge an address?
This may be due to restrictions on the type of object itself (ie. cast address can't be purged), or
this user does not have permission.


# Running


`node mock-console-server.js`

Navigate to http://localhost:4000/ to use the GraphQL playground.  This lets you explore the schema and run queries
dynamically.

# Filters

Most queries accept a `filter` argument.  This allows filtering of the results.  The filter is specified by a
SQL-92 style `where` clause.  JSON-path operands are supported thus allowing filtering of any leaf node of the result
object.  A JSON-path operand in the expression are enclosed in backticks.

e.g.

```
 `$.Spec.AddressSpace` = 'jupiter_as1' AND `$.ObjectMeta.Namespace` = 'app1_ns'
```

# Sorting

Most queries accept a `orderBy` argument.  This allows the sorting of the results by one or more fields.
The sort clause is specified by a SQL-92 like `order by` clause with JSON-paths identifying the leaf node in the
result that is to be subjected to the sort.  Sort order can be `ASC` (ascending - default) or `DESC` (descending).

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

The queries that may return a large result accept pagination.  The pagination
arguments are `first` which specifies the number of rows to be returned and `offset`
the starting index within the result set.  The object return provides a count
of the number of rows in the result set in total.

# Example Queries


## all_address_spaces

```
query all_address_spaces {
  addressSpaces {
    Total
    AddressSpaces {
      ObjectMeta {
        Namespace
        Name
        CreationTimestamp
      }
      Spec {
        Type
        Plan {
          Spec {
            DisplayName
          }
        }
      }
      Status {
        IsReady
        Messages
      }
    }
  }
}
```

## all_addresses_for_addressspace_view

```
query all_addresses_for_addressspace_view {
  addresses(
    filter: "`$.Spec.AddressSpace` = 'jupiter_as1' AND `$.ObjectMeta.Namespace` = 'app1_ns'"
  ) {
    Total
    Addresses {
      ObjectMeta {
        Namespace
        Name
      }
      Spec {
        Address
        Plan {
          Spec {
            DisplayName
          }
        }
      }
      Status {
        IsReady
        Messages
        PlanStatus {
          Partitions
        }
      }
      Metrics {
        Name
        Type
        Value
        Units
      }
    }
  }
}

```

## all_connections_for_addressspace_view

```
query all_connections_for_addressspace_view {
  connections(
    filter: "`$.Spec.AddressSpace.ObjectMeta.Name` = 'jupiter_as1' AND `$.Spec.AddressSpace.ObjectMeta.Namespace` = 'app1_ns'"
  ) {
    Total
    Connections {
      ObjectMeta {
        Name
      }
      Spec {
        Hostname
        ContainerId
        Protocol
      }
    }
  }
}
```


# Example Mutations

## Create address space

To create an address space, pass an input object describing the address space
to be created.  The return value is the new address space's metadata.

```
mutation create_as($as: AddressSpace_enmasse_io_v1beta1_Input!) {
  createAddressSpace(input: $as) {
    Name
    Uid
    CreationTimestamp
  }
}
```

args:

```
{
  "as": { "ObjectMeta": {"Name": "wibx", "Namespace": "app1_ns" },
    "Spec": {"Type": "standard", "Plan": "standard-small"}}
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

```
{
  "a": {"Name": "wibx", "Namespace": "app1_ns" },
"jsonPatch": "[{\"op\":\"replace\",\"path\":\"/Plan\",\"value\":\"standard-medium\"}]",
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
  "a": {"Name": "wibx", "Namespace": "app1_ns" }
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
  "a": { "ObjectMeta": {"Name": "jupiter_as1.wiby1", "Namespace": "app1_ns" },
    "Spec": {"Type": "queue", "Plan": "standard-small-queue", "Address": "wiby1", "AddressSpace": "jupiter_as1"}}
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
  "a": {"Name": "jupiter_as1.wiby1", "Namespace": "app1_ns" },
"jsonPatch": "[{\"op\":\"replace\",\"path\":\"/Plan\",\"value\":\"standard-medium-queue\"}]",
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
  "a": {"Name": "jupiter_as1.wiby1", "Namespace": "app1_ns" }
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
  "a": {"Name": "jupiter_as1.wiby1", "Namespace": "app1_ns" }
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
  "c": {"Name": "cassini:55596", "Namespace": "app1_ns" }
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
  "as": {"Name": "cassini:55596", "Namespace": "app1_ns" }
}
```

# Address Space / Address Command

To get the equivalent command line that, if run, would cause the given address space to be created.
The addressCommand works in the same manner, but for addresses.

```
query cmd($as: AddressSpace_enmasse_io_v1beta1_Input!) {
  addressSpaceCommand(input:$as)
}
```

args:

```
{
  "as": { "ObjectMeta": {"Name": "wibx", "Namespace": "app1_ns" },
    "Spec": {"Type": "standard", "Plan": "standard-small"}}
}
```