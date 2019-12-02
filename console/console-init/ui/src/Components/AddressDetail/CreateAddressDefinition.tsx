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
import { IDropdownOption } from "../Common/FilterDropdown";
export interface IAddressDefinition {
  addressName: string;
  handleAddressChange: (name: string) => void;
  type: string;
  setType: (value: any) => void;
  plan: string;
  setPlan: (value: any) => void;
  planDisabled?: boolean;
}
export const AddressDefinitaion: React.FunctionComponent<IAddressDefinition> = ({
  addressName,
  handleAddressChange,
  type,
  setType,
  plan,
  setPlan,
  planDisabled
}) => {
  const [isTypeOpen, setIsTypeOpen] = React.useState(false);
  const onTypeSelect = (event: any) => {
    setType(event.target.value);
    setIsTypeOpen(!isTypeOpen);
  };

  const [isPlanOpen, setIsPlanOpen] = React.useState(false);
  const onPlanSelect = (event: any) => {
    setPlan(event.target.value);
    setIsPlanOpen(!isPlanOpen);
  };
  const typeOptions: IDropdownOption[] = [
      {
        value: "Topic",
        label: "Topic",
        description: "A publish-subscribe topic"
      },
      {
        value: "Subscripition",
        label: "Subscripition",
        description: "A subscription on a specifed topic"
      },
      {
        value: "Queue",
        label: "Queue",
        description: "A store-and-forward queue"
      },
      {
        value: "Multicast",
        label: "Multicast",
        description:
          "A scalable 'direct' address for sending messages to mulitple consumers"
      },
      {
        value: "Anycast",
        label: "Anycast",
        description:
          "A scalable 'direct' address for sending messages to one consumer"
      }
    ],
    planOptions: IDropdownOption[] = [
      {
        value: "Small",
        label: "Small",
        description:
          "Create a small topic sharing underlying broker with other topics"
      },
      {
        value: "Medium",
        label: "Medium",
        description:
          "Create a medium sized topic sharing underlying broker with other topics"
      },
      {
        value: "Large",
        label: "Large",
        description: "Create a large topic backed by a dedicated broker"
      }
    ];
  return (
    <>
      <Grid>
        <GridItem span={6}>
          <Form>
            <FormGroup label="Name" isRequired={true} fieldId="address-name">
              <TextInput
                isRequired={true}
                type="text"
                id="address-name"
                name="address-name"
                value={addressName}
                onChange={handleAddressChange}
              />
            </FormGroup>

            <FormGroup label="Type" isRequired={true} fieldId="address-type">
              <br />
              <Dropdown
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
                    key={option.value}
                    value={option.value}
                    itemID={option.value}
                    component={"button"}
                  >
                    <b>{option.label}</b>
                    <br />
                    {option.description ? option.description : ""}
                  </DropdownItem>
                ))}
              />
            </FormGroup>

            <FormGroup label="Plan" isRequired={true} fieldId="address-plan">
              <br />
              <Dropdown
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
