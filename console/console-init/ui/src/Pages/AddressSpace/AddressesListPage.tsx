import * as React from "react";
import {
  // Link,
  // useLocation,
  // useHistory,
  useParams
} from "react-router-dom";

import {
  PageSection,
  PageSectionVariants,
  Pagination,
  GridItem,
  Grid,
  Modal,
  Button,
  InputGroup,
  Dropdown,
  DropdownPosition,
  KebabToggle,
  FlexItem,
  Flex
  // Breadcrumb,
  // BreadcrumbItem
} from "@patternfly/react-core";

import gql from "graphql-tag";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import {
  useDocumentTitle,
  useA11yRouteChange,
  Loading
  // useBreadcrumb
} from "use-patternfly";
import { IAddress, AddressList } from "src/Components/AddressSpace/AddressList";
import { EmptyAddress } from "src/Components/Common/EmptyAddress";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { IAddressResponse } from "src/Types/ResponseTypes";
import { EditAddress } from "../EditAddressPage";
import { AddressListFilter } from "src/Components/AddressSpace/AddressListFilter";
import { css, StyleSheet } from "@patternfly/react-styles";
import { CreateAddress } from "src/Pages/CreateAddress/CreateAddressPage";
import { DeletePrompt } from "src/Components/Common/DeletePrompt";
import { DELETE_ADDRESS } from "src/Queries/Quries";

export const GridStylesForTableHeader = StyleSheet.create({
  grid_bottom_border: {
    paddingBottom: "1em",
    borderBottom: "0.05em solid",
    borderBottomColor: "lightgrey"
  },
  filter_left_margin: {
    marginLeft: 24
  },
  create_button_left_margin: {
    marginLeft: 10
  }
});

const RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE = (
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
          Phase
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

function AddressesListFunction() {
  const { name, namespace, type } = useParams();
  useDocumentTitle("Address List");
  useA11yRouteChange();
  const [
    addressBeingEdited,
    setAddressBeingEdited
  ] = React.useState<IAddress | null>();

  const [
    addressBeingDeleted,
    setAddressBeingDeleted
  ] = React.useState<IAddress | null>();

  const [filter, setFilter] = React.useState("Name");
  const [addressType, setAddressType] = React.useState("Queue");
  const [status, setStatus] = React.useState("Active");
  const client = useApolloClient();
  // const location = useLocation();
  // const history = useHistory();
  // const searchParams = new URLSearchParams(location.search);
  // const page = parseInt(searchParams.get("page") || "", 10) || 0;
  // const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;

  const { loading, error, data, refetch } = useQuery<IAddressResponse>(
    RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE(name, namespace),
    { pollInterval: 20000 }
  );
  // console.log(data);
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
  if (error) return <Loading />;
  const { addresses } = data || {
    addresses: { Total: 0, Addresses: [] }
  };
  // addresses.Total = 0;
  // addresses.Addresses = [];
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
    isReady: address.Status.IsReady,
    status: address.Status.Phase,
    errorMessages: address.Status.Messages
  }));
  const handleEdit = (data: IAddress) => {
    if (!addressBeingEdited) {
      setAddressBeingEdited(data);
    }
  };
  const handleCancelEdit = () => setAddressBeingEdited(null);

  const handleSaving = () => void 0;
  const handleEditChange = (address: IAddress) =>
    setAddressBeingEdited(address);

  const handleCancelDelete = () => setAddressBeingDeleted(null);
  const handleDelete = async () => {
    if (addressBeingDeleted) {
      const deletedData = await client.mutate({
        mutation: DELETE_ADDRESS,
        variables: {
          a: {
            Name: addressBeingDeleted.name,
            Namespace: addressBeingDeleted.namespace
          }
        }
      });
      console.log(deletedData);
      if (
        deletedData &&
        deletedData.data &&
        deletedData.data.deleteAddress === true
      ) {
        setAddressBeingDeleted(null);
        refetch();
      }
    }
  };
  const handleDeleteChange = (address: IAddress) =>
    setAddressBeingDeleted(address);

  return (
    <PageSection variant={PageSectionVariants.light}>
      <Grid className={css(GridStylesForTableHeader.grid_bottom_border)}>
        <GridItem
          span={6}
          className={css(GridStylesForTableHeader.filter_left_margin)}
        >
          <InputGroup>
            <AddressListFilter
              onSearch={() => {
                console.log("on Search");
              }}
              setFilterValue={setFilter}
              filterValue={filter}
              setTypeValue={setAddressType}
              typeValue={addressType}
              setStatusValue={setStatus}
              statusValue={status}
            />
            <CreateAddress
              namespace={namespace || ""}
              addressSpace={name || ""}
              type={type || ""}
              refetch={refetch}
            />
            <Dropdown
              isPlain
              position={DropdownPosition.right}
              isOpen={false}
              onSelect={() => {}}
              toggle={<KebabToggle onToggle={() => {}} />}
              dropdownItems={[]}
            />
          </InputGroup>
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
      <AddressList
        rowsData={addressesList ? addressesList : []}
        onEdit={handleEdit}
        onDelete={handleDeleteChange}
      />
      {addresses.Total === 0 ? <EmptyAddress /> : ""}
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
      {addressBeingDeleted && (
        <DeletePrompt
          detail={`Are you sure you want to delete ${addressBeingDeleted.name} ?`}
          name={addressBeingDeleted.name}
          header="Delete this Address  ?"
          handleCancelDelete={handleCancelDelete}
          handleConfirmDelete={handleDelete}
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
    </PageSection>
  );
}

export default function AddressesListPage() {
  return <AddressesListFunction />;
}
