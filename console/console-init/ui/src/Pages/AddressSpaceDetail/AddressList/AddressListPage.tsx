import * as React from "react";
import { useApolloClient, useQuery } from "@apollo/react-hooks";
import { IAddressResponse } from "src/Types/ResponseTypes";
import {
  RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE,
  DELETE_ADDRESS,
  EDIT_ADDRESS
} from "src/Queries/Queries";
import { IAddress, AddressList } from "src/Components/AddressSpace/AddressList";
import { Loading } from "use-patternfly";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { Modal, Button } from "@patternfly/react-core";
import { EmptyAddress } from "src/Components/Common/EmptyAddress";
import { EditAddress } from "../../EditAddressPage";
import { DeletePrompt } from "src/Components/Common/DeletePrompt";
import { ISortBy } from "@patternfly/react-table";
export interface IAddressListPageProps {
  name?: string;
  namespace?: string;
  addressSpaceType?: string;
  filterNames?: string[];
  typeValue?: string | null;
  statusValue?: string | null;
  page: number;
  perPage: number;
  setTotalAddress: (total: number) => void;
  addressSpacePlan: string | null;
}
export const AddressListPage: React.FunctionComponent<IAddressListPageProps> = ({
  name,
  namespace,
  addressSpaceType,
  filterNames,
  typeValue,
  statusValue,
  setTotalAddress,
  page,
  perPage,
  addressSpacePlan
}) => {
  const [
    addressBeingEdited,
    setAddressBeingEdited
  ] = React.useState<IAddress | null>();

  const [
    addressBeingDeleted,
    setAddressBeingDeleted
  ] = React.useState<IAddress | null>();

  const client = useApolloClient();
  const [sortBy, setSortBy] = React.useState<ISortBy>();

  const { loading, error, data, refetch } = useQuery<IAddressResponse>(
    RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE(
      page,
      perPage,
      name,
      namespace,
      filterNames,
      typeValue,
      statusValue,
      sortBy
    ),
    { pollInterval: 20000, fetchPolicy: "network-only" }
  );
  if (loading) return <Loading />;
  if (error) return <Loading />;
  const { addresses } = data || {
    addresses: { Total: 0, Addresses: [] }
  };
  setTotalAddress(addresses.Total);
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

  const handleSaving = async () => {
    if (addressBeingEdited && addressSpaceType) {
      await client.mutate({
        mutation: EDIT_ADDRESS,
        variables: {
          a: {
            Name: addressBeingEdited.name,
            Namespace: addressBeingEdited.namespace
          },
          jsonPatch:
            '[{"op":"replace","path":"/Plan","value":"' +
            addressBeingEdited.plan +
            '"}]',
          patchType: "application/json-patch+json"
        }
      });
      refetch();
      setAddressBeingEdited(null);
    }
  };
  const handleEditChange = (address: IAddress) =>
    setAddressBeingEdited(address);

  const handlePlanChange = (plan: string) => {
    if (addressBeingEdited) {
      addressBeingEdited.plan = plan;
      setAddressBeingEdited({ ...addressBeingEdited });
    }
  };
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
      refetch();
      setAddressBeingDeleted(null);
    }
  };
  const handleDeleteChange = (address: IAddress) => {
    setAddressBeingDeleted(address);
  };
  const onSort = (_event: any, index: any, direction: any) => {
    setSortBy({ index: index, direction: direction });
  };

  return (
    <>
      {console.log("totalAddressSpaces", addressesList)}
      <AddressList
        rowsData={addressesList ? addressesList : []}
        onEdit={handleEdit}
        onDelete={handleDeleteChange}
        sortBy={sortBy}
        onSort={onSort}
      />
      {addresses.Total === 0 ? <EmptyAddress /> : ""}
      {addressBeingEdited && (
        <Modal
          title="Edit"
          isSmall
          isOpen={true}
          onClose={handleCancelEdit}
          actions={[
            <Button
              key="confirm"
              id="al-edit-confirm"
              variant="primary"
              onClick={handleSaving}>
              Confirm
            </Button>,
            <Button
              key="cancel"
              id="al-edit-cancel"
              variant="link"
              onClick={handleCancelEdit}>
              Cancel
            </Button>
          ]}
          isFooterLeftAligned>
          <EditAddress
            name={addressBeingEdited.name}
            type={addressBeingEdited.type}
            plan={addressBeingEdited.plan}
            addressSpacePlan={addressSpacePlan}
            onChange={handlePlanChange}
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
    </>
  );
};
