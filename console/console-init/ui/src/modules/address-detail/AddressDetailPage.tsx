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
import { AddressDetailHeader } from "modules/address-detail/components/AddressDetailHeader";
import { useQuery } from "@apollo/react-hooks";
import { IAddressDetailResponse } from "types/ResponseTypes";
import { getFilteredValue } from "components/common/ConnectionListFormatter";
import {
  DELETE_ADDRESS,
  RETURN_ADDRESS_DETAIL,
  EDIT_ADDRESS,
  CURRENT_ADDRESS_SPACE_PLAN,
  PURGE_ADDRESS
} from "graphql-module/queries";
import { AddressLinksListPage } from "./containers/AddressLinksListPage";
import { EditAddress } from "modules/address/containers/EditAddressPage";
import { IAddressSpacePlanResponse } from "modules/address/AddressListPage";
import { POLL_INTERVAL, FetchPolicy } from "constants/constants";
import { NoDataFound } from "components/common/NoDataFound";
import { useMutationQuery } from "hooks";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";

export default function AddressDetailPage() {
  const { namespace, name, type, addressname } = useParams();
  const { dispatch } = useStoreContext();

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
    const deleteAddress = data && data.data && data.data.deleteAddress;
    if (deleteAddress) {
      history.push(`/address-spaces/${namespace}/${name}/${type}/addresses`);
    }
  };

  const refetchQueries: string[] = ["single_addresses"];
  const [setPurgeAddressQueryVariables] = useMutationQuery(
    PURGE_ADDRESS,
    refetchQueries
  );

  const [setDeleteAddressQueryVariables] = useMutationQuery(
    DELETE_ADDRESS,
    undefined,
    resetDeleteFormState,
    resetDeleteFormState
  );

  const [setEditAddressQueryVariables] = useMutationQuery(
    EDIT_ADDRESS,
    undefined,
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

  const onDelete = () => {
    const data = addressDetail.metadata;
    const variables = {
      a: {
        name: data.name,
        namespace: data.namespace
      }
    };
    setDeleteAddressQueryVariables(variables);
  };

  const purgeAddress = (data: any) => {
    const variables = {
      a: {
        name: data.name,
        namespace: data.namespace
      }
    };
    setPurgeAddressQueryVariables(variables);
  };

  const onChangeDelete = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_ADDRESS,
      modalProps: {
        selectedItems: [addressDetail.metadata.name],
        data: addressDetail,
        onConfirm: onDelete,
        option: "Delete",
        detail: `Are you sure you want to delete this address: ${addressDetail.spec.address} ?`,
        header: "Delete this Address ?"
      }
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

  const onConfirmPurge = async () => {
    if (
      addressDetail.spec.plan.spec.addressType.toLowerCase() === "queue" ||
      addressDetail.spec.plan.spec.addressType.toLowerCase() === "subscription"
    ) {
      await purgeAddress({
        name: addressDetail.metadata.name,
        namespace: addressDetail.metadata.namespace
      });
    }
  };

  const onChangePurge = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.PURGE_ADDRESS,
      modalProps: {
        selectedItems: [addressDetail.metadata.name],
        data: addressDetail,
        onConfirm: onConfirmPurge,
        option: "Purge",
        detail: `Are you sure you want to purge this address: ${addressDetail.spec.address} ?`,
        header: "Purge this Address ?"
      }
    });
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
          onDelete={onChangeDelete}
          onPurge={onChangePurge}
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
      <AddressLinksListPage
        addressspace_name={name || ""}
        addressspace_namespace={namespace || ""}
        addressspace_type={type || " "}
        addressName={addressname || " "}
        addressDisplayName={addressDetail && addressDetail.spec.address}
      />
    </>
  );
}
