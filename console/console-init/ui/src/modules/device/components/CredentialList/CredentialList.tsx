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
import { PlusCircleIcon, TimesIcon } from "@patternfly/react-icons";
import { css, StyleSheet } from "@patternfly/react-styles";
import {
  DropdownWithToggle,
  SwitchWithToggle,
  DividerWithHeading
} from "components";
import {
  SecretList,
  ExtensionList,
  ISecret,
  IExtension
} from "modules/device/components";
import { findIndexByProperty } from "utils";
import {
  credentialTypeOptions,
  SHOW_ADVANCE_SETTING,
  HIDE_ADVANCE_SETTING
} from "modules/device/utils";

const styles = StyleSheet.create({
  addMoreScrets: { marginLeft: "-15px", marginBottom: "20px" },
  addMoreExt: { marginLeft: "-15px" },
  format_item: { whiteSpace: "normal", textAlign: "justify" },
  dropdown_align: { display: "flex" },
  dropdown_toggle_align: { flex: "1" },
  crd_section: {
    marginTop: "var(--pf-global--spacer--xl)"
  }
});

export interface ICredential {
  id?: string;
  "auth-id"?: string;
  type?: string;
  secrets: ISecret[];
  ext?: IExtension[];
  enabled?: boolean;
  isExpandedAdvancedSetting?: boolean;
}

export interface ICredentialListProps {
  credentials: Array<ICredential>;
  handleInputChange: (
    id: string,
    event: any,
    value: string | boolean,
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
  const showAdvancedSetting = (
    id: string,
    isExpandedAdvancedSetting: boolean,
    selectedType: string
  ) => {
    const index = findIndexByProperty(credentials, "id", id);
    const secrets = (index >= 0 && credentials[index]["secrets"]) || [];
    const extensions = (index >= 0 && credentials[index]["ext"]) || [];
    const isEnabledStatus =
      (index >= 0 && credentials[index]["enabled"]) || false;

    const onChangeStatus = (id: string, event: any, checked: boolean) => {
      event.target.name = "enabled";
      handleInputChange(id, event, checked);
    };

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
            <GridItem span={12} className={styles.addMoreScrets}>
              {selectedType && (
                <Button
                  variant="link"
                  type="button"
                  icon={<PlusCircleIcon />}
                  onClick={() => addMoreItem(id, "secrets")}
                >
                  Add more secret
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
              <br />
              <Button
                variant="link"
                className={styles.addMoreExt}
                type="button"
                icon={<PlusCircleIcon />}
                onClick={() => addMoreItem(id, "ext")}
              >
                Add more Ext Key/Value
              </Button>
            </GridItem>
            <GridItem span={12}>
              <DividerWithHeading heading={"Status"} />
              <br />
            </GridItem>
            <GridItem span={10}>
              Enable or disable this credential set.
            </GridItem>
            <GridItem span={2}>
              <SwitchWithToggle
                id={"cl-status-switch-" + id}
                label={"Enabled"}
                labelOff={"Disabled"}
                isChecked={isEnabledStatus}
                onChange={(checked, event) =>
                  onChangeStatus(id, event, checked)
                }
              />
            </GridItem>
          </Grid>
        )}
      </>
    );
  };

  const shouldSecretsHeadingVisible = (
    type: string,
    isExpandedAdvancedSetting: boolean
  ) => {
    if (type === "x509" && !isExpandedAdvancedSetting) {
      return false;
    }
    return true;
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
          return (
            <Grid key={id}>
              <GridItem span={6}>
                <Card>
                  <CardHead>
                    <CardActions>
                      {credentials.length > 1 && (
                        <Button
                          variant="link"
                          type="button"
                          onClick={() => onDeleteItem(id)}
                          icon={
                            <TimesIcon
                              color={"var(--pf-c-button--m-plain--Color)"}
                            />
                          }
                        />
                      )}
                    </CardActions>
                  </CardHead>
                  <CardBody>
                    <Form>
                      <FormGroup
                        fieldId={"cl-auth-id-textinput-" + id}
                        isRequired
                        label="Auth ID"
                      >
                        <TextInput
                          id={"cl-auth-id-textinput" + id}
                          type="text"
                          name="auth-id"
                          isRequired
                          onChange={(value, event) =>
                            handleInputChange(id, event, value)
                          }
                        />
                      </FormGroup>
                      <FormGroup
                        fieldId={"cl-type-dropdown-" + id}
                        isRequired
                        label="Credential type"
                      >
                        <DropdownWithToggle
                          id={"cl-type-dropdown-" + id}
                          name="type"
                          className={css(styles.dropdown_align)}
                          toggleClass={css(styles.dropdown_toggle_align)}
                          position={DropdownPosition.left}
                          onSelectItem={(value, event) =>
                            onSelectType(id, event, value)
                          }
                          dropdownItems={credentialTypeOptions}
                          value={type}
                          isLabelAndValueNotSame={true}
                        />
                      </FormGroup>
                      {shouldSecretsHeadingVisible(
                        type,
                        isExpandedAdvancedSetting
                      ) && <DividerWithHeading heading={"Secrets"} />}
                      {showAdvancedSetting(id, isExpandedAdvancedSetting, type)}
                      <Expandable
                        toggleText={
                          isExpandedAdvancedSetting
                            ? HIDE_ADVANCE_SETTING
                            : SHOW_ADVANCE_SETTING
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
