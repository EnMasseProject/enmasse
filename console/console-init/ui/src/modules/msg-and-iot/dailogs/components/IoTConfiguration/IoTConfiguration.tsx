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
  DropdownPosition,
  Flex,
  FlexItem,
  FlexModifiers,
  Split,
  SplitItem
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";
import { DropdownWithToggle, IDropdownOption } from "components";
import { ToggleOnIcon, ToggleOffIcon, IconSize } from "@patternfly/react-icons";
import ColorType from "@storybook/addon-knobs/dist/components/types/Color";

export const dropdown_item_styles = StyleSheet.create({
  format_item: { whiteSpace: "normal", textAlign: "justify" },
  dropdown_align: { display: "flex" },
  dropdown_toggle_align: { flex: "1" },
  dropdown_item: { fontWeight: "bold" }
});

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
  console.log(namespaceOptions);
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
              <br />
              <DropdownWithToggle
                id="cas-dropdown-namespace"
                className={css(dropdown_item_styles.dropdown_align)}
                toggleClass={css(dropdown_item_styles.dropdown_toggle_align)}
                dropdownItemClass={dropdown_item_styles.dropdown_item}
                position={DropdownPosition.left}
                onSelectItem={onNameSpaceSelect}
                dropdownItems={namespaceOptions}
                value={namespace}
              />
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
                      color="var(--pf-global--active-color--100)"
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
