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
  EDIT_ADDRESS_SPACE,
  DOWNLOAD_CERTIFICATE
} from "queries";
import { IAddressSpacesResponse } from "types/ResponseTypes";
import { EditAddressSpace } from "pages/EditAddressSpace";
import { ISortBy } from "@patternfly/react-table";
import { compareTwoAddress } from "pages/AddressSpaceDetail/AddressList/AddressListPage";
import { FetchPolicy, POLL_INTERVAL } from "constants/constants";
import { IObjectMeta_v1_Input } from "pages/AddressSpaceDetail/AddressSpaceDetailPage";
import { useMutationQuery } from "hooks";

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
  if (sortValue && sortBy !== sortValue) {
    setSortBy(sortValue);
  }

  const resetEditFormState = () => {
    setAddressSpaceBeingEdited(null);
    refetch();
  };

  /**
   * calling useMutationQuery hook for edit address space
   * passing cal back function resetFormState which will call on onError and onCompleted query respectively
   */
  const [setEditAddressSpaceQueryVariables] = useMutationQuery(
    EDIT_ADDRESS_SPACE,
    resetEditFormState,
    resetEditFormState
  );

  const resetDeleteFormState = (data: any) => {
    if (data && data.deleteAddressSpace === true) {
      setAddressSpaceBeingDeleted(null);
      refetch();
    }
  };
  const [setDeleteAddressSpaceQueryVariables] = useMutationQuery(
    DELETE_ADDRESS_SPACE,
    resetDeleteFormState,
    resetDeleteFormState
  );

  const handleCancelEdit = () => setAddressSpaceBeingEdited(null);
  const handleSaving = () => {
    if (addressSpaceBeingEdited) {
      const variables = {
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
      };
      setEditAddressSpaceQueryVariables(variables);
    }
  };

  const handleEditChange = (addressSpace: IAddressSpace) =>
    setAddressSpaceBeingEdited(addressSpace);

  const handleCancelDelete = () => setAddressSpaceBeingDeleted(null);
  const handleDelete = () => {
    if (addressSpaceBeingDeleted) {
      const variables = {
        a: {
          name: addressSpaceBeingDeleted.name,
          namespace: addressSpaceBeingDeleted.nameSpace
        }
      };
      setDeleteAddressSpaceQueryVariables(variables);
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
    addressSpaces: { total: 0, addressSpaces: [] }
  };
  setTotalAddressSpaces(addressSpaces.total);

  //Download the certificate function
  const downloadCertificate = async (data: IObjectMeta_v1_Input) => {
    const dataToDownload = await client.query({
      query: DOWNLOAD_CERTIFICATE,
      variables: {
        as: {
          name: data.name,
          namespace: data.namespace
        }
      },
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    if (dataToDownload.errors) {
      console.log("Error while download", dataToDownload.errors);
    }
    const url = window.URL.createObjectURL(
      new Blob([dataToDownload.data.messagingCertificateChain])
    );
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `${data.name}.crt`);
    document.body.appendChild(link);
    link.click();
    if (link.parentNode) link.parentNode.removeChild(link);
  };

  const addressSpacesList: IAddressSpace[] = addressSpaces.addressSpaces.map(
    addSpace => ({
      name: addSpace.metadata.name,
      nameSpace: addSpace.metadata.namespace,
      creationTimestamp: addSpace.metadata.creationTimestamp,
      type: addSpace.spec.type,
      planValue: addSpace.spec.plan.metadata.name,
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
            addSpace.metadata.name,
            nameSpace,
            addSpace.metadata.namespace
          )
        ).length === 1
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
          onDownload={downloadCertificate}
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
