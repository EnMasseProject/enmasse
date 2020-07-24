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
  CardTitle
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { getLabelByKey } from "utils";
import { SwitchWithToggle } from "components";
import { SecretsView, ISecretsViewProps } from "./SecretsView";
import { ExtensionsView } from "./ExtensionsView";
import { hasOwnProperty } from "utils";

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
  }
});

interface ICredentialView extends Pick<ISecretsViewProps, "secrets"> {
  "auth-id": string;
  type?: string;
  ext?: any;
  enabled?: boolean;
}

export interface ICredentialsViewProps
  extends Pick<ICredentialProps, "onChangeStatus">,
    Pick<ISecretsViewProps, "onConfirmPassword"> {
  id: string;
  credentials: ICredentialView[];
}

export interface ICredentialProps
  extends Pick<ISecretsViewProps, "onConfirmPassword"> {
  credential: ICredentialView;
  onChangeStatus?: (authId: string) => void;
}

export const Credential: React.FC<ICredentialProps> = ({
  credential,
  onChangeStatus,
  onConfirmPassword
}) => {
  const { secrets = [], ext = [], type, enabled, "auth-id": authId } =
    credential || {};

  const onChange = () => {
    onChangeStatus && onChangeStatus(authId);
  };

  return (
    <>
      <Grid>
        <GridItem span={3}>
          <Title
            headingLevel="h1"
            size="md"
            id="credentials-view-auth-id-title"
          >
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
        <SecretsView
          secrets={secrets}
          id={"credetials-view-secrets-" + authId}
          heading={"Secrets"}
          onConfirmPassword={onConfirmPassword}
        />
        <ExtensionsView
          id={"credetials-view-extensions-" + authId}
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
              <SwitchWithToggle
                id={authId}
                label={"On"}
                labelOff={"Off"}
                isChecked={enabled}
                onChange={onChange}
              />
            </GridItem>
          </>
        )}
      </Grid>
    </>
  );
};

export const CredentialsView: React.FC<ICredentialsViewProps> = ({
  id,
  credentials,
  onChangeStatus,
  onConfirmPassword
}) => {
  const CredentialsNotFound = () => (
    <Text component={TextVariants.p} id="credentials-view-not-found-text">
      There are no credentials for this device. This device is connected to the
      other devices as gateways.
    </Text>
  );

  return (
    <Card id={id}>
      <CardTitle>
        <Title id="credentials-view-title" headingLevel="h1" size="2xl">
          Credentials
        </Title>
      </CardTitle>
      <CardBody>
        {credentials &&
          credentials.map((credential: ICredentialView, index: number) => {
            const { "auth-id": authId } = credential;
            return (
              <>
                <Credential
                  credential={credential}
                  key={authId}
                  onChangeStatus={onChangeStatus}
                  onConfirmPassword={onConfirmPassword}
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
