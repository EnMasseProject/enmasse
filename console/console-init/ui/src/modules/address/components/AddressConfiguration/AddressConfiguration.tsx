import React from "react";
import {
  Grid,
  GridItem,
  Form,
  FormGroup,
  TextInput,
  Dropdown,
  DropdownPosition,
  DropdownToggle,
  DropdownItem
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";
import { IDropdownOption } from "components/common";
import { dropdown_item_styles } from "modules/address-space";
import { FetchPolicy } from "constants/constants";

const styles = StyleSheet.create({
  capitalize_labels: {
    "text-transform": "capitalize"
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
  onTypeSelect: (event: any) => void;
  onPlanSelect: (event: any) => void;
  onTopicSelect: (event: any) => void;
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
  isTypeOpen,
  setIsTypeOpen,
  isPlanOpen,
  setIsPlanOpen,
  isTopicOpen,
  setIsTopicOpen,
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
                    <div className={css(dropdown_item_styles.format_item)}>
                      {option.description}
                    </div>
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

export { AddressConfiguration };
