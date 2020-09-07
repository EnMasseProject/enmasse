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
  DropdownPosition
} from "@patternfly/react-core";
import { StyleSheet } from "@patternfly/react-styles";
import { IDropdownOption, DropdownWithToggle } from "components";
import { ExpiryAddress, DeadLetterAddress } from "modules/address/components";
import { AddressTypes } from "constant";

const styles = StyleSheet.create({
  dropdownItem: {
    "text-transform": "capitalize",
    fontWeight: "bold"
  },
  dropdown_align: {
    display: "flex"
  },
  dropdown_toggle: {
    flex: "1"
  }
});

export interface IAddressConfigurationProps {
  addressName: string;
  isNameValid: boolean;
  handleAddressChange: (name: string) => void;
  type: string;
  plan: string;
  topic: string;
  deadletter: string;
  expiryAddress: string;
  onTypeSelect?: (value: string) => void;
  onPlanSelect?: (value: string) => void;
  onTopicSelect?: (value: string) => void;
  onDeadletterSelect?: (value: string) => void;
  onExpiryAddressSelect?: (value: string) => void;
  typeOptions: IDropdownOption[];
  planOptions: IDropdownOption[];
  topicsForSubscription?: IDropdownOption[];
  deadletterOptions?: IDropdownOption[];
}

const AddressConfiguration: React.FunctionComponent<IAddressConfigurationProps> = ({
  addressName,
  isNameValid,
  handleAddressChange,
  type,
  plan,
  topic,
  deadletter,
  expiryAddress,
  onTypeSelect,
  onPlanSelect,
  onTopicSelect,
  onDeadletterSelect,
  onExpiryAddressSelect,
  typeOptions,
  planOptions,
  deadletterOptions,
  topicsForSubscription
}) => {
  const getHelperText = () => {
    if (addressName && addressName.trim() !== "" && !isNameValid) {
      return (
        <small>
          Only digits (0-9), lower case letters (a-z), -, and . allowed, and
          should start with alpha-numeric characters.
        </small>
      );
    }
  };

  return (
    <>
      <Grid>
        <GridItem span={6}>
          <Form>
            <FormGroup
              label="Address"
              isRequired={true}
              fieldId="address-name"
              helperText={getHelperText()}
            >
              <TextInput
                isRequired={true}
                type="text"
                id="address-name"
                name="address-name"
                value={addressName}
                onChange={handleAddressChange}
                isValid={
                  (addressName && addressName.trim() === "") || isNameValid
                }
              />
            </FormGroup>

            <FormGroup label="Type" isRequired={true} fieldId="address-type">
              <br />
              <DropdownWithToggle
                id="address-definition-type-dropdown"
                className={styles.dropdown_align}
                toggleClass={styles.dropdown_toggle}
                dropdownItemClass={styles.dropdownItem}
                position={DropdownPosition.left}
                onSelectItem={onTypeSelect}
                value={type}
                dropdownItems={typeOptions}
                dropdownItemId="address-definition-type-dropdown-item"
              />
            </FormGroup>

            <FormGroup label="Plan" isRequired={true} fieldId="address-plan">
              <br />
              <DropdownWithToggle
                id="address-definition-plan-dropdown"
                position={DropdownPosition.left}
                onSelectItem={onPlanSelect}
                className={styles.dropdown_align}
                toggleClass={styles.dropdown_toggle}
                dropdownItemClass={styles.dropdownItem}
                value={plan}
                dropdownItems={planOptions}
                dropdownItemId="address-definition-plan-dropdown-item"
                isDisabled={type.trim() === ""}
              />
            </FormGroup>
            {type && type.toLowerCase() === AddressTypes.SUBSCRIPTION && (
              <FormGroup
                label="Topic"
                isRequired={true}
                fieldId="address-topic"
              >
                <br />
                <DropdownWithToggle
                  id="address-definition-topic-dropdown"
                  className={styles.dropdown_align}
                  toggleClass={styles.dropdown_toggle}
                  dropdownItemClass={styles.dropdownItem}
                  position={DropdownPosition.left}
                  onSelectItem={onTopicSelect}
                  value={topic}
                  dropdownItems={topicsForSubscription}
                  dropdownItemId="address-definition-topic-dropdown-item"
                  isDisabled={type.trim() !== AddressTypes.SUBSCRIPTION}
                  isDisplayLabelAndValue={true}
                />
              </FormGroup>
            )}
            {(type?.toLocaleLowerCase() === AddressTypes.TOPIC ||
              type?.toLocaleLowerCase() === AddressTypes.QUEUE) && (
              <ExpiryAddress
                expiryAddress={expiryAddress}
                onExpiryAddressSelect={onExpiryAddressSelect}
                deadletterOptions={deadletterOptions}
              />
            )}
            {(type?.toLowerCase() === AddressTypes.SUBSCRIPTION ||
              type?.toLowerCase() === AddressTypes.QUEUE) && (
              <>
                <DeadLetterAddress
                  deadletterAddress={deadletter}
                  onDeadletterSelect={onDeadletterSelect}
                  deadletterOptions={deadletterOptions}
                />
              </>
            )}
          </Form>
        </GridItem>
      </Grid>
    </>
  );
};

export { AddressConfiguration };
