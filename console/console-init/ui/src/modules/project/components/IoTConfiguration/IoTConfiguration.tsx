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
  Switch,
  Title,
  TitleSizes
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
      <div style={{ paddingLeft: 20 }}>
        <Grid>
          <GridItem span={6}>
            <Title headingLevel="h2" size={TitleSizes["2xl"]}>
              Configure your project
            </Title>
            <br />
            <Form>
              <FormGroup
                label="Project name"
                isRequired={true}
                fieldId="iot-config-project-name-input"
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
                  validated={
                    name.trim() === "" || isNameValid ? "default" : "error"
                  }
                  type="text"
                  id="iot-config-project-name-input"
                  name="iot-name"
                  onChange={handleNameChange}
                  value={name}
                />
              </FormGroup>
              <FormGroup
                label="Namespace"
                isRequired={true}
                fieldId="iot-config-name-space-selecttoggle"
              >
                <SelectWithToggle
                  selectOptions={namespaceOptions}
                  onSelectItem={onNameSpaceSelect}
                  selections={namespace}
                  ariaLabel={"iot namespace"}
                  id="iot-config-name-space-selecttoggle"
                  optionId="iot-namespace-select-option"
                  variant={SelectVariant.typeahead}
                />
              </FormGroup>
              <FormGroup
                isInline
                label="Enable"
                fieldId="iot-config-enabled-switch"
              >
                <Split hasGutter>
                  <SplitItem>
                    Enable the connections between IoT project and devices
                  </SplitItem>
                  <SplitItem isFilled> </SplitItem>
                  <SplitItem>
                    <Switch
                      id="iot-config-enabled-switch"
                      aria-label="switch to enable connections between IoT projects and devices"
                      isChecked={isEnabled}
                      onChange={() => handleEnabledChange(!isEnabled)}
                    />
                  </SplitItem>
                </Split>
              </FormGroup>
            </Form>
          </GridItem>
        </Grid>
      </div>
    </>
  );
};

export { IoTConfiguration };
