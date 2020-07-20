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
  Radio,
  Switch
} from "@patternfly/react-core";
import { css, StyleSheet } from "aphrodite";
import { DropdownWithToggle, IDropdownOption } from "components";

export const dropdown_item_styles = StyleSheet.create({
  format_item: { whiteSpace: "normal", textAlign: "justify" },
  dropdown_align: { display: "flex" },
  dropdown_toggle_align: { flex: "1" },
  dropdown_item: { fontWeight: "bold" }
});

export interface IPlanOption {
  label: string;
  value: string;
  description?: string;
}

export interface IAuthenticationServiceOptions {
  value: string;
  label: string;
}

export interface IMessagingConfigurationProps {
  onNameSpaceSelect: (event: any) => void;
  handleNameChange: (name: string) => void;
  handleTypeChange: (checked: boolean, event: any) => void;
  onPlanSelect: (event: any) => void;
  onAuthenticationServiceSelect: (event: any) => void;
  namespaceOptions: IDropdownOption[];
  namespace: string;
  name: string;
  isNameValid: boolean;
  type: string;
  plan: string;
  planOptions: IPlanOption[];
  authenticationService: string;
  authenticationServiceOptions: IAuthenticationServiceOptions[];
  customizeEndpoint?: boolean;
  handleCustomEndpointChange: (value: boolean) => void;
}

export const MessagingConfiguration: React.FC<IMessagingConfigurationProps> = ({
  onNameSpaceSelect,
  namespace,
  namespaceOptions,
  name,
  isNameValid,
  handleNameChange,
  handleTypeChange,
  onPlanSelect,
  type,
  plan,
  planOptions,
  onAuthenticationServiceSelect,
  authenticationService,
  authenticationServiceOptions,
  customizeEndpoint,
  handleCustomEndpointChange
}) => {
  const getHelperText = () => {
    return name.trim() !== "" && !isNameValid ? (
      <small>
        Only lowercase alphanumeric characters, -, and . allowed, and should
        start and end with an alpha-numeric character.
      </small>
    ) : (
      ""
    );
  };

  return (
    <>
      <Grid>
        <GridItem span={6}>
          <Form>
            <FormGroup
              label="Namespace"
              isRequired={true}
              fieldId="messaging-config-name-space-dropdowntoggle"
            >
              <br />
              <DropdownWithToggle
                id="messaging-config-name-space-dropdowntoggle"
                className={css(dropdown_item_styles.dropdown_align)}
                toggleClass={css(dropdown_item_styles.dropdown_toggle_align)}
                dropdownItemClass={css(dropdown_item_styles.dropdown_item)}
                position={DropdownPosition.left}
                onSelectItem={onNameSpaceSelect}
                dropdownItems={namespaceOptions}
                value={namespace}
              />
            </FormGroup>
            <FormGroup
              label="Name"
              isRequired={true}
              fieldId="messaging-config-address-space-input"
              helperText={getHelperText()}
            >
              <TextInput
                isRequired={true}
                validated={
                  name.trim() === "" || isNameValid ? "default" : "error"
                }
                type="text"
                id="messaging-config-address-space-input"
                aria-label="Input address space"
                name="address-space"
                onChange={handleNameChange}
                value={name}
              />
            </FormGroup>
            <FormGroup
              isInline
              label="Type"
              fieldId="messaging-config-type-radio"
              isRequired={true}
            >
              <Radio
                isChecked={type === "standard"}
                onChange={handleTypeChange}
                value={"standard"}
                id="messaging-config-standard-radio"
                label="Standard"
                name="radio-5"
              />
              <Radio
                isChecked={type === "brokered"}
                onChange={handleTypeChange}
                id="messaging-config-brokered-radio"
                value={"brokered"}
                label="Brokered"
                name="radio-6"
              />
            </FormGroup>
            <FormGroup
              label="Address space plan"
              isRequired={true}
              fieldId="messaging-config-plan-dropdowntoggle"
            >
              <br />
              <DropdownWithToggle
                id="messaging-config-plan-dropdowntoggle"
                position={DropdownPosition.left}
                onSelectItem={onPlanSelect}
                className={css(dropdown_item_styles.dropdown_align)}
                toggleClass={css(dropdown_item_styles.dropdown_toggle_align)}
                dropdownItemClass={css(dropdown_item_styles.dropdown_item)}
                dropdownItems={planOptions}
                isDisabled={type.trim() === ""}
                value={plan}
              />
            </FormGroup>
            <FormGroup
              label="Authentication service"
              isRequired={true}
              fieldId="messaging-config-auth-service-dropdowntoggle"
            >
              <br />
              <DropdownWithToggle
                id="messaging-config-auth-service-dropdowntoggle"
                position={DropdownPosition.left}
                onSelectItem={onAuthenticationServiceSelect}
                className={css(dropdown_item_styles.dropdown_align)}
                toggleClass={css(dropdown_item_styles.dropdown_toggle_align)}
                dropdownItemClass={css(dropdown_item_styles.dropdown_item)}
                dropdownItems={authenticationServiceOptions}
                isDisabled={type.trim() === ""}
                value={authenticationService}
              />
            </FormGroup>

            <FormGroup fieldId="messaging-config-customize-endpoint-switch">
              <br />
              <Switch
                id="messaging-config-customize-endpoint-switch"
                label={"Customize Endpoint"}
                isChecked={customizeEndpoint}
                onChange={handleCustomEndpointChange}
              />
            </FormGroup>
          </Form>
        </GridItem>
      </Grid>
    </>
  );
};
