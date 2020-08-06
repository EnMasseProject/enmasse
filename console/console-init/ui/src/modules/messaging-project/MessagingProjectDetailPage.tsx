/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useHistory } from "react-router";
import { useParams, Link } from "react-router-dom";
import {
  useA11yRouteChange,
  useDocumentTitle,
  Loading,
  useBreadcrumb
} from "use-patternfly";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { StyleSheet, css } from "aphrodite";
import {
  PageSection,
  PageSectionVariants,
  Breadcrumb,
  BreadcrumbItem
} from "@patternfly/react-core";
import {
  IMessagingProjectHeaderProps,
  MessagingProjectHeader,
  MessagingProjectNavigation,
  IAddressSpace
} from "modules/messaging-project/components";
import {
  DOWNLOAD_CERTIFICATE,
  DELETE_MESSAGING_PROJECT,
  RETURN_ADDRESS_SPACE_DETAIL
} from "graphql-module/queries";
import { POLL_INTERVAL, FetchPolicy } from "constant";
import { NoDataFound } from "components";
import { useMutationQuery } from "hooks";
import { Routes } from "./Routes";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import { IAddressSpaceDetailResponse } from "schema/ResponseTypes";

const styles = StyleSheet.create({
  no_bottom_padding: {
    paddingBottom: 0
  }
});

export interface IObjectMeta_v1_Input {
  name: string;
  namespace: string;
}

const breadcrumb = (
  <Breadcrumb>
    <BreadcrumbItem>
      <Link id="home-link" to={"/"}>
        Home
      </Link>
    </BreadcrumbItem>
    <BreadcrumbItem isActive={true}>Address Space</BreadcrumbItem>
  </Breadcrumb>
);
export default function AddressSpaceDetailPage() {
  const { name, namespace, subList } = useParams();
  const { dispatch } = useStoreContext();
  const history = useHistory();
  const client = useApolloClient();
  useA11yRouteChange();
  useBreadcrumb(breadcrumb);
  useDocumentTitle("Messaging Project Detail");

  const { loading, data } = useQuery<IAddressSpaceDetailResponse>(
    RETURN_ADDRESS_SPACE_DETAIL(name, namespace),
    {
      fetchPolicy: FetchPolicy.NETWORK_ONLY,
      pollInterval: POLL_INTERVAL
    }
  );

  const resetFormState = (data: any) => {
    if (data) {
      history.push("/");
    }
  };
  const [setDeleteAddressSpaceQueryVariables] = useMutationQuery(
    DELETE_MESSAGING_PROJECT,
    undefined,
    resetFormState,
    resetFormState
  );

  if (loading) return <Loading />;

  const { addressSpaces } = data || {
    addressSpaces: { total: 0, addressSpaces: [] }
  };
  if (addressSpaces.addressSpaces.length <= 0) {
    return (
      <NoDataFound
        type={"Messaging project"}
        name={name || ""}
        routeLink={"/"}
      />
    );
  }

  const addressSpace: IAddressSpace | null =
    addressSpaces && addressSpaces.addressSpaces.length > 0
      ? {
          name: addressSpaces.addressSpaces[0].metadata.name,
          nameSpace: addressSpaces.addressSpaces[0].metadata.namespace,
          creationTimestamp:
            addressSpaces.addressSpaces[0].metadata.creationTimestamp,
          type: addressSpaces.addressSpaces[0].spec.type,
          displayName:
            addressSpaces.addressSpaces[0].spec.plan.spec.displayName,
          planValue: addressSpaces.addressSpaces[0].spec.plan.metadata.name,
          isReady:
            addressSpaces.addressSpaces[0] &&
            addressSpaces.addressSpaces[0].status &&
            addressSpaces.addressSpaces[0].status.isReady,
          phase:
            addressSpaces.addressSpaces[0] &&
            addressSpaces.addressSpaces[0].status &&
            addressSpaces.addressSpaces[0].status.phase,
          messages:
            addressSpaces.addressSpaces[0] &&
            addressSpaces.addressSpaces[0].status
              ? addressSpaces.addressSpaces[0].status.messages
              : [],
          authenticationService:
            addressSpaces.addressSpaces[0].spec.authenticationService.name
        }
      : null;

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
    link.setAttribute("download", `${name}.crt`);
    document.body.appendChild(link);
    link.click();
    if (link.parentNode) link.parentNode.removeChild(link);
  };

  const onDelete = () => {
    const variables = {
      as: [
        {
          name: addressSpaceDetails.name,
          namespace: addressSpaceDetails.namespace
        }
      ]
    };
    setDeleteAddressSpaceQueryVariables(variables);
  };

  const onChangeDelete = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_MESSAGING_PROJECT,
      modalProps: {
        selectedItems: [addressSpaceDetails.name],
        data: addressSpace,
        onConfirm: onDelete,
        option: "Delete",
        detail: `Are you sure you want to delete this messaging project: ${addressSpaceDetails.name} ?`,
        header: "Delete this Messaging Project ?"
      }
    });
  };

  const onChangeEdit = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.EDIT_ADDRESS_SPACE,
      modalProps: {
        addressSpace
      }
    });
  };

  const metadata =
    addressSpaces &&
    addressSpaces.addressSpaces[0] &&
    addressSpaces.addressSpaces[0].metadata;

  const addressSpaceDetails: IMessagingProjectHeaderProps = {
    name: metadata && metadata.name,
    namespace: metadata && metadata.namespace,
    createdOn: metadata && metadata.creationTimestamp,
    type:
      addressSpaces &&
      addressSpaces.addressSpaces[0] &&
      addressSpaces.addressSpaces[0].spec &&
      addressSpaces.addressSpaces[0].spec.type,
    onDownload: data => {
      downloadCertificate(data);
    },
    onDelete: onChangeDelete,
    onEdit: onChangeEdit
  };

  return (
    <>
      <PageSection
        variant={PageSectionVariants.light}
        className={css(styles.no_bottom_padding)}
      >
        <MessagingProjectHeader {...addressSpaceDetails} />
        <MessagingProjectNavigation
          activeItem={subList || "addresses"}
        ></MessagingProjectNavigation>
      </PageSection>
      <PageSection>
        <Routes />
      </PageSection>
    </>
  );
}
