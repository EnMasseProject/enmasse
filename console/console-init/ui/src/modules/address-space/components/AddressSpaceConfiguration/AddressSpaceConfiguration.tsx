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
import { css, StyleSheet } from "@patternfly/react-styles";
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

export interface IAddressSpaceConfigurationProps {
  onNameSpaceSelect: (event: any) => void;
  handleNameChange: (name: string) => void;
  handleBrokeredChange: () => void;
  onPlanSelect: (event: any) => void;
  handleStandardChange: () => void;
  onAuthenticationServiceSelect: (event: any) => void;
  namespaceOptions: IDropdownOption[];
  namespace: string;
  name: string;
  isNameValid: boolean;
  isStandardChecked: boolean;
  isBrokeredChecked: boolean;
  type: string;
  plan: string;
  planOptions: IPlanOption[];
  authenticationService: string;
  authenticationServiceOptions: IAuthenticationServiceOptions[];
  customizeEndpoint?: boolean;
  handleCustomEndpointChange: (value: boolean) => void;
}

export const AddressSpaceConfiguration: React.FC<IAddressSpaceConfigurationProps> = ({
  onNameSpaceSelect,
  namespace,
  namespaceOptions,
  name,
  isNameValid,
  handleNameChange,
  handleStandardChange,
  isBrokeredChecked,
  handleBrokeredChange,
  onPlanSelect,
  type,
  plan,
  planOptions,
  onAuthenticationServiceSelect,
  authenticationService,
  authenticationServiceOptions,
  isStandardChecked,
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
            <FormGroup label="Namespace" isRequired={true} fieldId="name-space">
              <br />
              <DropdownWithToggle
                id="cas-dropdown-namespace"
                className={css(dropdown_item_styles.dropdown_align)}
                toggleClass={css(dropdown_item_styles.dropdown_toggle_align)}
                dropdownItemClass={dropdown_item_styles.dropdown_item}
                position={DropdownPosition.left}
                onSelectItem={onNameSpaceSelect}
                dropdownItems={namespaceOptions}
                value={namespace}
              />
            </FormGroup>
            <FormGroup
              label="Name"
              isRequired={true}
              fieldId="address-space"
              helperText={getHelperText()}
            >
              <TextInput
                isRequired={true}
                isValid={name.trim() === "" || isNameValid}
                type="text"
                id="address-space"
                name="address-space"
                onChange={handleNameChange}
                value={name}
              />
            </FormGroup>
            <FormGroup
              isInline
              label="Type"
              fieldId="simple-form-name"
              isRequired={true}
            >
              <Radio
                isChecked={isStandardChecked}
                onChange={handleStandardChange}
                id="cas-standard-radio"
                label="Standard"
                name="radio-5"
              />
              <Radio
                isChecked={isBrokeredChecked}
                onChange={handleBrokeredChange}
                id="cas-brokered-radio"
                label="Brokered"
                name="radio-6"
              />
            </FormGroup>
            <FormGroup
              label="Address space plan"
              isRequired={true}
              fieldId="address-space-plan"
            >
              <br />
              <DropdownWithToggle
                id="cas-dropdown-plan"
                position={DropdownPosition.left}
                onSelectItem={onPlanSelect}
                className={css(dropdown_item_styles.dropdown_align)}
                toggleClass={css(dropdown_item_styles.dropdown_toggle_align)}
                dropdownItemClass={dropdown_item_styles.dropdown_item}
                dropdownItems={planOptions}
                isDisabled={type.trim() === ""}
                value={plan}
              />
            </FormGroup>
            <FormGroup
              label="Authentication service"
              isRequired={true}
              fieldId="authentication-service"
            >
              <br />
              <DropdownWithToggle
                id="cas-dropdown-auth-service"
                position={DropdownPosition.left}
                onSelectItem={onAuthenticationServiceSelect}
                className={css(dropdown_item_styles.dropdown_align)}
                toggleClass={css(dropdown_item_styles.dropdown_toggle_align)}
                dropdownItemClass={dropdown_item_styles.dropdown_item}
                dropdownItems={authenticationServiceOptions}
                isDisabled={type.trim() === ""}
                value={authenticationService}
              />
            </FormGroup>

            <FormGroup fieldId="customize-endpoint">
              <br />
              <Switch
                id="asc-switch-customize-endpoint"
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
