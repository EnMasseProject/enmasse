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
      Resource {
        ObjectMeta {
          Name
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
          Name
          Role
          Connection {
            containerId
            protocol
          }
          Metrics {
            Name
            Type
            Value
            Units
          }
        }
      }
      Metrics {
        Name
        Type
        Value
        Units
      }

      Resource {
        ObjectMeta {
          Name
          ResourceVersion
          CreationTimestamp
          Annotations {
            Key
            Value
          }
        }
        Spec {
          Address
          Type
        }
        Status {
          IsReady
          Messages
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
    Spec
    {
      DisplayName
      LongDescription
      ShortDescription
    }
    ObjectMeta {
      Name
    }
  }
}
```

# mutations

## create address

```
mutation createAddress($a:AddressK8sInput!) {
  createAddress(input:$a) {
    ObjectMeta {
      Name
      ResourceVersion
    }
  }
}{
  "a": { "ObjectMeta": {"Name": "standard.kwfoo", "Namespace": "enmasse-infra" },
    "Spec": {"Address": "kwfoo", "Type": "queue", "Plan": "standard-small-queue"}}
}
```

## patch address

```
mutation patchAddress($a:ObjectMetaK8sInput!, $patch: String!, $patchType: String!) {
  patchAddress(input:$a, jsonPatch:$patch, patchType: $patchType) {
    ObjectMeta {
      Name
      ResourceVersion
      Annotations {
        Key
        Value
      }
    }
  }
}
{
  "a": { "Name": "standard.kwfoo", "Namespace": "enmasse-infra" },
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
  "a": { "Name": "standard.kwfoo", "Namespace": "enmasse-infra" }
}
```

