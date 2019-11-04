import * as React from 'react';
import {
  Title,
  Form,
  FormGroup,
  FormSelect,
  FormSelectOption,
  TextInput,
  GridItem,
  Grid,
  Dropdown,
  DropdownToggle,
  DropdownItem,
} from '@patternfly/react-core';
import { IDropdownOption } from '../Common/FilterDropdown';

export interface IAddressDefinitionProps {
  name: string;
  setName: (value: string) => void;
  type: string;
  setType: (value: string) => void;
  plan: string;
  setPlan: (value: string) => void;
}
interface IOption {
  value: string;
  label: string;
  details: string;
}

interface ITypeDropdown {
  value: string;
  setValue: (event: any) => void;
  options: IOption[];
}

const typeOptions = [
  {
    value: 'topic',
    label: 'Topic',
    details: 'A publish-subscribe topic',
  },
  {
    value: 'subscription',
    label: 'Subscription',
    details: 'Asubscription on a specified topic',
  },
  {
    value: 'queue',
    label: 'Queue',
    details: 'A store-and-forward queue',
  },
  {
    value: 'multicast',
    label: 'Multicast',
    details:
      "A scalable 'direct' address for sending messages to multiple consumers",
  },
  {
    value: 'anycast',
    label: 'Anycast',
    details: "A scalable 'direct' address for sending messages to one consumer",
  },
];

const planOptions = [
  {
    value: 'small',
    label: 'Small',
    details: 'Create a small topic sharing underlying broker with other topics',
  },
  {
    value: 'medium',
    label: 'Medium',
    details:
      'Create a medium sized topic sharing underlying broker with other topics',
  },
  {
    value: 'large',
    label: 'Large',
    details: 'Create a large topic backed by a dedicated broker',
  },
];

const TypeDropdown: React.FunctionComponent<ITypeDropdown> = ({
  value,
  setValue,
  options,
}) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const onSelect = (event: any) => {
    setIsOpen(!isOpen);
    //setValue(value);
  };
  return (
    <Dropdown
      position="left"
      style={{ width: 400 }}
      onSelect={onSelect}
      isOpen={isOpen}
      toggle={
        <DropdownToggle
          onToggle={() => {
            setIsOpen(!isOpen);
          }}
        >
          {value}
        </DropdownToggle>
      }
      dropdownItems={options.map(option => (
        <DropdownItem
          key={option.value}
          value={option.value}
          itemID={option.value}
        >
          <b>{option.label}</b>
          <br />
          {option.details}
        </DropdownItem>
      ))}
    />
  );
};
export const AddressSpaceDefinition: React.FunctionComponent<
  IAddressDefinitionProps
> = ({ name, setName, type, setType, plan, setPlan }) => {
  const onNameChange = (value: string) => {
    setName(value);
  };
  const onPlanChange = (value: string, event: any) => {
    setPlan(value);
  };
  return (
    <>
      <Title headingLevel="h3" size="lg">
        Define the Address
      </Title>
      <Grid>
        <GridItem span={6}>
          <Form>
            <FormGroup label="Name" isRequired fieldId="horizontal-form-name">
              <TextInput
                value={name}
                type="text"
                id="horizontal-form-name"
                aria-describedby="horizontal-form-name-helper"
                name="horizontal-form-name"
                onChange={onNameChange}
              />
            </FormGroup>
            <FormGroup label="Type" fieldId="horizontal-form-title">
              <br />
              <TypeDropdown
                value={type}
                setValue={setType}
                options={typeOptions}
              />
            </FormGroup>
            <FormGroup label="Plan" fieldId="horizontal-form-title">
              <br />
              <TypeDropdown
                value={plan}
                setValue={setPlan}
                options={planOptions}
              />
            </FormGroup>
          </Form>
        </GridItem>
      </Grid>
    </>
  );
};
