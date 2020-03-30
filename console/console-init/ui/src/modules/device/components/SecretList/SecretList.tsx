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
  DropdownPosition,
  TextArea,
  Button
} from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { css, StyleSheet } from "@patternfly/react-styles";
import { DropdownWithToggle } from "components";
import { hasOwnProperty, ISelectOption } from "utils";

export const dropdown_item_styles = StyleSheet.create({
  format_item: { whiteSpace: "normal", textAlign: "justify" },
  dropdown_align: { display: "flex", marginRight: 10 },
  dropdown_toggle_align: { flex: "1" }
});

export const secrets_styles = StyleSheet.create({
  delete_secret: { float: "right" },
  not_before: { marginRight: "10px" }
});

export interface ISecret {
  id?: string;
  "pwd-hash"?: string;
  "pwd-function"?: string;
  salt?: string;
  "not-before"?: string;
  "not-after"?: string;
  comment?: string;
  key?: string;
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
  const pwdFunctionOptions: ISelectOption[] = [
    { key: "function", label: "function name", value: "function name" },
    { key: "x-509-certificate", label: "X-509 Certificate", value: "X-509" }
  ];

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
      <GridItem span={12}>
        {index > 0 && (
          <GridItem span={12}>
            <Button
              className={secrets_styles.delete_secret}
              variant="link"
              type="button"
              icon={<MinusCircleIcon />}
              onClick={() => onDeleteSecrets(credentialId, "secrets", id)}
            >
              Delete Secrets
              <br />
            </Button>
          </GridItem>
        )}
        {hasOwnProperty(secret, "pwd-hash") && (
          <GridItem span={12}>
            <FormGroup
              fieldId={"pwd-hash" + id}
              label="Password"
              isRequired={isRequired}
            >
              <TextInput
                id={"pwd-hash" + id}
                isRequired={isRequired}
                type="password"
                name="pwd-hash"
                onChange={(value, event) =>
                  handleInputChangeSecrets(credentialId, event, value, id)
                }
              />
            </FormGroup>
          </GridItem>
        )}
        {hasOwnProperty(secret, "key") && (
          <GridItem span={12}>
            <FormGroup fieldId={"key" + id} label="Key" isRequired={isRequired}>
              <TextInput
                id={"key" + id}
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
        <br />
      </GridItem>
    );
  };

  const advancedSecretsSetting = (secret: ISecret, id: string) => {
    return (
      <>
        {hasOwnProperty(secret, "pwd-function") && (
          <GridItem span={6}>
            <FormGroup fieldId={"pwd-function" + id} label="Hash-function">
              <DropdownWithToggle
                id={"pwd-function-" + id}
                name="pwd-function"
                className={css(dropdown_item_styles.dropdown_align)}
                toggleClass={css(dropdown_item_styles.dropdown_toggle_align)}
                position={DropdownPosition.left}
                onSelectItem={(value, event) =>
                  handleInputChangeSecrets(credentialId, event, value, id)
                }
                dropdownItems={pwdFunctionOptions}
                value={secret["pwd-function"] || ""}
              />
            </FormGroup>
          </GridItem>
        )}
        {hasOwnProperty(secret, "salt") && (
          <GridItem span={6}>
            <FormGroup fieldId={"salt" + id} label="Salt">
              <TextInput
                id={"salt" + id}
                type="text"
                name="salt"
                onChange={(value, event) =>
                  handleInputChangeSecrets(credentialId, event, value, id)
                }
              />
            </FormGroup>
            <br />
          </GridItem>
        )}
        {hasOwnProperty(secret, "not-before") && (
          <GridItem span={6}>
            <FormGroup
              fieldId={"not-before" + id}
              label="Time before"
              className={secrets_styles.not_before}
            >
              <TextInput
                id={"not-before" + id}
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
            <FormGroup fieldId={"not-after" + id} label="Time after">
              <TextInput
                id={"not-after" + id}
                type="datetime-local"
                name="not-after"
                placeholder="YYYY-MM-DD 00:00"
                onChange={(value, event) =>
                  handleInputChangeSecrets(credentialId, event, value, id)
                }
              />
            </FormGroup>
            <br />
          </GridItem>
        )}
        {hasOwnProperty(secret, "comment") && (
          <GridItem span={12}>
            <FormGroup fieldId={"comment" + id} label="Comment">
              <TextArea
                id={"comment" + id}
                name="comment"
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
