/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  BreadcrumbItem,
  Breadcrumb,
  Modal,
  Button
} from "@patternfly/react-core";
import { useBreadcrumb, useA11yRouteChange, Loading } from "use-patternfly";
import { Link, useHistory } from "react-router-dom";
import { useParams } from "react-router";
import { AddressDetailHeader } from "components/AddressDetail/AddressDetailHeader";
import { useQuery } from "@apollo/react-hooks";
import { IAddressDetailResponse } from "types/ResponseTypes";
import { getFilteredValue } from "components/common/ConnectionListFormatter";
import { DialoguePrompt } from "components/common/DialoguePrompt";
import {
  DELETE_ADDRESS,
  RETURN_ADDRESS_DETAIL,
  EDIT_ADDRESS,
  CURRENT_ADDRESS_SPACE_PLAN,
  PURGE_ADDRESS
} from "queries";
import { IObjectMeta_v1_Input } from "pages/AddressSpaceDetail/AddressSpaceDetailPage";
import { AddressLinksWithFilterAndPagination } from "./AddressLinksWithFilterAndPaginationPage";
import { EditAddress } from "pages/EditAddressPage";
import { IAddressSpacePlanResponse } from "pages/AddressSpaceDetail/AddressList/AddressesListWithFilterAndPaginationPage";
import { POLL_INTERVAL, FetchPolicy } from "constants/constants";
import { ErrorAlert } from "components/common/ErrorAlert";
import { NoDataFound } from "components/common/NoDataFound";
import { useMutationQuery } from "hooks";

