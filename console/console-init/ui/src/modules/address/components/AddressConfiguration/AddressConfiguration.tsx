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
interface IAddressConfigurationProps {
  addressName: string;
  isNameValid: boolean;
  handleAddressChange: (name: string) => void;
  type: string;
  plan: string;
  topic: string;
  isTypeOpen: boolean;
  setIsTypeOpen: (value: boolean) => void;
  isPlanOpen: boolean;
  setIsPlanOpen: (value: boolean) => void;
  isTopicOpen: boolean;
  setIsTopicOpen: (value: boolean) => void;
  onTypeSelect: (value: string) => void;
  onPlanSelect: (value: string) => void;
  onTopicSelect: (value: string) => void;
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
              <DropdownWithToggle
                id="address-definition-type-dropdown"
                className={styles.dropdown_align}
                toggleClass={styles.dropdown_toggle}
                dropdownItemClass={styles.dropdownItem}
                position={DropdownPosition.left}
                onSelectItem={onTypeSelect}
                value={type}
                dropdownItems={typeOptions}
                dropdownItemIdPrefix="address-definition-type-dropdown-item"
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
                dropdownItemIdPrefix="address-definition-plan-dropdown-item"
                isDisabled={type.trim() === ""}
              />
            </FormGroup>
            {type && type === "subscription" && (
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
                  dropdownItemIdPrefix="address-definition-topic-dropdown-item"
                  isDisabled={type.trim() !== "subscription"}
                  isDisplayLabelAndValue={true}
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
