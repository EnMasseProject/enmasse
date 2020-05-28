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
  CardHeader,
  Divider,
  Text,
  TextVariants
} from "@patternfly/react-core";
import { getLabelByKey } from "utils";
import { SwitchWithToggle } from "components";
import { SecretsView, ISecretsViewProps } from "./SecretsView";
import { ExtensionsView } from "./ExtensionsView";
import { hasOwnProperty } from "utils";
import { StyleSheet } from "@patternfly/react-styles";

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
  "auth-id"?: string;
  type?: string;
  ext?: any;
  enabled?: boolean;
}

export interface ICredentialsViewProps {
  id: string;
  credentials: ICredentialView[];
}

export const Credential: React.FC<{ credential: ICredentialView }> = ({
  credential
}) => {
  const { secrets = [], ext = [], type, enabled, "auth-id": authId } =
    credential || {};
  return (
    <>
      <Grid>
        <GridItem span={3}>
          <Title headingLevel="h1" size="md">
            <b>{getLabelByKey("auth-id")}</b>
          </Title>
        </GridItem>
        <GridItem span={9} className={styles.row_margin}>
          {authId}
        </GridItem>
        <GridItem span={3}>
          <Title headingLevel="h1" size="md">
            <b>{getLabelByKey("type")}</b>
          </Title>
        </GridItem>
        <GridItem span={9} className={styles.row_margin}>
          {getLabelByKey(type || "")}
        </GridItem>
        <SecretsView
          secrets={secrets}
          id={"credetials-view-secrets-" + authId}
          heading={"Secrets"}
        />
        <ExtensionsView
          id={"credetials-view-extensions-" + authId}
          ext={ext}
          heading={"Ext"}
        />
        {hasOwnProperty(credential, "enabled") && (
          <>
            <GridItem span={12} className={styles.status_section_margin}>
              Status
            </GridItem>
            <GridItem span={3}>
              <Title headingLevel="h1" size="md">
                <b>Enable</b>
              </Title>
            </GridItem>
            <GridItem span={9}>
              <SwitchWithToggle
                id={authId}
                label={"On"}
                labelOff={"Off"}
                isChecked={enabled}
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
  credentials
}) => {
  const CredentialsNotFound = () => (
    <Text component={TextVariants.p}>
      There are no credentials for this device. This device is connected to the
      other devices as gateways.
    </Text>
  );

  return (
    <Card id={id}>
      <CardHeader>
        <Title id="credential-view-header" headingLevel="h1" size="2xl">
          Credentials
        </Title>
      </CardHeader>
      <CardBody>
        {credentials &&
          credentials.map((credential: ICredentialView, index: number) => {
            const { "auth-id": authId } = credential;
            return (
              <>
                <Credential credential={credential} key={authId} />
                {index < credentials.length - 1 && (
                  <Divider className={styles.devider_margin} />
                )}
              </>
            );
          })}
        {!(credentials && credentials.length > 0) && <CredentialsNotFound />}
      </CardBody>
    </Card>
  );
};
