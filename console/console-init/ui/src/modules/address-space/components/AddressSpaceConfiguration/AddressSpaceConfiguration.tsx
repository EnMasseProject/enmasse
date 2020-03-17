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
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownPosition,
  Radio
} from "@patternfly/react-core";
import { IDropdownOption } from "components/common/FilterDropdown";
import { css, StyleSheet } from "@patternfly/react-styles";
export const dropdown_item_styles = StyleSheet.create({
  format_item: { whiteSpace: "normal", textAlign: "justify" }
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
  setIsNameSpaceOpen: (isOpen: boolean) => void;
  handleNameChange: (name: string) => void;
  handleBrokeredChange: () => void;
  onPlanSelect: (event: any) => void;
  handleStandardChange: () => void;
  setIsPlanOpen: (isOpen: boolean) => void;
  onAuthenticationServiceSelect: (event: any) => void;
  setIsAuthenticationServiceOpen: (isOpen: boolean) => void;
  isNameSpaceOpen: boolean;
  namespace: string;
  namespaceOptions: IDropdownOption[];
  name: string;
  isNameValid: boolean;
  isStandardChecked: boolean;
  isBrokeredChecked: boolean;
  isPlanOpen: boolean;
  type: string;
  plan: string;
  planOptions: IPlanOption[];
  isAuthenticationServiceOpen: boolean;
  authenticationService: string;
  authenticationServiceOptions: IAuthenticationServiceOptions[];
}

export const AddressSpaceConfiguration: React.FC<IAddressSpaceConfigurationProps> = ({
  onNameSpaceSelect,
  isNameSpaceOpen,
  setIsNameSpaceOpen,
  namespace,
  namespaceOptions,
  name,
  isNameValid,
  handleNameChange,
  handleStandardChange,
  isBrokeredChecked,
  handleBrokeredChange,
  onPlanSelect,
  isPlanOpen,
  type,
  setIsPlanOpen,
  plan,
  planOptions,
  onAuthenticationServiceSelect,
  isAuthenticationServiceOpen,
  setIsAuthenticationServiceOpen,
  authenticationService,
  authenticationServiceOptions,
  isStandardChecked
}) => {
  return (
    <>
      <Grid>
        <GridItem span={6}>
          <Form>
            <FormGroup label="Namespace" isRequired={true} fieldId="name-space">
              <br />
              <Dropdown
                id="cas-dropdown-namespace"
                position={DropdownPosition.left}
                onSelect={onNameSpaceSelect}
                isOpen={isNameSpaceOpen}
                style={{ display: "flex" }}
                toggle={
                  <DropdownToggle
                    style={{ flex: "1" }}
                    onToggle={() => setIsNameSpaceOpen(!isNameSpaceOpen)}
                  >
                    {namespace}
                  </DropdownToggle>
                }
                dropdownItems={namespaceOptions.map(option => (
                  <DropdownItem
                    id={`cas-dropdown-item-namespace${option.value}`}
                    key={option.value}
                    value={option.value}
                    itemID={option.value}
                    component={"button"}
                  >
                    <b>{option.label}</b>
                  </DropdownItem>
                ))}
              />
            </FormGroup>
            <FormGroup
              label="Name"
              isRequired={true}
              fieldId="address-space"
              helperText={
                name.trim() !== "" && !isNameValid ? (
                  <small>
                    Only lowercase alphanumeric characters, -, and . allowed,
                    and should start and end with an alpha-numeric character.
                  </small>
                ) : (
                  ""
                )
              }
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
              <Dropdown
                id="cas-dropdown-plan"
                position={DropdownPosition.left}
                onSelect={onPlanSelect}
                isOpen={isPlanOpen}
                style={{ display: "flex" }}
                toggle={
                  <DropdownToggle
                    isDisabled={type.trim() === ""}
                    style={{ flex: "1", position: "inherit" }}
                    onToggle={() => setIsPlanOpen(!isPlanOpen)}
                  >
                    {plan}
                  </DropdownToggle>
                }
                dropdownItems={planOptions.map((option: IPlanOption) => (
                  <DropdownItem
                    id={`cas-dropdown-item-plan${option.value}`}
                    key={option.value}
                    value={option.value}
                    itemID={option.value}
                    component={"button"}
                  >
                    <b>{option.label}</b>
                    <br />
                    <div className={css(dropdown_item_styles.format_item)}>
                      {option.description}
                    </div>
                  </DropdownItem>
                ))}
              />
            </FormGroup>
            <FormGroup
              label="Authentication service"
              isRequired={true}
              fieldId="authentication-service"
            >
              <br />
              <Dropdown
                id="cas-dropdown-auth-service"
                position={DropdownPosition.left}
                onSelect={onAuthenticationServiceSelect}
                isOpen={isAuthenticationServiceOpen}
                style={{ display: "flex" }}
                toggle={
                  <DropdownToggle
                    isDisabled={type.trim() === ""}
                    style={{ flex: "1", position: "inherit" }}
                    onToggle={() =>
                      setIsAuthenticationServiceOpen(
                        !isAuthenticationServiceOpen
                      )
                    }
                  >
                    {authenticationService}
                  </DropdownToggle>
                }
                dropdownItems={authenticationServiceOptions.map(
                  (option: IAuthenticationServiceOptions) => (
                    <DropdownItem
                      id={`cas-dropdown-item-auth-service${option.value}`}
                      key={option.value}
                      value={option.value}
                      itemID={option.value}
                      component={"button"}
                    >
                      <b>{option.label}</b>
                    </DropdownItem>
                  )
                )}
              />
            </FormGroup>
          </Form>
        </GridItem>
      </Grid>
    </>
  );
};
