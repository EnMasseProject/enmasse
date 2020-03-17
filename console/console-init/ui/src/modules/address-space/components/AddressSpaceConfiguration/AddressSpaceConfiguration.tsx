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
<<<<<<< HEAD:console/console-init/ui/src/modules/address-space/dialogs/CreateAddressSpace/CreateAddressSpaceConfiguration.tsx
import {
  RETURN_ADDRESS_SPACE_PLANS,
  RETURN_NAMESPACES,
  RETURN_AUTHENTICATION_SERVICES
} from "graphql-module/queries";
import { Loading } from "use-patternfly";
import { dnsSubDomainRfc1123NameRegexp } from "types/Configs";
import { StyleSheet, css } from "@patternfly/react-styles";
=======
import { css, StyleSheet } from "@patternfly/react-styles";
>>>>>>> Refactor dialogs components (#4110):console/console-init/ui/src/modules/address-space/components/AddressSpaceConfiguration/AddressSpaceConfiguration.tsx

export const dropdown_item_styles = StyleSheet.create({
  format_item: { whiteSpace: "normal", textAlign: "justify" }
});
<<<<<<< HEAD:console/console-init/ui/src/modules/address-space/dialogs/CreateAddressSpace/CreateAddressSpaceConfiguration.tsx
export interface IAddressSpaceConfiguration {
  name: string;
  setName: (name: string) => void;
  type: string;
  setType: (type: string) => void;
  plan: string;
  setPlan: (plan: string) => void;
  namespace: string;
  setNamespace: (namespace: string) => void;
  authenticationService: string;
  setAuthenticationService: (authenticationService: string) => void;
  isNameValid: boolean;
  setIsNameValid: (isNameValid: boolean) => void;
}
export interface IAddressSpacePlans {
  addressSpacePlans: Array<{
    metadata: {
      name: string;
      uid: string;
      creationTimestamp: Date;
    };
    spec: {
      addressSpaceType: string;
      displayName: string;
      longDescription: string;
      shortDescription: string;
    };
  }>;
}

export interface IAddressSpaceAuthServiceResponse {
  addressSpaceSchema_v2: IAddressSpaceAuthService[];
=======

export interface IPlanOption {
  label: string;
  value: string;
  description?: string;
>>>>>>> Refactor dialogs components (#4110):console/console-init/ui/src/modules/address-space/components/AddressSpaceConfiguration/AddressSpaceConfiguration.tsx
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
