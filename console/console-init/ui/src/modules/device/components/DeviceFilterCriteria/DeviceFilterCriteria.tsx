import React, { useState, ReactElement } from "react";
import { IDropdownOption, DropdownWithToggle } from "components";
import {
  InputGroup,
  TextInput,
  Button,
  Grid,
  GridItem,
  Select,
  SelectVariant,
  SelectOptionObject,
  SelectOption
} from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { ISelectOption } from "utils";

export enum operator {
  LTE = "lessthanequalto",
  GTE = "greaterthanequalto",
  LT = "lessthan",
  GT = "greaterthan",
  EQ = "equalto",
  NEQ = "notequalto"
}

export const operatordropdownOptions: IDropdownOption[] = [
  { key: operator.EQ, value: operator.EQ, label: "=" },
  { key: operator.NEQ, value: operator.NEQ, label: "≠" },
  { key: operator.GTE, value: operator.GTE, label: "≥" },
  { key: operator.GT, value: operator.GT, label: ">" },
  { key: operator.LTE, value: operator.LTE, label: "≤" },
  { key: operator.LT, value: operator.LT, label: "<" }
];

export interface IDeviceFilterCriteria {
  parameter: string;
  operator: string;
  value: string;
  key: string;
}
interface IDeviceFilterCriteriaProps {
  criteria: IDeviceFilterCriteria;
  deleteCriteria: (criteria: IDeviceFilterCriteria) => void;
  setCriteria: (criteria: IDeviceFilterCriteria) => void;
}
const DeviceFilterCriteria: React.FunctionComponent<IDeviceFilterCriteriaProps> = ({
  criteria,
  deleteCriteria,
  setCriteria
}) => {
  const [isExpanded, setIsExpanded] = useState<boolean>(false);
  const options: ISelectOption[] = [
    {
      value: "accelerated speed",
      label: "accelerated speed",
      key: "accelerated speed"
    },
    { value: "distance", label: "distance", key: "distance" },
    { value: "humidity", label: "humidity", key: "humidity" },
    { value: "item", label: "item", key: "item" },
    { value: "location", label: "location", key: "location" },
    { value: "latitude", label: "latitude", key: "latitude" },
    { value: "temparature", label: "temparature", key: "temparature" },
    { value: "team", label: "team", key: "team" },
    { value: "template", label: "template", key: "template" },
    { value: "tend", label: "tend", key: "tend" }
  ];
  const onChangeParameter = (event: any) => {
    const value = event.target.value;
    const criteriaObj = { ...criteria };
    criteriaObj.parameter = value;
    setCriteria(criteriaObj);
    const optionList: ReactElement[] = [];
    return optionList;
  };

  const onChangeValue = (value: string) => {
    const criteriaObj = { ...criteria };
    criteriaObj.value = value.trim();
    setCriteria(criteriaObj);
  };

  const onSelectOperator = (value: string) => {
    const criteriaObj = { ...criteria };
    criteriaObj.operator = value;
    setCriteria(criteriaObj);
  };

  const onClearParameter = () => {
    const criteriaObj = { ...criteria };
    criteriaObj.parameter = "";
    setCriteria(criteriaObj);
  };

  const onSelect = (event: any, value: string | SelectOptionObject) => {
    const criteriaObj = { ...criteria };
    criteriaObj.parameter = value.toString();
    setCriteria(criteriaObj);
    setIsExpanded(false);
  };

  const onToggle = () => {
    setIsExpanded(!isExpanded);
  };

  return (
    <InputGroup>
      <Grid>
        <GridItem span={5}>
          <Select
            variant={SelectVariant.typeahead}
            ariaLabelTypeAhead="Select a parameter"
            onToggle={onToggle}
            onSelect={onSelect}
            onClear={onClearParameter}
            onFilter={onChangeParameter}
            selections={criteria.parameter}
            isExpanded={isExpanded}
            ariaLabelledBy={"Select a parameter"}
          >
            {options.map((option, index) => (
              <SelectOption
                isDisabled={option.isDisabled}
                key={index}
                value={option.value}
              />
            ))}
          </Select>
        </GridItem>
        <GridItem span={2}>
          <DropdownWithToggle
            id={"filter-criteria-operator" + criteria.key}
            name="Filter Criteria Operator"
            onSelectItem={value => onSelectOperator(value)}
            dropdownItems={operatordropdownOptions}
            value={criteria.operator}
            isLabelAndValueNotSame={true}
          />
        </GridItem>
        <GridItem span={5}>
          <TextInput
            isRequired
            type="text"
            id={"filter-criteria-value-input" + criteria.key}
            name="device-id"
            aria-describedby="Filter criteria value input"
            value={criteria.value}
            onChange={onChangeValue}
          />
        </GridItem>
      </Grid>
      <Button
        variant="link"
        id={"remove-criteria" + criteria.key}
        icon={<MinusCircleIcon />}
        onClick={() => deleteCriteria(criteria)}
      />
    </InputGroup>
  );
};

export { DeviceFilterCriteria };
