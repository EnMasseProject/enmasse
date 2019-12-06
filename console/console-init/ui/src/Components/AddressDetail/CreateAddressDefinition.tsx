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
import { useQuery } from "@apollo/react-hooks";
import { IDropdownOption } from "../Common/FilterDropdown";
import { RETURN_ADDRESS_PLANS, RETURN_ADDRESS_TYPES } from "src/Queries/Queries";
import { Loading } from "use-patternfly";
import { css, StyleSheet } from "@patternfly/react-styles";

const styles = StyleSheet.create({
  capitalize_labels: {
    "text-transform": "capitalize"
  }
});

export interface IAddressDefinition {
  addressName: string;
  handleAddressChange: (name: string) => void;
  type: string;
  setType: (value: any) => void;
  plan: string;
  setPlan: (value: any) => void;
  planDisabled?: boolean;
}
interface IAddressPlans {
  addressPlans:  Array<{
    Spec: {
      AddressType: string;
      DisplayName: string;
      ShortDescription: string;
    };
  }>;
}
interface IAddressTypes {
  addressTypes_v2:  Array<{
    Spec: {
      DisplayName: string;
      LongDescription: string;
      ShortDescription: string;
    };
  }>;
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
  const { loading, error, data } = useQuery<IAddressTypes>(
    RETURN_ADDRESS_TYPES
  );
  const { addressPlans } = useQuery<IAddressPlans>(
    RETURN_ADDRESS_PLANS
  ).data || {
    addressPlans : []
  };
  
  if (loading) return <Loading />;
  if (error) return <Loading />;
  const { addressTypes_v2 } = data || {
    addressTypes_v2: []
  };

  let typeOptions: IDropdownOption[] = addressTypes_v2.map(type => {
    return {
      value: type.Spec.DisplayName,
      label: type.Spec.DisplayName,
      description: type.Spec.ShortDescription
    }
  });

  let planOptions: any[] = [];

  if(type){
    planOptions = addressPlans.map(plan => {
      if(plan.Spec.AddressType === type){
        return {
          value: plan.Spec.DisplayName,
          label: plan.Spec.DisplayName,
          description: plan.Spec.ShortDescription
        }
      }
    }).filter(plan => plan !== undefined) || [];
  }
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
                    <b className={css(styles.capitalize_labels)}>{option.label}</b>
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
