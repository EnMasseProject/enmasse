/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { BreadcrumbItem, Breadcrumb } from "@patternfly/react-core";
import { useBreadcrumb, useA11yRouteChange, Loading } from "use-patternfly";
import { Link, useHistory } from "react-router-dom";
import { useParams } from "react-router";
import { AddressDetailHeader } from "modules/address-detail/components/AddressDetailHeader/AddressDetailHeader";
import { useQuery } from "@apollo/react-hooks";
import { IAddressDetailResponse } from "types/ResponseTypes";
import { getFilteredValue } from "components/common/ConnectionListFormatter";
import {
  DELETE_ADDRESS,
  RETURN_ADDRESS_DETAIL,
  CURRENT_ADDRESS_SPACE_PLAN,
  PURGE_ADDRESS
} from "graphql-module/queries";
import { AddressLinksPage } from "./containers/AddressLinks";
import { IAddressSpacePlanResponse } from "modules/address/AddressPage";
import { POLL_INTERVAL, FetchPolicy } from "constants/constants";
import { NoDataFound } from "components/common/NoDataFound";
import { useMutationQuery } from "hooks";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import { IAddress } from "modules/address/components/AddressList";

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
  const [addressSpacePlan, setAddressSpacePlan] = React.useState<string | null>(
    null
  );
  const { loading, data } = useQuery<IAddressDetailResponse>(
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

  const address: IAddress = {
    name: addressDetail.metadata.name,
    displayName: addressDetail.spec.address,
    namespace: addressDetail.metadata.namespace,
    type: addressDetail.spec.plan.spec.addressType,
    planLabel:
      addressDetail.spec.plan.spec.displayName ||
      addressDetail.spec.plan.metadata.name,
    planValue: addressDetail.spec.plan.metadata.name,
    topic: addressDetail.spec.topic,
    messageIn: getFilteredValue(addressDetail.metrics, "enmasse_messages_in"),
    messageOut: getFilteredValue(addressDetail.metrics, "enmasse_messages_out"),
    storedMessages: getFilteredValue(
      addressDetail.metrics,
      "enmasse_messages_stored"
    ),
    senders: getFilteredValue(addressDetail.metrics, "enmasse_senders"),
    receivers: getFilteredValue(addressDetail.metrics, "enmasse_receivers"),
    partitions:
      addressDetail.status && addressDetail.status.planStatus
        ? addressDetail.status.planStatus.partitions
        : 0,
    isReady: addressDetail.status.isReady,
    creationTimestamp: addressDetail.metadata.creationTimestamp
  };

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

  const onChangeEdit = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.EDIT_ADDRESS,
      modalProps: {
        address,
        addressSpacePlan
      }
    });
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
          type={address.type}
          name={address.name}
          plan={address.planLabel}
          topic={address.topic}
          storedMessages={address.storedMessages}
          partitions={address.partitions || ""}
          onEdit={onChangeEdit}
          onDelete={onChangeDelete}
          onPurge={onChangePurge}
        />
      )}
      <AddressLinksPage
        addressspace_name={name || ""}
        addressspace_namespace={namespace || ""}
        addressspace_type={type || " "}
        addressName={addressname || " "}
        addressDisplayName={addressDetail && addressDetail.spec.address}
      />
    </>
  );
}
