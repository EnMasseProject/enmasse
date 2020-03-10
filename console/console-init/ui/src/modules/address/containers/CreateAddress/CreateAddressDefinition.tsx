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
  DropdownPosition
} from "@patternfly/react-core";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { IDropdownOption } from "components/common/FilterDropdown";
import {
  RETURN_ADDRESS_PLANS,
  RETURN_ADDRESS_TYPES,
  RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION
} from "graphql-module/queries";
import { Loading } from "use-patternfly";
import { css, StyleSheet } from "@patternfly/react-styles";
import { IAddressResponse } from "types/ResponseTypes";

const styles = StyleSheet.create({
  capitalize_labels: {
    "text-transform": "capitalize"
  }
});

export interface IAddressDefinition {
  addressspaceName: string;
  namespace: string;
  addressName: string;
  addressSpacePlan: string;
  handleAddressChange: (name: string) => void;
  type: string;
  setType: (value: any) => void;
  plan: string;
  setPlan: (value: any) => void;
  topic: string;
  addressSpaceType: string;
  setTopic: (value: string) => void;
  planDisabled?: boolean;
  typeOptions: IDropdownOption[];
  setTypeOptions: (values: IDropdownOption[]) => void;
  planOptions: IDropdownOption[];
  setPlanOptions: (values: IDropdownOption[]) => void;
  topicsForSubscription: IDropdownOption[];
  setTopicForSubscripitons: (values: IDropdownOption[]) => void;
  isNameValid: boolean;
}
interface IAddressPlans {
  addressPlans: Array<{
    metadata: {
      name: string;
    };
    spec: {
      addressType: string;
      displayName: string;
      shortDescription: string;
    };
  }>;
}
interface IAddressTypes {
  addressTypes_v2: Array<{
    spec: {
      displayName: string;
      longDescription: string;
      shortDescription: string;
    };
  }>;
}
export const AddressDefinition: React.FunctionComponent<IAddressDefinition> = ({
  addressspaceName,
  namespace,
  addressName,
  addressSpacePlan,
  handleAddressChange,
  isNameValid,
  type,
  setType,
  plan,
  setPlan,
  planDisabled,
  addressSpaceType,
  topic,
  setTopic,
  typeOptions,
  setTypeOptions,
  topicsForSubscription,
  setTopicForSubscripitons,
  planOptions,
  setPlanOptions
}) => {
  const [isTypeOpen, setIsTypeOpen] = React.useState(false);
  const [isTopicOpen, setIsTopicOpen] = React.useState<boolean>(false);
  const client = useApolloClient();

  const onTypeSelect = async (event: any) => {
    if (
      event.currentTarget.childNodes[0] &&
      event.currentTarget.childNodes[0].value
    ) {
      const type = event.currentTarget.childNodes[0].value;
      setType(type);
      const addressPlans = await client.query<IAddressPlans>({
        query: RETURN_ADDRESS_PLANS(addressSpacePlan, type)
      });
      if (addressPlans.data && addressPlans.data.addressPlans.length > 0) {
        const planOptions = addressPlans.data.addressPlans.map(plan => {
          return {
            value: plan.metadata.name,
            label: plan.spec.displayName,
            description: plan.spec.shortDescription
          };
        });
        setPlan(" ");
        setTopic(" ");
        setPlanOptions(planOptions);
      }
      if (type === "subscription") {
        const topics_addresses = await client.query<IAddressResponse>({
          query: RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION(
            addressspaceName,
            namespace,
            type
          )
        });
        if (
          topics_addresses.data &&
          topics_addresses.data.addresses &&
          topics_addresses.data.addresses.addresses.length > 0
        ) {
          const topics = topics_addresses.data.addresses.addresses.map(
            address => {
              return {
                value: address.spec.address,
                label: address.metadata.name
              };
            }
          );
          setTopicForSubscripitons(topics);
        }
      }
      setIsTypeOpen(!isTypeOpen);
    }
  };

  const [isPlanOpen, setIsPlanOpen] = React.useState(false);
  const onPlanSelect = (event: any) => {
    event.currentTarget.childNodes[0] &&
      setPlan(event.currentTarget.childNodes[0].value);
    setIsPlanOpen(!isPlanOpen);
  };
  const onTopicSelect = (event: any) => {
    event.currentTarget.childNodes[0] &&
      setTopic(event.currentTarget.childNodes[0].value);
    setIsTopicOpen(!isTopicOpen);
  };
  const { loading, error, data } = useQuery<IAddressTypes>(
    RETURN_ADDRESS_TYPES,
    {
      variables: {
        a: addressSpaceType
      }
    }
  );
  if (loading) return <Loading />;
  if (error) return <Loading />;
  const { addressTypes_v2 } = data || {
    addressTypes_v2: []
  };
  const types: IDropdownOption[] = addressTypes_v2.map(type => {
    return {
      value: type.spec.displayName,
      label: type.spec.displayName,
      description: type.spec.shortDescription
    };
  });
  if (typeOptions.length === 0) setTypeOptions(types);

  return (
    <>
      <Grid>
        <GridItem span={6}>
          <Form>
            <FormGroup
              label="Address"
              isRequired={true}
              fieldId="address-name"
              helperText={
                addressName.trim() !== "" && !isNameValid ? (
                  <small>
                    Only digits (0-9), lower case letters (a-z), -, and .
                    allowed, and should start with alpha-numeric characters.
                  </small>
                ) : (
                  ""
                )
              }
            >
              <TextInput
                isRequired={true}
                type="text"
                id="address-name"
                name="address-name"
                value={addressName}
                onChange={handleAddressChange}
                isValid={addressName.trim() === "" || isNameValid}
              />
            </FormGroup>

            <FormGroup label="Type" isRequired={true} fieldId="address-type">
              <br />
              <Dropdown
                id="address-definition-type-dropdown"
                position={DropdownPosition.left}
                onSelect={onTypeSelect}
                isOpen={isTypeOpen}
                style={{ display: "flex" }}
                toggle={
                  <DropdownToggle
                    style={{ flex: "1" }}
                    onToggle={() => setIsTypeOpen(!isTypeOpen)}
                  >
                    {type}
                  </DropdownToggle>
                }
                dropdownItems={typeOptions.map(option => (
                  <DropdownItem
                    id={`address-definition-type-dropdown-item${option.value}`}
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

            <FormGroup label="Plan" isRequired={true} fieldId="address-plan">
              <br />
              <Dropdown
                id="address-definition-plan-dropdown"
                position={DropdownPosition.left}
                onSelect={onPlanSelect}
                isOpen={isPlanOpen}
                style={{ display: "flex" }}
                toggle={
                  <DropdownToggle
                    style={{ flex: "1", position: "inherit" }}
                    isDisabled={type.trim() === ""}
                    onToggle={() => setIsPlanOpen(!isPlanOpen)}
                  >
                    {plan}
                  </DropdownToggle>
                }
                dropdownItems={planOptions.map(option => (
                  <DropdownItem
                    id={`address-definition-plan-dropdown-item${option.value}`}
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
            {type && type === "subscription" && (
              <FormGroup
                label="Topic"
                isRequired={true}
                fieldId="address-topic"
              >
                <br />
                <Dropdown
                  id="address-definition-topic-dropdown"
                  position={DropdownPosition.left}
                  onSelect={onTopicSelect}
                  isOpen={isTopicOpen}
                  style={{ display: "flex" }}
                  toggle={
                    <DropdownToggle
                      style={{ flex: "1", position: "inherit" }}
                      onToggle={() => setIsTopicOpen(!isTopicOpen)}
                      isDisabled={type.trim() !== "subscription"}
                    >
                      {topic}
                    </DropdownToggle>
                  }
                  dropdownItems={
                    topicsForSubscription &&
                    topicsForSubscription.map(option => (
                      <DropdownItem
                        id={`address-definition-topic-dropdown-item${option.value}`}
                        key={option.value}
                        value={option.value}
                        itemID={option.value}
                        component={"button"}
                      >
                        <b>{option.label}</b>
                        <br />
                        {option.value}
                      </DropdownItem>
                    ))
                  }
                />
              </FormGroup>
            )}
          </Form>
        </GridItem>
      </Grid>
    </>
  );
};