export default function AddressDetailPage() {
  const { namespace, name, type, addressname } = useParams();
  const breadcrumb = React.useMemo(
    () => (
      <Breadcrumb>
        <BreadcrumbItem>
          <Link id="ad-page-link-home" to={"/"}>
            Home
          </Link>
        </BreadcrumbItem>
        <BreadcrumbItem>
          <Link
            id="ad-page-link-addresses"
            to={`/address-spaces/${namespace}/${name}/${type}/addresses`}
          >
            {name}
          </Link>
        </BreadcrumbItem>
        <BreadcrumbItem id="ad-page-link-address" isActive={true}>
          Address
        </BreadcrumbItem>
      </Breadcrumb>
    ),
    [name, namespace, type]
  );
  useA11yRouteChange();
  useBreadcrumb(breadcrumb);
  const history = useHistory();

  const [isDeleteModalOpen, setIsDeleteModalOpen] = React.useState(false);
  const [isPurgeModalOpen, setIsPurgeModalOpen] = React.useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = React.useState(false);
  const [addressPlan, setAddressPlan] = React.useState<string | null>(null);
  const [addressSpacePlan, setAddressSpacePlan] = React.useState<string | null>(
    null
  );
  const { loading, error, data, refetch } = useQuery<IAddressDetailResponse>(
    RETURN_ADDRESS_DETAIL(name, namespace, addressname),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  const addressSpaces = useQuery<IAddressSpacePlanResponse>(
    CURRENT_ADDRESS_SPACE_PLAN(name, namespace)
  ).data || { addressSpaces: { addressSpaces: [] } };

  if (!addressSpacePlan && addressSpaces.addressSpaces.addressSpaces[0]) {
    setAddressSpacePlan(
      addressSpaces.addressSpaces.addressSpaces[0].spec.plan.metadata.name
    );
  }

  const resetEditAdressFormState = () => {
    refetch();
    setIsEditModalOpen(!isEditModalOpen);
  };

  const resetDeleteFormState = (data: any) => {
    setIsDeleteModalOpen(!isDeleteModalOpen);
    if (data && data.deleteAddress) {
      history.push(`/address-spaces/${namespace}/${name}/${type}/addresses`);
    }
  };

  const [setDeleteAddressQueryVariables] = useMutationQuery(
    DELETE_ADDRESS,
    resetDeleteFormState,
    resetDeleteFormState
  );
  const [setEditAddressQueryVariables] = useMutationQuery(
    EDIT_ADDRESS,
    resetEditAdressFormState,
    resetEditAdressFormState
  );

  if (loading) return <Loading />;
  const { addresses } = data || {
    addresses: { total: 0, addresses: [] }
  };
  if (addresses.addresses.length <= 0) {
    return (
      <NoDataFound
        type={"Address"}
        name={addressname || ""}
        routeLink={`/address-spaces/${namespace}/${name}/${type}/addresses`}
      />
    );
  }
  const addressDetail = addresses && addresses.addresses[0];
  if (addressPlan === null) {
    setAddressPlan(addressDetail.spec.plan.metadata.name);
  }
  const handleCancelDelete = () => {
    setIsDeleteModalOpen(!isDeleteModalOpen);
  };

  /**
   * delete address
   * @param data
   */
  const deleteAddress = (data: IObjectMeta_v1_Input) => {
    const variables = {
      a: {
        name: data.name,
        namespace: data.namespace
      }
    };
    setDeleteAddressQueryVariables(variables);
  };

  const purgeAddress = async (data: any) => {
    const purgeData = await client.mutate({
      mutation: PURGE_ADDRESS,
      variables: {
        a: {
          name: data.name,
          namespace: data.namespace
        }
      }
    });
    // if (purgeData.errors) {
    //   purgeErrorData.push(purgeData);
    // }
    if (purgeData.data) {
      return purgeData;
    }
  };

  const handleDelete = () => {
    deleteAddress({
      name: addressDetail.metadata.name,
      namespace: addressDetail.metadata.namespace.toString()
    });
  };

  const handleSaving = () => {
    if (addressDetail && type) {
      const variables = {
        a: {
          name: addressDetail.metadata.name,
          namespace: addressDetail.metadata.namespace
        },
        jsonPatch:
          '[{"op":"replace","path":"/spec/plan","value":"' +
          addressPlan +
          '"}]',
        patchType: "application/json-patch+json"
      };
      setEditAddressQueryVariables(variables);
    }
  };

  const handleConfirmPurge = async () => {
    if (
      addressDetail.spec.plan.spec.addressType.toLowerCase() === "queue" ||
      addressDetail.spec.plan.spec.addressType.toLowerCase() === "subscription"
    ) {
      await purgeAddress({
        name: addressDetail.metadata.name,
        namespace: addressDetail.metadata.namespace
      });
    }
    refetch();
    setIsPurgeModalOpen(false);
  };

  const handleCancelPurge = () => {
    setIsPurgeModalOpen(false);
  };

  return (
    <>
      {addressDetail && (
        <AddressDetailHeader
          type={addressDetail.spec.plan.spec.addressType}
          name={addressDetail.spec.address}
          plan={addressDetail.spec.plan.spec.displayName}
          topic={addressDetail.spec.topic}
          storedMessages={getFilteredValue(
            addressDetail.metrics,
            "enmasse_messages_stored"
          )}
          partitions={
            addressDetail.status && addressDetail.status.planStatus
              ? addressDetail.status.planStatus.partitions
              : 0
          }
          onEdit={() => setIsEditModalOpen(true)}
          onDelete={() => setIsDeleteModalOpen(true)}
          onPurge={() => setIsPurgeModalOpen(true)}
        />
      )}
      <Modal
        title="Edit"
        id="addr-detail-edit-modal"
        isSmall
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(!isEditModalOpen)}
        actions={[
          <Button
            key="confirm"
            id="addr-detail-edit-confirm"
            variant="primary"
            onClick={handleSaving}
          >
            Confirm
          </Button>,
          <Button
            key="cancel"
            id="addr-detail-edit-cancel"
            variant="link"
            onClick={() => setIsEditModalOpen(!isEditModalOpen)}
          >
            Cancel
          </Button>
        ]}
        isFooterLeftAligned
      >
        {addressDetail && (
          <EditAddress
            name={addressDetail.metadata.name}
            type={addressDetail.spec.plan.spec.addressType}
            plan={addressPlan || ""}
            addressSpacePlan={addressSpacePlan}
            onChange={setAddressPlan}
          />
        )}
      </Modal>
      {isDeleteModalOpen && (
        <DialoguePrompt
          option="Delete"
          detail={`Are you sure you want to delete this address: ${addressDetail.spec.address} ?`}
          names={[addressDetail.metadata.name]}
          header="Delete this Address ?"
          handleCancelDialogue={handleCancelDelete}
          handleConfirmDialogue={handleDelete}
        />
      )}
      {isPurgeModalOpen && (
        <DialoguePrompt
          option="Purge"
          detail={`Are you sure you want to purge this address: ${addressDetail.spec.address} ?`}
          names={[addressDetail.metadata.name]}
          header="Purge this Address ?"
          handleCancelDialogue={handleCancelPurge}
          handleConfirmDialogue={handleConfirmPurge}
        />
      )}
      <AddressLinksWithFilterAndPagination
        addressspace_name={name || ""}
        addressspace_namespace={namespace || ""}
        addressspace_type={type || " "}
        addressName={addressname || " "}
        addressDisplayName={addressDetail && addressDetail.spec.address}
      />
    </>
  );
}
