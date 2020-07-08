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
import { StyleSheet, css } from "aphrodite";
import { IDropdownOption, DropdownWithToggle } from "components";

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
  onTypeSelect?: (value: string) => void;
  onPlanSelect?: (value: string) => void;
  onTopicSelect?: (value: string) => void;
  typeOptions: IDropdownOption[];
  planOptions: IDropdownOption[];
  topicsForSubscription: IDropdownOption[];
}

const AddressConfiguration: React.FunctionComponent<IAddressConfigurationProps> = ({
  addressName,
  isNameValid,
  handleAddressChange,
  type,
  plan,
  topic,
  onTypeSelect,
  onPlanSelect,
  onTopicSelect,
  typeOptions,
  planOptions,
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
              fieldId="addr-config-addressname-textinput"
              helperText={getHelperText()}
            >
              <TextInput
                isRequired={true}
                type="text"
                id="addr-config-addressname-textinput"
                aria-label="input text for address name"
                name="address-name"
                value={addressName}
                onChange={handleAddressChange}
                validated={
                  (addressName && addressName.trim() === "") || isNameValid
                    ? "default"
                    : "error"
                }
              />
            </FormGroup>

            <FormGroup
              label="Type"
              isRequired={true}
              fieldId="addr-config-type-dropdown"
            >
              <br />
              <DropdownWithToggle
                id="addr-config-type-dropdown"
                aria-label="Dropdown to select type"
                className={css(styles.dropdown_align)}
                toggleClass={css(styles.dropdown_toggle)}
                dropdownItemClass={css(styles.dropdownItem)}
                position={DropdownPosition.left}
                onSelectItem={onTypeSelect}
                value={type}
                dropdownItems={typeOptions}
                dropdownItemIdPrefix="address-definition-type-dropdown-item"
              />
            </FormGroup>

            <FormGroup
              label="Plan"
              isRequired={true}
              fieldId="addr-config-plan-dropdown"
            >
              <br />
              <DropdownWithToggle
                id="addr-config-plan-dropdown"
                aria-label="dropdown to select plan"
                position={DropdownPosition.left}
                onSelectItem={onPlanSelect}
                className={css(styles.dropdown_align)}
                toggleClass={css(styles.dropdown_toggle)}
                dropdownItemClass={css(styles.dropdownItem)}
                value={plan}
                dropdownItems={planOptions}
                dropdownItemIdPrefix="address-definition-plan-dropdown-item"
                isDisabled={type.trim() === ""}
              />
            </FormGroup>
            {type && type.toLowerCase() === "subscription" && (
              <FormGroup
                label="Topic"
                isRequired={true}
                fieldId="addr-config-topic-dropdown"
              >
                <br />
                <DropdownWithToggle
                  id="addr-config-topic-dropdown"
                  aria-label="dropdown for topic"
                  className={css(styles.dropdown_align)}
                  toggleClass={css(styles.dropdown_toggle)}
                  dropdownItemClass={css(styles.dropdownItem)}
                  position={DropdownPosition.left}
                  onSelectItem={onTopicSelect}
                  value={topic}
                  dropdownItems={topicsForSubscription}
                  dropdownItemIdPrefix="address-definition-topic-dropdown-item"
                  isDisabled={type.trim() !== "subscription"}
                  shouldDisplayLabelAndValue={true}
                />
              </FormGroup>
            )}
          </Form>
        </GridItem>
      </Grid>
    </>
  );
};

export { AddressConfiguration };
