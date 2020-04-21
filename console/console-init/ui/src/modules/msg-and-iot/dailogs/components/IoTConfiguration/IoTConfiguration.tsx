/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Grid,
  GridItem,
  Form,
  FormGroup,
  TextInput,
  Split,
  SplitItem,
  Select,
  SelectOption,
  SelectVariant,
  SelectOptionObject
} from "@patternfly/react-core";
import { IDropdownOption } from "components";
import { ToggleOnIcon, ToggleOffIcon, IconSize } from "@patternfly/react-icons";

const colorOptions = { blue: "var(--pf-global--active-color--100)" };

export interface IIoTConfigurationProps {
  onNameSpaceSelect: (event: any) => void;
  handleNameChange: (name: string) => void;
  handleEnabledChange: (value: boolean) => void;
  namespaceOptions: IDropdownOption[];
  namespace: string;
  name: string;
  isNameValid: boolean;
  isEnabled: boolean;
}

const IoTConfiguration: React.FC<IIoTConfigurationProps> = ({
  onNameSpaceSelect,
  handleNameChange,
  handleEnabledChange,
  namespaceOptions,
  namespace,
  name,
  isNameValid,
  isEnabled
}) => {
  const [isExpanded, setIsExpanded] = useState<boolean>(false);
  const onSelect = (event: any, selection: string | SelectOptionObject) => {
    onNameSpaceSelect(selection.toString());
    setIsExpanded(false);
  };
  const onToggle = () => {
    setIsExpanded(!isExpanded);
  };
  const onClear = () => {
    onNameSpaceSelect("");
  };
  return (
    <>
      <Grid>
        <GridItem span={6}>
          <Form>
            <FormGroup
              label="Project name"
              isRequired={true}
              fieldId="iot"
              helperText={
                name.trim() !== "" && !isNameValid ? (
                  <small>
                    Only lowercase alphanumeric characters, -, and . allowed,
                    and should start and end with an alpha-numeric character.
                  </small>
                ) : (
                  ""
                )
              }
            >
              <TextInput
                isRequired={true}
                isValid={name.trim() === "" || isNameValid}
                type="text"
                id="iot-name"
                name="iot-name"
                onChange={handleNameChange}
                value={name}
              />
            </FormGroup>
            <FormGroup label="Namespace" isRequired={true} fieldId="name-space">
              <Select
                variant={SelectVariant.typeahead}
                ariaLabelTypeAhead="Select a namespace"
                onToggle={onToggle}
                onSelect={onSelect}
                onClear={onClear}
                selections={namespace}
                isExpanded={isExpanded}
                ariaLabelledBy={"select-namespace"}
              >
                {namespaceOptions.map((option, index) => (
                  <SelectOption
                    isDisabled={option.disabled}
                    key={index}
                    value={option.value}
                  />
                ))}
              </Select>
            </FormGroup>
            <FormGroup isInline label="Enable" fieldId="iot-enabled">
              <Split gutter="md">
                <SplitItem>
                  Enable the connections between IoT project and devices
                </SplitItem>
                <SplitItem isFilled> </SplitItem>
                <SplitItem>
                  {isEnabled ? (
                    <ToggleOnIcon
                      onClick={() => {
                        handleEnabledChange(false);
                      }}
                      color={colorOptions.blue}
                      size={IconSize.lg}
                    />
                  ) : (
                    <ToggleOffIcon
                      onClick={() => {
                        handleEnabledChange(true);
                      }}
                      size={IconSize.lg}
                    />
                  )}
                </SplitItem>
              </Split>
            </FormGroup>
          </Form>
        </GridItem>
      </Grid>
    </>
  );
};

export { IoTConfiguration };
