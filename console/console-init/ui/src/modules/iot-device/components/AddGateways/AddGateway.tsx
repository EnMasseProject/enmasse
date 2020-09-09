/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  FormGroup,
  Form,
  Button,
  DropdownPosition,
  Grid,
  GridItem,
  Split,
  SplitItem
} from "@patternfly/react-core";
import { DropdownWithToggle, TypeAheadSelect } from "components";
import "./pf-overrides.css";
import { gatewayTypeOptions } from "modules/iot-device/utils";
import { mockGatewayDevices, mockGatewayGroups } from "mock-data/iot_device";
import { StyleSheet, css } from "aphrodite";
import { ChipGroupsWithTitle } from "components";

const styles = StyleSheet.create({
  dropdown_min_width: {
    "min-width": "12rem",
    width: "100%"
  }
});

export interface IGatewaysProps {
  header?: string;
  gatewayDevices?: string[];
  gatewayGroups?: string[];
  returnGatewayDevices?: (gateways: string[]) => void;
  returnGatewayGroups?: (gatewayGroups: string[]) => void;
}

export const AddGateways: React.FunctionComponent<IGatewaysProps> = ({
  gatewayDevices: deviceList = [],
  gatewayGroups: groupList = [],
  returnGatewayDevices,
  returnGatewayGroups
}) => {
  const [gatewayDevices, setGatewayDevices] = useState<string[]>(deviceList);
  const [gatewayGroups, setGatewayGroups] = useState<string[]>(groupList);
  const [inputType, setInputType] = useState<string>("device_id");
  const [gatewaySelections, setGatewaySelections] = useState<string[]>([]);

  useEffect(() => {
    returnGatewayDevices && returnGatewayDevices(gatewayDevices);
  }, [gatewayDevices]);

  useEffect(() => {
    returnGatewayGroups && returnGatewayGroups(gatewayGroups);
  }, [gatewayGroups]);

  const addGateway = () => {
    let newSelections: string[];
    if (inputType === "device_id" && gatewaySelections?.length > 0) {
      newSelections = Array.from(
        new Set(gatewayDevices?.concat(gatewaySelections))
      );
      setGatewayDevices(newSelections);
    } else if (inputType === "gateway_group" && gatewaySelections?.length > 0) {
      newSelections = Array.from(
        new Set(gatewayGroups?.concat(gatewaySelections))
      );
      setGatewayGroups(newSelections);
    }
    setGatewaySelections([]);
  };

  const removeDevice = (id: string) => {
    const idIndex = gatewayDevices.indexOf(id);
    if (idIndex >= 0) {
      gatewayDevices.splice(idIndex, 1);
      setGatewayDevices([...gatewayDevices]);
    }
  };

  const removeGatewayGroup = (group: string) => {
    const idIndex = gatewayGroups.indexOf(group);
    if (idIndex >= 0) {
      gatewayGroups.splice(idIndex, 1);
      setGatewayGroups([...gatewayGroups]);
    }
  };

  const onTypeSelect = (val: string) => {
    setInputType(val);
    setGatewaySelections([]);
  };

  const isDisableAddGatewayButton = () => {
    if (gatewaySelections?.length > 0) {
      return false;
    }
    return true;
  };

  const onSelectGateway = (_: any, selection: any) => {
    if (gatewaySelections.includes(selection)) {
      setGatewaySelections(
        gatewaySelections.filter(item => item !== selection)
      );
    } else {
      setGatewaySelections([...gatewaySelections, selection]);
    }
  };

  const onChangeDeviceIdInput = async (value: string) => {
    // TODO: Use the backend query to populate opions in TypeAhead.
    if (inputType === "device_id") return mockGatewayDevices;
    else return mockGatewayGroups;
  };

  const clearGatewaySelections = () => {
    setGatewaySelections([]);
  };

  const getPlaceholderText = () => {
    let placeholder =
      inputType === "device_id"
        ? "Input gateway device ID"
        : "Input gateway group name";
    return placeholder;
  };

  return (
    <Grid>
      <GridItem span={6}>
        <Form>
          <FormGroup
            label="gateway device id or gateway group name"
            id="add-gateway-form-group"
            isRequired
            helperTextInvalid="Invalid device ID"
            fieldId="add-gateway-device-id-input"
            validated="default"
          >
            <Split hasGutter>
              <SplitItem>
                <DropdownWithToggle
                  id="add-gateway-type-dropdown"
                  toggleId="add-gateway-type-dropdown-toggle"
                  position={DropdownPosition.left}
                  dropdownItems={gatewayTypeOptions}
                  onSelectItem={onTypeSelect}
                  value={inputType}
                  isLabelAndValueNotSame={true}
                  toggleClass={css(styles.dropdown_min_width)}
                />
              </SplitItem>
              <SplitItem isFilled>
                <TypeAheadSelect
                  threshold={1}
                  id="add-gateway-typeahead"
                  aria-label="Input gateway"
                  aria-describedby="Input gateway"
                  onSelect={onSelectGateway}
                  onClear={clearGatewaySelections}
                  selected={gatewaySelections}
                  typeAheadAriaLabel={"Typeahead to select gateway devices"}
                  isMultiple={true}
                  onChangeInput={onChangeDeviceIdInput}
                  placeholderText={getPlaceholderText()}
                />
              </SplitItem>
              <SplitItem>
                <Button
                  id="add-gateway-button"
                  isDisabled={isDisableAddGatewayButton()}
                  variant="secondary"
                  onClick={addGateway}
                >
                  Add
                </Button>
              </SplitItem>
            </Split>
          </FormGroup>
        </Form>
        <br />
        <br />
        <ChipGroupsWithTitle
          id="gateway-device-chipgroups"
          titleId="gateway-device-title"
          title={"Selected gateway devices"}
          items={gatewayDevices}
          removeItem={removeDevice}
        />
        <br />
        <br />
        <ChipGroupsWithTitle
          id="gateway-groups-chipgroups"
          titleId="gateway-groups-title"
          title={"Selected gateway groups"}
          items={gatewayGroups}
          removeItem={removeGatewayGroup}
        />
      </GridItem>
    </Grid>
  );
};
