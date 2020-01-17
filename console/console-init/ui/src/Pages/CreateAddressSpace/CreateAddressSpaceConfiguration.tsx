/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
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
import { useQuery } from "@apollo/react-hooks";
import { IDropdownOption } from "../../Components/Common/FilterDropdown";
import {
  RETURN_ADDRESS_SPACE_PLANS,
  RETURN_NAMESPACES
} from "src/Queries/Queries";
import { Loading } from "use-patternfly";
import { css, StyleSheet } from "@patternfly/react-styles";
const styles = StyleSheet.create({
  capitalize_labels: {
    "text-transform": "capitalize"
  }
});

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
}
export interface IAddressSpacePlans {
  addressSpacePlans: Array<{
    ObjectMeta: {
      Name: string;
      Uid: string;
      CreationTimestamp: Date;
    };
    Spec: {
      AddressSpaceType: string;
    };
  }>;
}
export interface INamespaces {
  namespaces: Array<{
    ObjectMeta: {
      Name: string;
    };
    Status: {
      Phase: string;
    };
  }>;
}

export const AddressSpaceConfiguration: React.FunctionComponent<IAddressSpaceConfiguration> = ({
  name,
  setName,
  namespace,
  setNamespace,
  type,
  setType,
  plan,
  setPlan,
  authenticationService,
  setAuthenticationService
}) => {
  //TODO: Fix namespace value on the textbox
  const [isNameSpaceOpen, setIsNameSpaceOpen] = React.useState(false);
  const [isStandardChecked, setIsStandardChecked] = React.useState(false);
  const [isBrokeredChecked, setIsBrokeredChecked] = React.useState(false);
  const onNameSpaceSelect = (event: any) => {
    setNamespace(event.target.value);
    setIsNameSpaceOpen(!isNameSpaceOpen);
  };
  const [isPlanOpen, setIsPlanOpen] = React.useState(false);
  const onPlanSelect = (event: any) => {
    //innertext being used here as value property is undefined, because of PF defect
    setPlan(event.target.innerText);
    setIsPlanOpen(!isPlanOpen);
  };

  const [
    isAuthenticationServiceOpen,
    setIsAuthenticationServiceOpen
  ] = React.useState(false);
  const onAuthenticationServiceSelect = (event: any) => {
    setAuthenticationService(event.target.value);
    setIsAuthenticationServiceOpen(!isAuthenticationServiceOpen);
  };

  const { loading, error, data } = useQuery<INamespaces>(RETURN_NAMESPACES);
  const { addressSpacePlans } = useQuery<IAddressSpacePlans>(
    RETURN_ADDRESS_SPACE_PLANS
  ).data || {
    addressSpacePlans: []
  };

  if (loading) return <Loading />;
  if (error) return <Loading />;
  const { namespaces } = data || {
    namespaces: []
  };

  let namespaceOptions: IDropdownOption[];

  let planOptions: any[] = [];
  let authenticationServiceOptions: any[] = [];
  namespaceOptions = namespaces.map(namespace => {
    return {
      value: namespace.ObjectMeta.Name,
      label: namespace.ObjectMeta.Name
    };
  });
  if (type) {
    planOptions =
      addressSpacePlans
        .map(plan => {
          if (plan.Spec.AddressSpaceType === type) {
            return {
              value: plan.ObjectMeta.Name,
              label: plan.ObjectMeta.Name
              // description: plan.Spec.ShortDescription
            };
          }
        })
        .filter(plan => plan !== undefined) || [];
  }

  const handleBrokeredChange = () => {
    setIsBrokeredChecked(true);
    setIsStandardChecked(false);
    setType("brokered");
  };

  const handleStandardChange = () => {
    setIsStandardChecked(true);
    setIsBrokeredChecked(false);
    setType("standard");
  };

  const handleNameChange = (name: string) => {
    setName(name);
  };

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
                    <b className={css(styles.capitalize_labels)}>
                      {option.label}
                    </b>
                    <br />
                    {option.description ? option.description : ""}
                  </DropdownItem>
                ))}
              />
            </FormGroup>
            <FormGroup label="Name" isRequired={true} fieldId="address-space">
              <TextInput
                isRequired={true}
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
                    style={{ flex: "1", position: "inherit" }}
                    onToggle={() => setIsPlanOpen(!isPlanOpen)}
                  >
                    {plan}
                  </DropdownToggle>
                }
                dropdownItems={planOptions.map(option => (
                  <DropdownItem
                    id={`cas-dropdown-item-plan${option.value}`}
                    key={option.value}
                    value={option.value}
                    itemID={option.value}
                    component={"button"}
                  >
                    <b>{option.label}</b>
                    <br />
                    {option.description}
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
                position={DropdownPosition.right}
                onSelect={onAuthenticationServiceSelect}
                isOpen={isAuthenticationServiceOpen}
                style={{ display: "flex" }}
                toggle={
                  <DropdownToggle
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
                dropdownItems={authenticationServiceOptions.map(option => (
                  <DropdownItem
                    id={`cas-dropdown-item-auth-service${option.value}`}
                    key={option.value}
                    value={option.value}
                    itemID={option.value}
                    component={"button"}
                  >
                    <b>{option.label}</b>
                    <br />
                    {option.description}
                  </DropdownItem>
                ))}
              />
            </FormGroup>
          </Form>
        </GridItem>
      </Grid>
    </>
  );
};
