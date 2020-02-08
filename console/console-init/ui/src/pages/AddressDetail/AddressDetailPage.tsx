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
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { IAddressDetailResponse } from "types/ResponseTypes";
import { getFilteredValue } from "components/common/ConnectionListFormatter";
import { DialoguePrompt } from "components/common/DialoguePrompt";
import {
  DELETE_ADDRESS,
  RETURN_ADDRESS_DETAIL,
  EDIT_ADDRESS,
  CURRENT_ADDRESS_SPACE_PLAN
} from "queries";
import { IObjectMeta_v1_Input } from "pages/AddressSpaceDetail/AddressSpaceDetailPage";
import { AddressLinksWithFilterAndPagination } from "./AddressLinksWithFilterAndPaginationPage";
import { EditAddress } from "pages/EditAddressPage";
import { IAddressSpacePlanResponse } from "pages/AddressSpaceDetail/AddressList/AddressesListWithFilterAndPaginationPage";
import { POLL_INTERVAL } from "constants/constants";

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
  const client = useApolloClient();
  const [isDeleteModalOpen, setIsDeleteModalOpen] = React.useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = React.useState(false);
  const [addressPlan, setAddressPlan] = React.useState<string | null>(null);
  const [addressSpacePlan, setAddressSpacePlan] = React.useState<string | null>(
    null
  );
  const { loading, error, data, refetch } = useQuery<IAddressDetailResponse>(
    RETURN_ADDRESS_DETAIL(name, namespace, addressname),
    { pollInterval: 5000 }
  );

  const addressSpaces = useQuery<IAddressSpacePlanResponse>(
    CURRENT_ADDRESS_SPACE_PLAN(name, namespace)
  ).data || { addressSpaces: { AddressSpaces: [] } };

  if (!addressSpacePlan && addressSpaces.addressSpaces.AddressSpaces[0]) {
    setAddressSpacePlan(
      addressSpaces.addressSpaces.AddressSpaces[0].Spec.Plan.ObjectMeta.Name
    );
  }

  if (loading) return <Loading />;
  if (error) console.log(error);
  const { addresses } = data || {
    addresses: { Total: 0, Addresses: [] }
  };

  const addressDetail = addresses && addresses.Addresses[0];
  if (addressPlan === null) {
    setAddressPlan(addressDetail.Spec.Plan.Spec.DisplayName);
  }

  const handleCancelDelete = () => {
    setIsDeleteModalOpen(!isDeleteModalOpen);
  };
  // async function to delete a address space
  const deleteAddress = async (data: IObjectMeta_v1_Input) => {
    const deletedData = await client.mutate({
      mutation: DELETE_ADDRESS,
      variables: {
        a: {
          Name: data.name,
          Namespace: data.namespace
        }
      }
    });
    if (deletedData.data && deletedData.data.deleteAddress) {
      setIsDeleteModalOpen(!isDeleteModalOpen);
      history.push(`/address-spaces/${namespace}/${name}/${type}/addresses`);
    }
  };
  const handleDelete = () => {
    deleteAddress({
      name: addressDetail.ObjectMeta.Name,
      namespace: addressDetail.ObjectMeta.Namespace.toString()
    });
  };

  const handleSaving = async () => {
    if (addressDetail && type) {
      await client.mutate({
        mutation: EDIT_ADDRESS,
        variables: {
          a: {
            Name: addressDetail.ObjectMeta.Name,
            Namespace: addressDetail.ObjectMeta.Namespace.toString()
          },
          jsonPatch:
            '[{"op":"replace","path":"/Plan","value":"' + addressPlan + '"}]',
          patchType: "application/json-patch+json"
        }
      });
    }
    refetch();
    setIsEditModalOpen(!isEditModalOpen);
  };

  return (
    <>
      {addressDetail && (
        <AddressDetailHeader
          type={addressDetail.Spec.Plan.Spec.AddressType}
          name={addressDetail.Spec.Address}
          plan={addressDetail.Spec.Plan.Spec.DisplayName}
          topic={addressDetail.Spec.Topic}
          storedMessages={getFilteredValue(
            addressDetail.Metrics,
            "enmasse_messages_stored"
          )}
          partitions={
            addressDetail.Status && addressDetail.Status.PlanStatus
              ? addressDetail.Status.PlanStatus.Partitions
              : 0
          }
          onEdit={() => setIsEditModalOpen(!isEditModalOpen)}
          onDelete={() => setIsDeleteModalOpen(!isDeleteModalOpen)}
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
        <EditAddress
          name={addressDetail.ObjectMeta.Name}
          type={addressDetail.Spec.Plan.Spec.AddressType}
          plan={addressPlan || ""}
          addressSpacePlan={addressSpacePlan}
          onChange={setAddressPlan}
        />
      </Modal>
      {isDeleteModalOpen && (
        <DialoguePrompt
          option="Delete"
          detail={`Are you sure you want to delete this address: ${addressDetail.Spec.Address} ?`}
          names={[addressDetail.ObjectMeta.Name]}
          header="Delete this Address ?"
          handleCancelDialogue={handleCancelDelete}
          handleConfirmDialogue={handleDelete}
        />
      )}
      <AddressLinksWithFilterAndPagination
        addressspace_name={name || ""}
        addressspace_namespace={namespace || ""}
        addressspace_type={type || " "}
        addressName={addressname || " "}
      />
    </>
  );
}
