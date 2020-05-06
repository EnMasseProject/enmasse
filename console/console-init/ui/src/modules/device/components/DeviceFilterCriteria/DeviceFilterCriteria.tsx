import React from "react";
import { IDropdownOption, DropdownWithToggle } from "components";
import {
  InputGroup,
  TextInput,
  Button,
  Grid,
  GridItem
} from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";

export enum operator {
  lte = "lessthanequalto",
  gte = "greaterthanequalto",
  lt = "lessthan",
  gt = "greaterthan",
  eq = "equalto",
  neq = "notequalto"
}

export const operatordropdownOptions: IDropdownOption[] = [
  { key: operator.eq, value: operator.eq, label: "=" },
  { key: operator.neq, value: operator.neq, label: "≠" },
  { key: operator.gte, value: operator.gte, label: "≥" },
  { key: operator.gt, value: operator.gt, label: ">" },
  { key: operator.lte, value: operator.lte, label: "≤" },
  { key: operator.lt, value: operator.lt, label: "<" }
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
  const onChangeParameter = (value: string) => {
    const criteriaObj = criteria;
    criteria.parameter = value;
    setCriteria(criteriaObj);
  };

  const onChangeValue = (value: string) => {
    const criteriaObj = criteria;
    criteria.value = value.trim();
    setCriteria(criteriaObj);
  };

  const onSelectOperator = (value: string) => {
    const criteriaObj = criteria;
    criteria.operator = value;
    setCriteria(criteriaObj);
  };

  return (
    <InputGroup>
      <Grid>
        <GridItem span={5}>
          <TextInput
            isRequired
            type="text"
            id={"filter-criteria-paramter-input" + criteria.key}
            name="parameterInput"
            aria-describedby="Filter criteria value input"
            value={criteria.parameter}
            onChange={onChangeParameter}
          />
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
