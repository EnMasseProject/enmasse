import * as React from "react";
import { Link, useLocation, useHistory } from "react-router-dom";
import { PageSection, PageSectionVariants } from "@patternfly/react-core";
import gql from "graphql-tag";
import { useQuery } from "@apollo/react-hooks";
import { useDocumentTitle, useA11yRouteChange, Loading } from "use-patternfly";
import { IAddress, AddressList } from "src/Components/AddressSpace/AddressList";
import { EmptyAddress } from "src/Components/Common/EmptyAddress";

const ALL_ADDRESS_FOR_ADDRESS_SPACE = gql`
  query all_addresses_for_addressspace_view {
    addresses(
      filter: "\`$.Spec.AddressSpace\` = 'jupiter_as1' AND \`$.Metadata.Namespace\` = 'app1_ns'"
    ) {
      Total
      Addresses {
        Metadata {
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
`;

interface IAddressResponse {
  addresses: {
    Total: number;
    Addresses: Array<{
      Metadata: {
        Namespace: string;
        Name: string;
      };
      Spec: {
        Address: string;
        Plan: {
          Spec: {
            DisplayName: string;
          };
        };
      };
      Status: {
        IsReady: boolean;
        Messages: Array<string>;
      };
      Metrics: Array<{
        Name: string;
        Type: string;
        Value: number;
        Units: string;
      }>;
    }>;
  };
}

function AddressesListFunction() {
  useDocumentTitle("Addressses List");
  useA11yRouteChange();
  const location = useLocation();
  const history = useHistory();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 0;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;

  const { loading, data } = useQuery<IAddressResponse>(
    ALL_ADDRESS_FOR_ADDRESS_SPACE,
    { pollInterval: 5000 }
  );

  const setSearchParam = React.useCallback(
    (name: string, value: string) => {
      searchParams.set(name, value.toString());
    },
    [searchParams]
  );

  const handlePageChange = React.useCallback(
    (newPage: number) => {
      setSearchParam("page", (newPage - 1).toString());
      history.push({
        search: searchParams.toString()
      });
    },
    [setSearchParam, history, searchParams]
  );

  const handlePerPageChange = React.useCallback(
    (newPerPage: number) => {
      setSearchParam("page", "0");
      setSearchParam("perPage", newPerPage.toString());
      history.push({
        search: searchParams.toString()
      });
    },
    [setSearchParam, history, searchParams]
  );
  if (loading) return <Loading />
//   console.log(data);
  const { addresses } = data || {
    addresses: { Total: 0, Addresses: [] }
  };
//   addresses.Total=0;
  const addressesList: IAddress[] = addresses.Addresses.map(address => ({
    name: address.Metadata.Name,
    type: address.Metadata.Namespace,
    plan: address.Spec.Plan.Spec.DisplayName,
    messagesIn: address.Metrics.filter(metric => {
      return "enmasse_messages_in";
    })[0].Value,
    messagesOut: address.Metrics.filter(metric => {
      return "enmasse_messages_out";
    })[0].Value,
    storedMessages: address.Metrics.filter(metric => {
      return "enmasse_messages_stored";
    })[0].Value,
    senders: address.Metrics.filter(metric => {
      return "enmasse-senders";
    })[0].Value,
    receivers: address.Metrics.filter(metric => {
      return "enmasse-receivers";
    })[0].Value,
    shards: 1,
    status: address.Status.IsReady ? "running" : "creating"
  }));
  console.log(addresses);
  if (addresses.Total == 0)
    return (
        <EmptyAddress/>
    );
  else  
  return (
  <AddressList
    rows={addressesList}
    onEdit={(data) => {
      console.log("on Edit",data);
    }}
    onDelete={() => {
      console.log("on Delete");
    }}
  />)
}

export default function AddressesListPage() {
  return <AddressesListFunction />;
}
