/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  Title,
  FormGroup,
  Form,
  Button,
  Chip,
  ChipGroup,
  GridItem,
  Grid,
  Popover,
  DropdownPosition
} from "@patternfly/react-core";
import { InfoCircleIcon } from "@patternfly/react-icons";
import { DropdownWithToggle, TypeAheadSelect } from "components";
import "./pf-overrides.css";
import { gatewayTypeOptions } from "modules/iot-device/utils";
import { mockGatewayDevices, mockGatewayGroups } from "mock-data/iot_device";

export interface IGatewaysProps {
  header?: string;
  gatewayDevices?: string[];
  gatewayGroups?: string[];
  returnGatewayDevices?: (gateways: string[]) => void;
  returnGatewayGroups?: (gatewayGroups: string[]) => void;
}

export const AddGateways: React.FunctionComponent<IGatewaysProps> = ({
  header = "Add permitted devices as gateways to this new device",
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

  return (
    <Grid hasGutter>
      <GridItem span={8}>
        <Title id="add-gateway-device-info-title" headingLevel="h3" size="2xl">
          {header}
        </Title>
        <br />
        <Popover
          bodyContent={
            <div>
              In AMQ IoT, gateway devices are represented in Hono in the same
              way as any other devices.
            </div>
          }
          aria-label="Add gateway devices info popover"
          closeBtnAriaLabel="Close Gateway Devices info popover"
        >
          <Button
            variant="link"
            id="add-gateway-info-popover-button"
            icon={<InfoCircleIcon />}
          >
            How AMQ IoT handles gateways?
          </Button>
        </Popover>
        <br />
        <br />
        <Form>
          <FormGroup
            label="Device ID of gateway"
            id="add-gateway-form-group"
            isRequired
            helperTextInvalid="Invalid device ID"
            fieldId="add-gateway-device-id-input"
            validated="default"
          >
            <Grid>
              <GridItem span={2}>
                <DropdownWithToggle
                  id="add-gateway-type-dropdown"
                  toggleId="add-gateway-type-dropdown-toggle"
                  position={DropdownPosition.left}
                  dropdownItems={gatewayTypeOptions}
                  onSelectItem={onTypeSelect}
                  value={inputType}
                  isLabelAndValueNotSame={true}
                />
              </GridItem>
              <GridItem span={9}>
                <TypeAheadSelect
                  id="add-gateway-typeahead"
                  aria-label="Input gateway"
                  aria-describedby="Input gateway"
                  onSelect={onSelectGateway}
                  onClear={clearGatewaySelections}
                  selected={gatewaySelections}
                  typeAheadAriaLabel={"Typeahead to select gateway devices"}
                  isMultiple={true}
                  onChangeInput={onChangeDeviceIdInput}
                />
              </GridItem>
              <GridItem span={1}>
                <Button
                  id="add-gateway-button"
                  isDisabled={isDisableAddGatewayButton()}
                  variant="secondary"
                  onClick={addGateway}
                >
                  Add
                </Button>
              </GridItem>
            </Grid>
          </FormGroup>
        </Form>
        <br />
        {gatewayDevices.length > 0 && (
          <>
            <Title id="add-gateway-id-list-title" headingLevel="h6" size="md">
              Selected gateway devices
            </Title>
            <br />
            <ChipGroup id="add-gateway-remove-chipgroup">
              {gatewayDevices.map((deviceId: string) => (
                <Chip
                  key={deviceId}
                  id={`add-gateway-remove-chip-${deviceId}`}
                  value={deviceId}
                  onClick={() => removeDevice(deviceId)}
                >
                  {deviceId}
                </Chip>
              ))}
            </ChipGroup>
          </>
        )}
        <br />
        <br />
        {gatewayGroups.length > 0 && (
          <>
            <Title
              id="add-gateway-group-list-title"
              headingLevel="h6"
              size="md"
            >
              Selected gateway groups
            </Title>
            <br />
            <ChipGroup id="add-gateway-group-chip-group">
              {gatewayGroups.map((group: string) => (
                <Chip
                  key={group}
                  id={`add-gateway-group-remove-chip-${group}`}
                  value={group}
                  onClick={() => removeGatewayGroup(group)}
                >
                  {group}
                </Chip>
              ))}
            </ChipGroup>
          </>
        )}
      </GridItem>
    </Grid>
  );
};
