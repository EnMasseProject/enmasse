import * as React from "react";
import { Link, useLocation, useHistory, useParams } from "react-router-dom";
import {
  PageSection,
  PageSectionVariants,
  Pagination,
  GridItem,
  Grid,
  Modal,
  Button
} from "@patternfly/react-core";
import gql from "graphql-tag";
import { useQuery } from "@apollo/react-hooks";
import { useDocumentTitle, useA11yRouteChange, Loading } from "use-patternfly";
import { IAddress, AddressList } from "src/Components/AddressSpace/AddressList";
import { EmptyAddress } from "src/Components/Common/EmptyAddress";
import { AddressListFilterWithPagination } from "src/Components/AddressSpace/AddressListFilterWithPaginationHeader";
import { StyleSheet } from "@patternfly/react-styles";
import { EditAddress } from "./EditAddressPage";

const styles = StyleSheet.create({
  header_bottom_border: {
    borderBottom: "1px solid black"
  }
});
const return_ALL_ADDRESS_FOR_ADDRESS_SPACE = (
  name?: string,
  namespace?: string
) => {
  const ALL_ADDRESS_FOR_ADDRESS_SPACE = gql`
  query all_addresses_for_addressspace_view {
    addresses(
      filter: "\`$.Spec.AddressSpace\` = '${name}' AND \`$.ObjectMeta.Namespace\` = '${namespace}'"
    ) {
      Total
      Addresses {
        ObjectMeta {
          Namespace
          Name
        }
        Spec {
          Address
          Type
          Plan {
            Spec {
              DisplayName
            }
          }
        }
        Status {
          PlanStatus{
            Partitions
          }
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
  return ALL_ADDRESS_FOR_ADDRESS_SPACE;
};

interface IAddressResponse {
  addresses: {
    Total: number;
    Addresses: Array<{
      ObjectMeta: {
        Namespace: string;
        Name: string;
      };
      Spec: {
        Address: string;
        Type: string;
        Plan: {
          Spec: {
            DisplayName: string;
          };
        };
      };
      Status: {
        PlanStatus: {
          Partitions: number;
        };
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

export interface IMetrics {
  Name: string;
  Type: string;
  Value: number;
  Units: string;
}

function AddressesListFunction() {
  const { name, namespace } = useParams();
  useDocumentTitle("Address List");
  useA11yRouteChange();
  const [
    addressBeingEdited,
    setAddressBeingEdited
  ] = React.useState<IAddress | null>();
  // const location = useLocation();
  // const history = useHistory();
  // const searchParams = new URLSearchParams(location.search);
  // const page = parseInt(searchParams.get("page") || "", 10) || 0;
  // const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;

  const { loading, data } = useQuery<IAddressResponse>(
    return_ALL_ADDRESS_FOR_ADDRESS_SPACE(name, namespace),
    { pollInterval: 5000 }
  );

  // const setSearchParam = React.useCallback(
  //   (name: string, value: string) => {
  //     searchParams.set(name, value.toString());
  //   },
  //   [searchParams]
  // );

  // const handlePageChange = React.useCallback(
  //   (newPage: number) => {
  //     setSearchParam("page", (newPage - 1).toString());
  //     history.push({
  //       search: searchParams.toString()
  //     });
  //   },
  //   [setSearchParam, history, searchParams]
  // );

  // const handlePerPageChange = React.useCallback(
  //   (newPerPage: number) => {
  //     setSearchParam("page", "0");
  //     setSearchParam("perPage", newPerPage.toString());
  //     history.push({
  //       search: searchParams.toString()
  //     });
  //   },
  //   [setSearchParam, history, searchParams]
  // );
  if (loading) return <Loading />;

  const { addresses } = data || {
    addresses: { Total: 0, Addresses: [] }
  };
  const getFilteredValue = (object: IMetrics[], value: string) => {
    const filtered = object.filter(obj => obj.Name === value);
    if (filtered.length > 0) {
      return filtered[0].Value;
    }
    return 0;
  };

  //   addresses.Total=0;
  const addressesList: IAddress[] = addresses.Addresses.map(address => ({
    name: address.ObjectMeta.Name,
    namespace: address.ObjectMeta.Namespace,
    type: address.Spec.Type,
    plan: address.Spec.Plan.Spec.DisplayName,
    messagesIn: getFilteredValue(address.Metrics, "enmasse_messages_in"),
    messagesOut: getFilteredValue(address.Metrics, "enmasse_messages_out"),
    storedMessages: getFilteredValue(
      address.Metrics,
      "enmasse_messages_stored"
    ),
    senders: getFilteredValue(address.Metrics, "enmasse-senders"),
    receivers: getFilteredValue(address.Metrics, "enmasse-receivers"),
    shards: address.Status.PlanStatus.Partitions,
    status: address.Status.IsReady ? "running" : "creating"
  }));

  const handleDelete = (data: IAddress) => void 0;
  const handleEdit = (data: IAddress) => {
    if (!addressBeingEdited) {
      setAddressBeingEdited(data);
    }
  };
  const handleCancelEdit = () => setAddressBeingEdited(null);
  const handleSaving = () => void 0;
  const handleEditChange = (address: IAddress) =>
    setAddressBeingEdited(address);

  return (
    <PageSection variant={PageSectionVariants.light}>
      <Grid>
        <GridItem span={6}>
          <AddressListFilterWithPagination />
        </GridItem>
        <GridItem span={6}>
          {addresses.Total === 0 ? (
            ""
          ) : (
            <Pagination
              itemCount={523}
              perPage={10}
              page={1}
              onSetPage={() => {}}
              widgetId="pagination-options-menu-top"
              onPerPageSelect={() => {}}
            />
          )}
        </GridItem>
      </Grid>
      {addresses.Total === 0 ? (
        <EmptyAddress />
      ) : (
        <AddressList
          rows={addressesList}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
      )}

      {addresses.Total === 0 ? (
        ""
      ) : (
        <Pagination
          itemCount={523}
          perPage={10}
          page={1}
          onSetPage={() => {}}
          widgetId="pagination-options-menu-top"
          onPerPageSelect={() => {}}
        />
      )}
      {addressBeingEdited && (
        <Modal
          title="Edit"
          isSmall
          isOpen={true}
          onClose={handleCancelEdit}
          actions={[
            <Button key="confirm" variant="primary" onClick={handleSaving}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={handleCancelEdit}>
              Cancel
            </Button>
          ]}
          isFooterLeftAligned
        >
          <EditAddress
            address={addressBeingEdited}
            onChange={handleEditChange}
          />
        </Modal>
      )}
    </PageSection>
  );
}

export default function AddressesListPage() {
  return <AddressesListFunction />;
}
