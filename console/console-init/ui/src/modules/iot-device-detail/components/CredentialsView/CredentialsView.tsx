/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Title,
  Grid,
  GridItem,
  Card,
  CardBody,
  Divider,
  Text,
  TextVariants,
  CardTitle,
  Switch
} from "@patternfly/react-core";
import { CheckCircleIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "aphrodite";
import { getLabelByKey } from "utils";
import { StatusLabelWithIcon } from "components";
import { ISecretsViewProps } from "./SecretsView";
import { ExtensionsView } from "./ExtensionsView";
import { hasOwnProperty } from "utils";
import { DialogTypes } from "constant";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import { SecretsViewContainer } from "modules/iot-device-detail/containers";

const styles = StyleSheet.create({
  row_margin: {
    marginBottom: 5
  },
  status_section_margin: {
    marginTop: 20,
    marginBottom: 10
  },
  devider_margin: {
    marginTop: 35,
    marginBottom: 35
  },
  card_border: {
    padding: 15,
    borderColor: "var(--pf-global--palette--black-300)",
    border: "0.1em"
  }
});

export interface ICredentialView extends Pick<ISecretsViewProps, "secrets"> {
  "auth-id": string;
  type?: string;
  ext?: any;
  enabled?: boolean;
}

export interface ICredentialsViewProps
  extends Pick<
    ICredentialProps,
    "toggleCredentialsStatus" | "onConfirmCredentialsStatus"
  > {
  id: string;
  credentials: ICredentialView[];
  enableActions?: boolean;
}

export interface ICredentialProps {
  credential: ICredentialView;
  toggleCredentialsStatus?: (
    authId: string,
    status: boolean,
    credentialType: string
  ) => void;
  onConfirmCredentialsStatus?: (data: any) => Promise<void>;
  enableActions: boolean;
}

export const Credential: React.FC<ICredentialProps> = ({
  credential,
  toggleCredentialsStatus,
  enableActions
}) => {
  const {
    secrets = [],
    ext = [],
    type = "",
    enabled = false,
    "auth-id": authId
  } = credential || {};

  const onChange = (checked: boolean) => {
    toggleCredentialsStatus && toggleCredentialsStatus(authId, checked, type);
  };

  return (
    <Grid>
      <GridItem span={3}>
        <Title headingLevel="h1" size="md" id="credentials-view-auth-id-title">
          {getLabelByKey("auth-id")}
        </Title>
      </GridItem>
      <GridItem span={9} className={css(styles.row_margin)}>
        {authId}
      </GridItem>
      <GridItem span={3}>
        <Title headingLevel="h1" size="md" id="credentials-view-type-title">
          {getLabelByKey("type")}
        </Title>
      </GridItem>
      <GridItem span={9} className={css(styles.row_margin)}>
        {getLabelByKey(type || "")}
      </GridItem>
      <SecretsViewContainer
        secrets={secrets}
        id={"credentials-view-secrets-" + authId}
        heading={"Secrets"}
        authId={authId}
        credentialType={type}
        enableActions={enableActions}
      />
      <ExtensionsView
        id={"credentials-view-extensions-" + authId}
        ext={ext}
        heading={"Ext"}
      />
      {hasOwnProperty(credential, "enabled") && (
        <>
          <GridItem span={12} className={css(styles.status_section_margin)}>
            <Title
              headingLevel="h6"
              size="xl"
              id="credentials-view-status-title"
            >
              Status
            </Title>
          </GridItem>
          <GridItem span={3}>
            <Title
              headingLevel="h1"
              size="md"
              id="credentials-view-enable-title"
            >
              Enable
            </Title>
          </GridItem>
          <GridItem span={9}>
            {enableActions ? (
              <Switch
                id={`credentials-view-status-switch-button-${authId}-${type}`}
                key={`${authId}-${type}`}
                label={"On"}
                labelOff={"Off"}
                isChecked={enabled}
                onChange={onChange}
              />
            ) : (
              <StatusLabelWithIcon
                id="credential-view-status-icon"
                key={`credential-view-status-${authId}-${type}`}
                isEnabled={enabled || false}
                disabledTitle={"Off"}
                enabledTitle={"On"}
                disabledIconColor="var(--pf-global--palette--black-300)"
              />
            )}
          </GridItem>
        </>
      )}
    </Grid>
  );
};

export const CredentialsView: React.FC<ICredentialsViewProps> = ({
  id,
  credentials,
  onConfirmCredentialsStatus,
  enableActions = true
}) => {
  const { dispatch } = useStoreContext();

  const CredentialsNotFound = () => (
    <Text component={TextVariants.p} id="credentials-view-not-found-text">
      There are no credentials for this device. This device is connected to the
      other devices as gateways.
    </Text>
  );

  const toggleCredentialsStatus = (
    authId: string,
    status: boolean,
    credentialType: string
  ) => {
    const dialogType: string = status
      ? DialogTypes.ENABLE
      : DialogTypes.DISABLE;
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.UPDATE_DEVICE_CREDENTIAL_STATUS,
      modalProps: {
        onConfirm: onConfirmCredentialsStatus,
        selectedItems: [authId],
        option: dialogType,
        data: { authId, status, credentialType },
        detail: `Are you sure you want to ${dialogType?.toLowerCase()} this credential: ${authId} ?`,
        header: `${dialogType} this credential ?`,
        confirmButtonLabel: dialogType
      }
    });
  };

  return (
    <Card id={id} className={css(styles.card_border)}>
      {enableActions && (
        <CardTitle>
          <Title id="credentials-view-title" headingLevel="h1" size="2xl">
            Credentials
          </Title>
        </CardTitle>
      )}
      <CardBody>
        {Array.isArray(credentials) &&
          credentials.map((credential: ICredentialView, index: number) => {
            const { "auth-id": authId, type } = credential;
            return (
              <>
                <Credential
                  credential={credential}
                  key={`${authId}-${type}`}
                  toggleCredentialsStatus={toggleCredentialsStatus}
                  enableActions={enableActions}
                />
                {index < credentials.length - 1 && (
                  <Divider className={css(styles.devider_margin)} />
                )}
              </>
            );
          })}
        {!(credentials && credentials.length > 0) && <CredentialsNotFound />}
      </CardBody>
    </Card>
  );
};
