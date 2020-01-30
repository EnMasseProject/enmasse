# GraphQL Prototype

Prototype lets you query addresses and address plans, and create/update/delete addresses using a GraphQL style interface.

To run the prototype there are two options:

- Prototype is part the Console deployment.
- Run standalone

## Part of EnMasse

point your browser at the Global Console, then navigate manually to

https://<consoleurl>/graphql

## Stand-alone

Authenticate to OpenShift on the command line `oc login`:

`go run cmd/console-graphql/main.go`

Then point the browser at http://localhost:8080/graphql

In both cases you will see a GraphQL playground.  You can explore the schema interactively.  There are also some
example queries below.  

Address and AddressPlans are wired to the EnMasse model, so create an addressspace and some addresses in the infra
namespace first. Links and Metrics are populated with random data.  Addresses and Links results are paginated.

# GraphQL prototype interface

Neither - Sorting and Filter is not implemented.  JSONPath will be used for both of these.

- sorting - list of jsonpath/direction pairs e.g. [<path>, 'up', <path>, 'down'] to specify a sort on two fields.
- filtering - a string describing a boolean expression "<path> = 'Sender'"


# Generating the code

go run scripts/gqlgen.go -c console/console-graphql/src/main/resources/gqlgen.yml


## query all address

```
query allAddress {
  queryAddress {
    Addresess {
      resource {
        objectMeta {
          name
        }
      }
    }
  }
}
```

## query some address (paginated addresses) retrieving links too 

```
query paginatedAddressQuery {
  queryAddress(first: 7) {
    Total
    Addresess {
      queryLinks {
        Total
        Links {
          name
          role
          connection {
            containerId
            protocol
          }
          metrics {
            name
            type
            value
            units
          }
        }
      }
      metrics {
        name
        type
        value
        units
      }

      resource {
        objectMeta {
          name
          resourceVersion
          creationTimestamp
          annotations {
            key
            value
          }
        }
        spec {
          address
          type
        }
        status {
          isReady
          messages
        }
      }
    }
  }
}
```

## addressplans

```
query myplans {
  addressPlans{
    spec
    {
      displayName
      longDescription
      shortDescription
    }
    objectMeta {
      name
    }
  }
}
```

# mutations

## create address

```
mutation createAddress($a:AddressK8sInput!) {
  createAddress(input:$a) {
    objectMeta {
      name
      resourceVersion
    }
  }
}{
  "a": { "objectMeta": {"name": "standard.kwfoo", "namespace": "enmasse-infra" },
    "spec": {"address": "kwfoo", "type": "queue", "plan": "standard-small-queue"}}
}
```

## patch address

```
mutation patchAddress($a:ObjectMetaK8sInput!, $patch: String!, $patchType: String!) {
  patchAddress(input:$a, jsonPatch:$patch, patchType: $patchType) {
    objectMeta {
      name
      resourceVersion
      annotations {
        key
        value
      }
    }
  }
}
{
  "a": { "name": "standard.kwfoo", "namespace": "enmasse-infra" },
  "patch": "{\"metadata\":{\"annotations\":{\"fish2\":\"dorado32\"}}}",
  "patchType": "application/merge-patch+json"
}
```

## delete address

```
mutation deleteAddress($a:ObjectMetaK8sInput!) {
  deleteAddress(input:$a)
}
{
  "a": { "name": "standard.kwfoo", "namespace": "enmasse-infra" }
}
```

