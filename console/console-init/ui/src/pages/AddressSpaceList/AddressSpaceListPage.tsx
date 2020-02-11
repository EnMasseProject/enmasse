/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { useA11yRouteChange, useDocumentTitle, Loading } from "use-patternfly";
import { Button, Modal } from "@patternfly/react-core";
import {
  AddressSpaceList,
  IAddressSpace
} from "components/AddressSpaceList/AddressSpaceList";
import { EmptyAddressSpace } from "components/AddressSpaceList/EmptyAddressSpace";
import { DialoguePrompt } from "components/common/DialoguePrompt";
import {
  DELETE_ADDRESS_SPACE,
  RETURN_ALL_ADDRESS_SPACES,
  EDIT_ADDRESS_SPACE
} from "queries";
import { IAddressSpacesResponse } from "types/ResponseTypes";
import { EditAddressSpace } from "pages/EditAddressSpace";
import { ISortBy, IRowData } from "@patternfly/react-table";
import { compareTwoAddress } from "pages/AddressSpaceDetail/AddressList/AddressListPage";
import { FetchPolicy, POLL_INTERVAL } from "constants/constants";

interface AddressSpaceListPageProps {
  page: number;
  perPage: number;
  totalAddressSpaces: number;
  setTotalAddressSpaces: (value: number) => void;
  filter_Names: string[];
  filter_NameSpace: string[];
  filter_Type: string | null;
  onCreationRefetch?: boolean;
  setOnCreationRefetch: (value: boolean) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  isCreateWizardOpen: boolean;
  setIsCreateWizardOpen: (value: boolean) => void;
  onSelectAddressSpace: (data: IAddressSpace, isSelected: boolean) => void;
  onSelectAllAddressSpace: (
    dataList: IAddressSpace[],
    isSelected: boolean
  ) => void;
  selectedAddressSpaces: Array<IAddressSpace>;
}
export const AddressSpaceListPage: React.FunctionComponent<AddressSpaceListPageProps> = ({
  page,
  perPage,
  totalAddressSpaces,
  setTotalAddressSpaces,
  filter_Names,
  filter_NameSpace,
  filter_Type,
  onCreationRefetch,
  setOnCreationRefetch,
  sortValue,
  setSortValue,
  isCreateWizardOpen,
  setIsCreateWizardOpen,
  onSelectAddressSpace,
  onSelectAllAddressSpace,
  selectedAddressSpaces
}) => {
  useDocumentTitle("Addressspace List");
  useA11yRouteChange();
  const client = useApolloClient();
  const [
    addressSpaceBeingEdited,
    setAddressSpaceBeingEdited
  ] = React.useState<IAddressSpace | null>();

  const [
    addressSpaceBeingDeleted,
    setAddressSpaceBeingDeleted
  ] = React.useState<IAddressSpace | null>();

  const [sortBy, setSortBy] = React.useState<ISortBy>();
  if (sortValue && sortBy != sortValue) {
    setSortBy(sortValue);
  }
  const handleCancelEdit = () => setAddressSpaceBeingEdited(null);
  const handleSaving = async () => {
    addressSpaceBeingEdited &&
      (await client.mutate({
        mutation: EDIT_ADDRESS_SPACE,
        variables: {
          a: {
            name: addressSpaceBeingEdited.name,
            namespace: addressSpaceBeingEdited.nameSpace
          },
          jsonPatch:
            '[{"op":"replace","path":"/spec/plan","value":"' +
            addressSpaceBeingEdited.planValue +
            '"},' +
            '{"op":"replace","path":"/spec/authenticationService/name","value":"' +
            addressSpaceBeingEdited.authenticationService +
            '"}' +
            "]",
          patchType: "application/json-patch+json"
        }
      }));
    setAddressSpaceBeingEdited(null);
    refetch();
  };

  const handleEditChange = (addressSpace: IAddressSpace) =>
    setAddressSpaceBeingEdited(addressSpace);

  const handleCancelDelete = () => setAddressSpaceBeingDeleted(null);
  const handleDelete = async () => {
    if (addressSpaceBeingDeleted) {
      const deletedData = await client.mutate({
        mutation: DELETE_ADDRESS_SPACE,
        variables: {
          a: {
            Name: addressSpaceBeingDeleted.name,
            Namespace: addressSpaceBeingDeleted.nameSpace
          }
        }
      });
      if (
        deletedData &&
        deletedData.data &&
        deletedData.data.deleteAddressSpace === true
      ) {
        setAddressSpaceBeingDeleted(null);
        refetch();
      }
    }
  };
  const handleDeleteChange = (addressSpace: IAddressSpace) =>
    setAddressSpaceBeingDeleted(addressSpace);

  const handlePlanChange = (plan: string) => {
    if (addressSpaceBeingEdited) {
      addressSpaceBeingEdited.planValue = plan;
      setAddressSpaceBeingEdited({ ...addressSpaceBeingEdited });
    }
  };

  const handleAuthServiceChanged = (authService: string) => {
    if (addressSpaceBeingEdited) {
      addressSpaceBeingEdited.authenticationService = authService;
      setAddressSpaceBeingEdited({ ...addressSpaceBeingEdited });
    }
  };

  const { loading, error, data, refetch } = useQuery<IAddressSpacesResponse>(
    RETURN_ALL_ADDRESS_SPACES(
      page,
      perPage,
      filter_Names,
      filter_NameSpace,
      filter_Type,
      sortBy
    ),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  if (loading) {
    return <Loading />;
  }
  if (error) {
    console.log(error);
  }

  if (onCreationRefetch) {
    refetch();
    setOnCreationRefetch(false);
  }

  const { addressSpaces } = data || {
    addressSpaces: { Total: 0, AddressSpaces: [] }
  };
  setTotalAddressSpaces(addressSpaces.Total);
  console.log(addressSpaces);
  const addressSpacesList: IAddressSpace[] = addressSpaces.AddressSpaces.map(
    addSpace => ({
      name: addSpace.objectMeta.name,
      nameSpace: addSpace.objectMeta.namespace,
      creationTimestamp: addSpace.objectMeta.creationTimestamp,
      type: addSpace.spec.type,
      planValue: addSpace.spec.plan.objectMeta.name,
      displayName: addSpace.spec.plan.spec.displayName,
      isReady: addSpace.status && addSpace.status.isReady,
      phase:
        addSpace.status && addSpace.status.phase ? addSpace.status.phase : "",
      messages:
        addSpace.status && addSpace.status.messages
          ? addSpace.status.messages
          : [],
      authenticationService:
        addSpace.spec &&
        addSpace.spec.authenticationService &&
        addSpace.spec.authenticationService.name,
      selected:
        selectedAddressSpaces.filter(({ name, nameSpace }) =>
          compareTwoAddress(
            name,
            addSpace.objectMeta.name,
            nameSpace,
            addSpace.objectMeta.namespace
          )
        ).length == 1
    })
  );
  const onSort = (_event: any, index: any, direction: any) => {
    setSortBy({ index: index, direction: direction });
    setSortValue({ index: index, direction: direction });
  };
  return (
    <>
      {totalAddressSpaces > 0 ? (
        <AddressSpaceList
          rows={addressSpacesList}
          onSelectAddressSpace={onSelectAddressSpace}
          onSelectAllAddressSpace={onSelectAllAddressSpace}
          onEdit={handleEditChange}
          onDelete={handleDeleteChange}
          onSort={onSort}
          sortBy={sortBy}
        />
      ) : (
        <EmptyAddressSpace
          isWizardOpen={isCreateWizardOpen}
          setIsWizardOpen={setIsCreateWizardOpen}
        />
      )}
      {addressSpaceBeingEdited && (
        <Modal
          isLarge
          id="as-list-edit-modal"
          title="Edit"
          isOpen={true}
          onClose={handleCancelEdit}
          actions={[
            <Button
              key="confirm"
              id="as-list-edit-confirm"
              variant="primary"
              onClick={handleSaving}
            >
              Confirm
            </Button>,
            <Button
              key="cancel"
              id="as-list-edit-cancel"
              variant="link"
              onClick={handleCancelEdit}
            >
              Cancel
            </Button>
          ]}
          isFooterLeftAligned={true}
        >
          <EditAddressSpace
            addressSpace={addressSpaceBeingEdited}
            onAuthServiceChanged={handleAuthServiceChanged}
            onPlanChange={handlePlanChange}
          ></EditAddressSpace>
        </Modal>
      )}
      {addressSpaceBeingDeleted && (
        <DialoguePrompt
          option="Delete"
          detail={`Are you sure you want to delete this addressspace: ${addressSpaceBeingDeleted.name} ?`}
          names={[addressSpaceBeingDeleted.name]}
          header="Delete this Address Space ?"
          handleCancelDialogue={handleCancelDelete}
          handleConfirmDialogue={handleDelete}
        />
      )}
    </>
  );
};
