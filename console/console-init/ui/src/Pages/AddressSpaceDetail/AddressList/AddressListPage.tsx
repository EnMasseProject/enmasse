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
import { getPlanAndTypeForAddressEdit } from "src/Components/Common/AddressFormatter";
export interface IAddressListPageProps {
  name?: string;
  namespace?: string;
  addressSpaceType?: string;
  inputValue?: string | null;
  filterValue?: string | null;
  typeValue?: string | null;
  statusValue?: string | null;
  page: number;
  perPage: number;
  setTotalAddress: (total: number) => void;
}
export const AddressListPage: React.FunctionComponent<IAddressListPageProps> = ({
  name,
  namespace,
  addressSpaceType,
  inputValue,
  filterValue,
  typeValue,
  statusValue,
  setTotalAddress,
  page,
  perPage
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

  const { loading, error, data, refetch } = useQuery<IAddressResponse>(
    RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE(
      page,
      perPage,
      name,
      namespace,
      filterValue,
      inputValue,
      typeValue,
      statusValue
    ),
    { pollInterval: 20000, fetchPolicy: "network-only" }
  );

  if (loading) return <Loading />;
  if (error) return <Loading />;
  console.log(data);
  const { addresses } = data || {
    addresses: { Total: 0, Addresses: [] }
  };
  setTotalAddress(addresses.Total);
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
            getPlanAndTypeForAddressEdit(
              addressBeingEdited.plan,
              addressSpaceType
            ) +
            '"}]',
          // "jsonPatch": "[{\"op\":\"replace\",\"path\":\"/Plan\",\"value\":\"standard-medium-queue\"}]",
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
    <>
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
            <Button key="confirm" id="al-edit-confirm" variant="primary" onClick={handleSaving}>
              Confirm
            </Button>,
            <Button key="cancel" id="al-edit-cancel" variant="link" onClick={handleCancelEdit}>
              Cancel
            </Button>
          ]}
          isFooterLeftAligned>
          <EditAddress
            name={addressBeingEdited.name}
            type={addressBeingEdited.type}
            plan={addressBeingEdited.plan}
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
