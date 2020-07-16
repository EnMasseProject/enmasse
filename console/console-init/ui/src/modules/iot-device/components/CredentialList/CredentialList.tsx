/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { Fragment } from "react";
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
  CardActions,
  CardHeader,
  ExpandableSection,
  Flex,
  FlexItem
} from "@patternfly/react-core";
import { PlusCircleIcon, TimesIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "aphrodite";
import {
  DropdownWithToggle,
  SwitchWithToggle,
  DividerWithTitle
} from "components";
import {
  SecretList,
  ExtensionList,
  ISecret,
  IExtension
} from "modules/iot-device/components";
import { findIndexByProperty } from "utils";
import {
  credentialTypeOptions,
  SHOW_ADVANCE_SETTING,
  HIDE_ADVANCE_SETTING
} from "modules/iot-device/utils";

const styles = StyleSheet.create({
  addMoreScrets: { marginLeft: -15, marginBottom: 20 },
  addMoreExt: { marginLeft: -15 },
  dropdown_align: { display: "flex" },
  dropdown_toggle_align: { flex: 1 }
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

    const addMoreSecret = () => {
      addMoreItem(id, "secrets");
    };

    const addMoreExtension = () => {
      addMoreItem(id, "ext");
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
            <GridItem span={12} className={css(styles.addMoreScrets)}>
              {selectedType && (
                <Button
                  id="credential-list-add-more-secret-button"
                  variant="link"
                  type="button"
                  icon={<PlusCircleIcon />}
                  onClick={addMoreSecret}
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
                id="credential-list-add-more-ext-keyvalue-button"
                variant="link"
                className={css(styles.addMoreExt)}
                type="button"
                icon={<PlusCircleIcon />}
                onClick={addMoreExtension}
              >
                Add more Ext Key/Value
              </Button>
              <br />
              <br />
            </GridItem>
            <GridItem span={12}>
              <DividerWithTitle title={"Status"} />
              <br />
            </GridItem>
            <Flex>
              <FlexItem>
                <GridItem span={9}>
                  Enable or disable this credential set.
                </GridItem>
              </FlexItem>
              <FlexItem align={{ default: "alignRight" }}>
                <GridItem span={3}>
                  <SwitchWithToggle
                    id={`credential-list-status-switch-${id}`}
                    label={"Enabled"}
                    labelOff={"Disabled"}
                    isChecked={isEnabledStatus}
                    onChange={(checked, event) =>
                      onChangeStatus(id, event, checked)
                    }
                  />
                </GridItem>
              </FlexItem>
            </Flex>
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

  const addMoreCredential = () => {
    addMoreItem();
  };

  return (
    <>
      {credentials &&
        credentials.map((credential: ICredential, index: number) => {
          const { id = "", isExpandedAdvancedSetting = false, type = "" } =
            credential || {};
          return (
            <Fragment key={id}>
              <Card>
                <CardHeader data-codemods="true">
                  <CardActions>
                    {credentials?.length > 1 && (
                      <Button
                        id="credential-list-delete-credential-button"
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
                </CardHeader>
                <CardBody>
                  <Form>
                    <FormGroup
                      fieldId={`credential-list-authid-textinput-${id}`}
                      isRequired
                      label="Auth ID"
                    >
                      <TextInput
                        id={`credential-list-authid-textinput-${id}`}
                        type="text"
                        name="auth-id"
                        isRequired
                        onChange={(value, event) =>
                          handleInputChange(id, event, value)
                        }
                        value={credential["auth-id"]}
                      />
                    </FormGroup>
                    <FormGroup
                      fieldId={`credential-list-type-dropdown-${id}`}
                      isRequired
                      label="Credential type"
                    >
                      <DropdownWithToggle
                        id={`credential-list-type-dropdown-${id}`}
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
                    ) && <DividerWithTitle title={"Secrets"} />}
                    {showAdvancedSetting(id, isExpandedAdvancedSetting, type)}
                    <ExpandableSection
                      toggleText={
                        isExpandedAdvancedSetting
                          ? HIDE_ADVANCE_SETTING
                          : SHOW_ADVANCE_SETTING
                      }
                      onToggle={() => onToggleAdvancedSetting(id)}
                      isExpanded={isExpandedAdvancedSetting}
                    >
                      {""}
                    </ExpandableSection>
                  </Form>
                </CardBody>
              </Card>
              <br />
            </Fragment>
          );
        })}
      <Card>
        <CardBody>
          <Button
            id="credential-list-add-more-credential-button"
            variant="link"
            type="button"
            icon={<PlusCircleIcon />}
            onClick={addMoreCredential}
          >
            Add more credentials
          </Button>
        </CardBody>
      </Card>
    </>
  );
};
