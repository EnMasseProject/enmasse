/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Grid,
  GridItem,
  FormGroup,
  TextInput,
  TextArea,
  Button
} from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "aphrodite";
import { hasOwnProperty } from "utils";

const styles = StyleSheet.create({
  delete_secret: { float: "right" },
  not_before: { marginRight: 10 }
});

export interface ISecret {
  id?: string;
  "pwd-hash"?: string;
  "not-before"?: string;
  "not-after"?: string;
  comment?: string;
  key?: string;
  "pwd-plain"?: string;
}

export interface ISecretListProps {
  secrets: ISecret[];
  onDeleteSecrets: (
    credentialId: string,
    property?: string,
    secretId?: string
  ) => void;
  handleInputChange: (
    id: string,
    evt: any,
    value: string,
    secretId?: string,
    property?: string
  ) => void;
  credentialId: string;
  isExpandedAdvancedSetting: boolean;
}

interface IDefaultSecretsSetting {
  secret: ISecret;
  id: string;
  index: number;
  credentialId: string;
  handleInputChangeSecrets: (
    id: string,
    evt: any,
    value: string,
    secretId?: string
  ) => void;
}

const DefaultSecretsSetting: React.FC<IDefaultSecretsSetting> = ({
  secret,
  id,
  index,
  credentialId,
  handleInputChangeSecrets
}) => {
  const isRequired = index === 0 ? true : false;

  const handleInputChangeSecret = (value: string, event: any) => {
    handleInputChangeSecrets(credentialId, event, value, id);
  };

  return (
    <>
      {hasOwnProperty(secret, "pwd-hash") && (
        <GridItem span={12}>
          <FormGroup
            fieldId={`secret-list-password-textinput-${id}`}
            label="Password"
            isRequired={isRequired}
          >
            <TextInput
              id={`secret-list-password-textinput-${id}`}
              isRequired={isRequired}
              type="password"
              name="pwd-hash"
              onChange={handleInputChangeSecret}
              value={secret["pwd-hash"]}
            />
          </FormGroup>
          <br />
        </GridItem>
      )}
      {hasOwnProperty(secret, "key") && (
        <GridItem span={12}>
          <FormGroup
            fieldId={`secret-list-key-textinput-${id}`}
            label="Key"
            isRequired={isRequired}
          >
            <TextInput
              id={`secret-list-key-textinput-${id}`}
              isRequired={isRequired}
              type="text"
              name="key"
              onChange={handleInputChangeSecret}
              value={secret["key"]}
            />
          </FormGroup>
        </GridItem>
      )}
    </>
  );
};

const AdvancedSecretsSetting: React.FC<Pick<
  IDefaultSecretsSetting,
  "id" | "secret" | "handleInputChangeSecrets" | "credentialId"
> &
  Pick<ISecretListProps, "onDeleteSecrets" | "secrets">> = ({
  id,
  secret,
  secrets,
  credentialId,
  handleInputChangeSecrets,
  onDeleteSecrets
}) => {
  const handleDeleteSecret = () => {
    onDeleteSecrets(credentialId, "secrets", id);
  };

  const handleInputChangeSecret = (value: string, event: any) => {
    handleInputChangeSecrets(credentialId, event, value, id);
  };

  return (
    <>
      {hasOwnProperty(secret, "not-before") && (
        <GridItem span={6}>
          <FormGroup
            fieldId={`secret-list-not-before-textinput-${id}`}
            label="Not before"
            className={css(styles.not_before)}
          >
            <TextInput
              id={`secret-list-not-before-textinput-${id}`}
              type="datetime-local"
              name="not-before"
              placeholder="YYYY-MM-DD 00:00"
              onChange={handleInputChangeSecret}
              value={secret["not-before"]}
            />
          </FormGroup>
        </GridItem>
      )}
      {hasOwnProperty(secret, "not-after") && (
        <GridItem span={6}>
          <FormGroup
            fieldId={`secret-list-not-after-textinput-"${id}`}
            label="Not after"
          >
            <TextInput
              id={`secret-list-not-after-textinput-${id}`}
              type="datetime-local"
              name="not-after"
              placeholder="YYYY-MM-DD 00:00"
              onChange={handleInputChangeSecret}
              value={secret["not-after"]}
            />
          </FormGroup>
        </GridItem>
      )}
      {hasOwnProperty(secret, "comment") && (
        <GridItem span={12}>
          <br />
          <FormGroup
            fieldId={`secret-list-comment-textinput-${id}`}
            label="Comment"
          >
            <TextArea
              id={`secret-list-comment-textinput-${id}`}
              name="comment"
              onChange={handleInputChangeSecret}
              value={secret["comment"]}
            />
          </FormGroup>
        </GridItem>
      )}
      {secrets.length > 1 && (
        <GridItem span={12}>
          <Button
            id="secret-list-delete-secret-button"
            className={css(styles.delete_secret)}
            variant="link"
            type="button"
            icon={<MinusCircleIcon />}
            onClick={handleDeleteSecret}
          >
            Delete Secret
          </Button>
        </GridItem>
      )}
    </>
  );
};

export const SecretList: React.FC<ISecretListProps> = ({
  secrets,
  onDeleteSecrets,
  handleInputChange,
  credentialId,
  isExpandedAdvancedSetting
}) => {
  const handleInputChangeSecrets = (
    id: string,
    evt: any,
    value: string,
    secretId?: string
  ) => {
    handleInputChange(id, evt, value, secretId, "secrets");
  };

  return (
    <>
      {secrets &&
        secrets.map((secret: ISecret, index: number) => {
          const { id = "" } = secret;
          return (
            <Grid key={id}>
              {
                <DefaultSecretsSetting
                  secret={secret}
                  id={id}
                  index={index}
                  credentialId={credentialId}
                  handleInputChangeSecrets={handleInputChangeSecrets}
                />
              }
              {isExpandedAdvancedSetting && (
                <AdvancedSecretsSetting
                  id={id}
                  secrets={secrets}
                  secret={secret}
                  credentialId={credentialId}
                  handleInputChangeSecrets={handleInputChangeSecrets}
                  onDeleteSecrets={onDeleteSecrets}
                />
              )}
            </Grid>
          );
        })}
    </>
  );
};
