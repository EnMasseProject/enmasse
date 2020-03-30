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
  Split,
  SplitItem,
  SelectVariant,
  Switch
} from "@patternfly/react-core";
import { IDropdownOption, SelectWithToggle } from "components";

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
              <SelectWithToggle
                selectOptions={namespaceOptions}
                onSelectItem={onNameSpaceSelect}
                selections={namespace}
                ariaLabel={"iot namespace"}
                id="iot-config-select-namespace"
                optionId="iot-namespace-select-option"
                variant={SelectVariant.typeahead}
              />
            </FormGroup>
            <FormGroup isInline label="Enable" fieldId="iot-enabled">
              <Split gutter="md">
                <SplitItem>
                  Enable the connections between IoT project and devices
                </SplitItem>
                <SplitItem isFilled> </SplitItem>
                <SplitItem>
                  <Switch
                    id="iot-enable-switch"
                    isChecked={isEnabled}
                    onChange={() => handleEnabledChange(!isEnabled)}
                  />
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
