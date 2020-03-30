/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Grid,
  GridItem,
  Form,
  FormGroup,
  TextInput,
  DropdownPosition,
  Button,
  Card,
  CardBody,
  CardHead,
  CardActions,
  Expandable
} from "@patternfly/react-core";
import { PlusCircleIcon, ErrorCircleOIcon } from "@patternfly/react-icons";
import { css, StyleSheet } from "@patternfly/react-styles";
import { DropdownWithToggle, ToggleIcon } from "components";
import {
  SecretList,
  ExtensionList,
  ISecret,
  IExtension,
  dropdown_item_styles
} from "modules/device/components";
import { findIndexByProperty, ISelectOption } from "utils";

export const credentialList_styles = StyleSheet.create({
  addMoreScrets: { marginLeft: "-15px", marginBottom: "20px" },
  addMoreExt: { marginLeft: "-15px" }
});

export interface ICredential {
  id?: string;
  "auth-id"?: string;
  type?: string;
  secrets: ISecret[];
  extensions?: IExtension[];
  enabled?: boolean;
  isExpandedAdvancedSetting?: boolean;
}

export interface ICredentialListProps {
  credentials: Array<ICredential>;
  handleInputChange: (
    id: string,
    event: any,
    value: any,
    childObjId?: string,
    property?: string
  ) => void;
  addMoreItem: (id?: string, property?: string) => void;
  onDeleteItem: (id: string, property?: string, childObjId?: string) => void;
  onToggleAdvancedSetting: (id: string) => void;
  onSelectType: (id: string, event: any, value: string) => void;
}

export const CredentialList: React.FC<ICredentialListProps> = ({
  credentials,
  handleInputChange,
  addMoreItem,
  onToggleAdvancedSetting,
  onSelectType,
  onDeleteItem
}) => {
  const typeOptions: ISelectOption[] = [
    { key: "hashed_password", label: "Password", value: "hashed_password" },
    { key: "x509", label: "X-509 Certificate", value: "x509" },
    { key: "psk", label: "PSK", value: "psk" }
  ];

  const showAdvancedSetting = (
    id: string,
    isExpandedAdvancedSetting: boolean,
    selectedType: string
  ) => {
    const index = findIndexByProperty(credentials, "id", id);
    const secrets = (index >= 0 && credentials[index]["secrets"]) || [];
    const extensions = (index >= 0 && credentials[index]["extensions"]) || [];
    const isEnabledStatus =
      (index >= 0 && credentials[index]["enabled"]) || false;
    return (
      <>
        <SecretList
          secrets={secrets}
          onDeleteSecrets={onDeleteItem}
          credentialId={id}
          handleInputChange={handleInputChange}
          isExpandedAdvancedSetting={isExpandedAdvancedSetting}
        />
        {isExpandedAdvancedSetting && (
          <Grid>
            <GridItem span={12} className={credentialList_styles.addMoreScrets}>
              {selectedType && (
                <Button
                  variant="link"
                  type="button"
                  icon={<PlusCircleIcon />}
                  onClick={() => addMoreItem(id, "secrets")}
                >
                  Add more secrets
                </Button>
              )}
            </GridItem>
            <ExtensionList
              credentialId={id}
              extensions={extensions}
              handleInputChange={handleInputChange}
              onDeleteExtension={onDeleteItem}
            />
            <GridItem span={12}>
              <Button
                variant="link"
                className={credentialList_styles.addMoreExt}
                type="button"
                icon={<PlusCircleIcon />}
                onClick={() => addMoreItem(id, "extensions")}
              >
                Add more Ext Key/Value
              </Button>
            </GridItem>
            <GridItem span={12}>
              <br />
              Status
              <br />
              <br />
            </GridItem>
            <GridItem span={10}>
              Enable or disable this credential set. It can also be modified
              after created.
            </GridItem>
            <GridItem span={2}>
              <ToggleIcon
                name="enabled"
                isEnabled={isEnabledStatus}
                enabledTitle="Enabled"
                disabledTitle="Disabled"
                onToggle={(isEnabled, event) =>
                  handleInputChange(id, event, isEnabled)
                }
              />
            </GridItem>
          </Grid>
        )}
      </>
    );
  };

  return (
    <>
      {credentials &&
        credentials.map(credential => {
          const {
            id = "",
            isExpandedAdvancedSetting = false,
            type = ""
          } = credential;
          const isSecretsHeadingVisible = type === "x509" ? false : true;
          return (
            <Grid key={id}>
              <GridItem span={6}>
                <Card>
                  <CardHead>
                    <CardActions>
                      <Button
                        variant="link"
                        type="button"
                        onClick={() => onDeleteItem(id)}
                        icon={<ErrorCircleOIcon />}
                      />
                    </CardActions>
                  </CardHead>
                  <CardBody>
                    <Form>
                      <FormGroup
                        fieldId={"auth-id" + id}
                        isRequired
                        label="Auth ID"
                      >
                        <TextInput
                          id={"auth-id" + id}
                          type="text"
                          name="auth-id"
                          isRequired
                          onChange={(value, event) =>
                            handleInputChange(id, event, value)
                          }
                        />
                      </FormGroup>
                      <FormGroup
                        fieldId={"type" + id}
                        isRequired
                        label="Credential type"
                      >
                        <DropdownWithToggle
                          id={"type" + id}
                          name="type"
                          className={css(dropdown_item_styles.dropdown_align)}
                          toggleClass={css(
                            dropdown_item_styles.dropdown_toggle_align
                          )}
                          position={DropdownPosition.left}
                          onSelectItem={(value, event) =>
                            onSelectType(id, event, value)
                          }
                          dropdownItems={typeOptions}
                          value={type}
                          isLabelAndValueNotSame={true}
                        />
                      </FormGroup>
                      {isSecretsHeadingVisible && <div>Secrets</div>}
                      {showAdvancedSetting(id, isExpandedAdvancedSetting, type)}
                      <Expandable
                        toggleText={
                          isExpandedAdvancedSetting
                            ? "Hide advance setting"
                            : "Show advanced setting"
                        }
                        onToggle={() => onToggleAdvancedSetting(id)}
                        isExpanded={isExpandedAdvancedSetting}
                      >
                        {""}
                      </Expandable>
                    </Form>
                  </CardBody>
                </Card>
                <br />
              </GridItem>
            </Grid>
          );
        })}
      <Grid>
        <GridItem span={6}>
          <Card>
            <CardBody>
              <Button
                variant="link"
                type="button"
                icon={<PlusCircleIcon />}
                onClick={() => addMoreItem()}
              >
                Add more credentials
              </Button>
            </CardBody>
          </Card>
        </GridItem>
      </Grid>
    </>
  );
};
