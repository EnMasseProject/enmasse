/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import _ from "lodash";
import { Title, Grid, GridItem, Button } from "@patternfly/react-core";
import { EditAltIcon } from "@patternfly/react-icons";
import classNames from "classnames";
import { getLabelByKey } from "utils";
import { ISecret } from "modules/iot-device/components";
import { StyleSheet, css } from "aphrodite";
import { PasswordLabel } from "components";

const styles = StyleSheet.create({
  row_margin: {
    marginBottom: 5
  },
  section_margin: {
    marginTop: 20,
    marginBottom: 10
  },
  c_button_PaddingLeft: {
    paddingLeft: 0
  },
  c_button_PaddingBottom: {
    paddingTop: 0
  }
});

export interface ISecretsViewProps
  extends Pick<
    ISecretRowProps,
    "onOpenUpdatePasswordDialog" | "authId" | "credentialType"
  > {
  id: string;
  secrets: Omit<ISecret, "id">[];
  heading: string;
  enableActions?: boolean;
}

interface ISecretRowProps {
  secret: ISecret;
  enableActions: boolean;
  authId?: string;
  credentialType?: string;
  onOpenUpdatePasswordDialog?: (formData: any) => void;
}

const SecretRow: React.FC<ISecretRowProps> = ({
  secret,
  enableActions,
  authId,
  credentialType,
  onOpenUpdatePasswordDialog
}) => {
  const onOpenPasswordDialog = () => {
    const { id } = secret;
    onOpenUpdatePasswordDialog &&
      onOpenUpdatePasswordDialog({
        authId,
        credentialType,
        secretId: id
      });
  };

  const renderGridItemValue = (value: string, key: string) => {
    if (key === "pwd-hash" && enableActions) {
      return (
        <Button
          id="secrets-view-change-password-button"
          variant="link"
          icon={<EditAltIcon />}
          className={classNames([
            styles.c_button_PaddingLeft,
            styles.c_button_PaddingBottom
          ])}
          onClick={onOpenPasswordDialog}
        >
          Change password
        </Button>
      );
    } else if (key === "key" || (key === "pwd-hash" && !enableActions)) {
      return (
        <PasswordLabel id="secrets-view-key-password-label" value={value} />
      );
    }
    return value;
  };

  const secretsKeys = Object.keys(_.omit(secret, "id"));
  return (
    <>
      {secretsKeys?.map((key: string) => {
        const value = secret && (secret as any)[key];
        return (
          <>
            {value && (
              <Grid key={"secrets-view-" + key}>
                <GridItem span={3}>
                  <Title headingLevel="h1" size="md">
                    {getLabelByKey(key)}
                  </Title>
                </GridItem>
                <GridItem span={9} className={css(styles.row_margin)}>
                  {renderGridItemValue(value, key)}
                </GridItem>
              </Grid>
            )}
          </>
        );
      })}
    </>
  );
};

export const SecretsView: React.FC<ISecretsViewProps> = ({
  id,
  secrets,
  heading,
  enableActions = true,
  authId,
  credentialType,
  onOpenUpdatePasswordDialog
}) => {
  return (
    <>
      {secrets && secrets.length > 0 && (
        <Grid id={id}>
          <GridItem span={12} className={css(styles.section_margin)}>
            <Title headingLevel="h6" size="xl">
              {heading}
            </Title>
          </GridItem>
          {secrets.map((secret: ISecret, index: number) => (
            <>
              <SecretRow
                secret={secret}
                enableActions={enableActions}
                authId={authId}
                credentialType={credentialType}
                onOpenUpdatePasswordDialog={onOpenUpdatePasswordDialog}
              />
              {index < secrets.length - 1 && <br />}
            </>
          ))}
        </Grid>
      )}
    </>
  );
};
