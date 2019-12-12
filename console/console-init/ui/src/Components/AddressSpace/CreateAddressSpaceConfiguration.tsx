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
import { IDropdownOption } from "../Common/FilterDropdown";
import {
  RETURN_ADDRESS_PLANS,
  RETURN_ADDRESS_TYPES
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
  addressPlans: Array<{
    Spec: {
      AddressType: string;
      DisplayName: string;
      ShortDescription: string;
    };
  }>;
}
interface INamespaces {
  addressTypes_v2: Array<{
    Spec: {
      DisplayName: string;
      LongDescription: string;
      ShortDescription: string;
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
  const [isNameSpaceOpen, setIsNameSpaceOpen] = React.useState(false);
  const onNameSpaceSelect = (event: any) => {
    setType(event.target.value);
    setIsNameSpaceOpen(!isNameSpaceOpen);
  };
  const [isPlanOpen, setIsPlanOpen] = React.useState(false);
  const onPlanSelect = (event: any) => {
    setPlan(event.target.value);
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

  const { loading, error, data } = useQuery<INamespaces>(RETURN_ADDRESS_TYPES);
  const { addressPlans } = useQuery<IAddressSpacePlans>(RETURN_ADDRESS_PLANS)
    .data || {
    addressPlans: []
  };

  if (loading) return <Loading />;
  if (error) return <Loading />;
  const { addressTypes_v2 } = data || {
    addressTypes_v2: []
  };

  let namespaceOptions: IDropdownOption[] = addressTypes_v2.map(type => {
    return {
      value: type.Spec.DisplayName,
      label: type.Spec.DisplayName,
      description: type.Spec.ShortDescription
    };
  });

  let planOptions: any[] = [];
  let authenticationServiceOptions: any[] = [];

  // if(type){
  //   planOptions = addressPlans.map(plan => {
  //     if(plan.Spec.AddressType === type){
  //       return {
  //         value: plan.Spec.DisplayName,
  //         label: plan.Spec.DisplayName,
  //         description: plan.Spec.ShortDescription
  //       }
  //     }
  //   }).filter(plan => plan !== undefined) || [];
  // }
  return (
    <>
      <Grid>
        <GridItem span={6}>
          <Form>
            <FormGroup label="Namespace" isRequired={true} fieldId="name-space">
              <br />
              <Dropdown
                position={DropdownPosition.left}
                onSelect={onNameSpaceSelect}
                isOpen={isNameSpaceOpen}
                style={{ display: "flex" }}
                toggle={
                  <DropdownToggle
                    style={{ flex: "1" }}
                    onToggle={() => setIsNameSpaceOpen(!isNameSpaceOpen)}
                  >
                    {type}
                  </DropdownToggle>
                }
                dropdownItems={namespaceOptions.map(option => (
                  <DropdownItem
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
                // onChange={handleAddressChange}
              />
            </FormGroup>
            <FormGroup
              isInline
              label="Type"
              fieldId="simple-form-name"
              isRequired={true}
            >
              <Radio id="cas-standard-radio" label="Standard" name="radio-5" />
              <Radio id="cas-brokered-radio" label="Brokered" name="radio-6" />
            </FormGroup>
            <FormGroup
              label="Address space plan"
              isRequired={true}
              fieldId="address-space-plan"
            >
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
            <FormGroup
              label="Authentication service"
              isRequired={true}
              fieldId="authentication-service"
            >
              <br />
              <Dropdown
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
