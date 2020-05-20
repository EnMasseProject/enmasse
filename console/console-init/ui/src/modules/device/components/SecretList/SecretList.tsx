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
import { css, StyleSheet } from "@patternfly/react-styles";
import { hasOwnProperty } from "utils";

export const dropdown_item_styles = StyleSheet.create({
  format_item: { whiteSpace: "normal", textAlign: "justify" },
  dropdown_align: { display: "flex", marginRight: 10 },
  dropdown_toggle_align: { flex: "1" }
});

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

  const defaultSecretsSetting = (
    secret: ISecret,
    id: string,
    index: number
  ) => {
    const isRequired = index === 0 ? true : false;
    return (
      <>
        {hasOwnProperty(secret, "pwd-hash") && (
          <GridItem span={12}>
            <FormGroup
              fieldId={"sc-pwd-hash-textinput-" + id}
              label="Password"
              isRequired={isRequired}
            >
              <TextInput
                id={"sc-pwd-hash-textinput-" + id}
                isRequired={isRequired}
                type="password"
                name="pwd-hash"
                onChange={(value, event) =>
                  handleInputChangeSecrets(credentialId, event, value, id)
                }
              />
            </FormGroup>
            <br />
          </GridItem>
        )}
        {hasOwnProperty(secret, "key") && (
          <GridItem span={12}>
            <FormGroup
              fieldId={"sc-key-textinput-" + id}
              label="Key"
              isRequired={isRequired}
            >
              <TextInput
                id={"sc-key-textinput-" + id}
                isRequired={isRequired}
                type="text"
                name="key"
                onChange={(value, event) =>
                  handleInputChangeSecrets(credentialId, event, value, id)
                }
              />
            </FormGroup>
          </GridItem>
        )}
      </>
    );
  };

  const advancedSecretsSetting = (secret: ISecret, id: string) => {
    return (
      <>
        {hasOwnProperty(secret, "not-before") && (
          <GridItem span={6}>
            <FormGroup
              fieldId={"sc-not-before-textinput-" + id}
              label="Not before"
              className={css(styles.not_before)}
            >
              <TextInput
                id={"sc-not-before-textinput-" + id}
                type="datetime-local"
                name="not-before"
                placeholder="YYYY-MM-DD 00:00"
                onChange={(value, event) =>
                  handleInputChangeSecrets(credentialId, event, value, id)
                }
              />
            </FormGroup>
          </GridItem>
        )}
        {hasOwnProperty(secret, "not-after") && (
          <GridItem span={6}>
            <FormGroup fieldId={"not-after" + id} label="Not after">
              <TextInput
                id={"sc-not-after-textinput-" + id}
                type="datetime-local"
                name="not-after"
                placeholder="YYYY-MM-DD 00:00"
                onChange={(value, event) =>
                  handleInputChangeSecrets(credentialId, event, value, id)
                }
              />
            </FormGroup>
          </GridItem>
        )}
        {hasOwnProperty(secret, "comment") && (
          <GridItem span={12}>
            <br />
            <FormGroup fieldId={"sc-comment-textinput-" + id} label="Comment">
              <TextArea
                id={"sc-comment-textinput-" + id}
                name="comment"
                onChange={(value, event) =>
                  handleInputChangeSecrets(credentialId, event, value, id)
                }
              />
            </FormGroup>
          </GridItem>
        )}
        {secrets.length > 1 && (
          <GridItem span={12}>
            <Button
              className={css(styles.delete_secret)}
              variant="link"
              type="button"
              icon={<MinusCircleIcon />}
              onClick={() => onDeleteSecrets(credentialId, "secrets", id)}
            >
              Delete Secret
            </Button>
          </GridItem>
        )}
      </>
    );
  };

  return (
    <>
      {secrets &&
        secrets.map((secret: ISecret, index: number) => {
          const { id = "" } = secret;
          return (
            <Grid key={id}>
              {defaultSecretsSetting(secret, id, index)}
              {isExpandedAdvancedSetting && advancedSecretsSetting(secret, id)}
            </Grid>
          );
        })}
    </>
  );
};
