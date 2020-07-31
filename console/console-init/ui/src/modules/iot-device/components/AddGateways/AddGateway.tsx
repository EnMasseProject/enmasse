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
  const [selectedGroups, setSelectedGroups] = useState<any[]>([]);
  const [selectedIDs, setSelectedIDs] = useState<any[]>([]);

  useEffect(() => {
    returnGatewayDevices && returnGatewayDevices(gatewayDevices);
  }, [gatewayDevices]);

  useEffect(() => {
    returnGatewayGroups && returnGatewayGroups(gatewayGroups);
  }, [gatewayGroups]);

  const addGateway = async () => {
    if (selectedIDs?.length) {
      const gatewayIDs: string[] = [];

      selectedIDs.forEach(device => {
        !gatewayDevices.includes(device) && gatewayIDs.push(device);
      });

      setGatewayDevices([...gatewayDevices, ...gatewayIDs]);
      setSelectedIDs([]);
    }
    if (selectedGroups?.length) {
      const groupNames: string[] = [];

      selectedGroups.forEach(group => {
        !gatewayGroups.includes(group) && groupNames.push(group);
      });

      setGatewayGroups([...gatewayGroups, ...groupNames]);
      setSelectedGroups([]);
    }
  };

  const removeDevice = (id: string) => {
    const idIndex = gatewayDevices.indexOf(id);
    idIndex >= 0 && gatewayDevices.splice(idIndex, 1);
    setGatewayDevices([...gatewayDevices]);
  };

  const removeGatewayGroup = (group: string) => {
    const idIndex = gatewayGroups.indexOf(group);
    idIndex >= 0 && gatewayGroups.splice(idIndex, 1);
    setGatewayGroups([...gatewayGroups]);
  };

  const onTypeSelect = (val: string) => {
    setInputType(val);
    setSelectedGroups([]);
    setSelectedIDs([]);
  };

  const isDisableAddGatewayButton = () => {
    if (
      (inputType === "gateway_group" && selectedGroups?.length > 0) ||
      (inputType === "device_id" && selectedIDs?.length > 0)
    ) {
      return false;
    }
    return true;
  };

  const onSelectGroup = (_: any, selection: any) => {
    if (selectedGroups.includes(selection)) {
      setSelectedGroups(selectedGroups.filter(item => item !== selection));
    } else {
      setSelectedGroups([...selectedGroups, selection]);
    }
  };

  const onSelectID = (_: any, selection: any) => {
    if (selectedIDs.includes(selection)) {
      setSelectedIDs(selectedIDs.filter(item => item !== selection));
    } else {
      setSelectedIDs([...selectedIDs, selection]);
    }
  };

  const onChangeDeviceIdInput = async (value: string) => {
    // TODO: Use the backend query to populate opions in TypeAhead.
    return [
      {
        id: "id-juno:57966",
        isDisabled: false,
        key: "key-juno:57966",
        value: "juno:57966"
      },
      {
        id: "device-1",
        isDisabled: false,
        key: "device-1",
        value: "device-1"
      },
      {
        id: "device-2",
        isDisabled: false,
        key: "device-2",
        value: "device-2"
      },
      {
        id: "device-3",
        isDisabled: false,
        key: "device-3",
        value: "device-3"
      },
      {
        id: "device-4",
        isDisabled: false,
        key: "device-4",
        value: "device-4"
      },
      {
        id: "device-4",
        isDisabled: false,
        key: "device-4",
        value: "device-4"
      },
      {
        id: "device-5",
        isDisabled: false,
        key: "device-5",
        value: "device-5"
      },
      {
        id: "device-5",
        isDisabled: false,
        key: "device-5",
        value: "device-5"
      }
    ];
  };

  const onChangeGroupInput = async (value: string) => {
    // TODO: Use the backend query to populate opions in TypeAhead.
    return [
      {
        id: "id-juno:57966",
        isDisabled: false,
        key: "key-juno:57966",
        value: "juno:57966"
      },
      {
        id: "group-1",
        isDisabled: false,
        key: "group-1",
        value: "group-1"
      },
      {
        id: "group-2",
        isDisabled: false,
        key: "group-2",
        value: "group-2"
      }
    ];
  };

  const clearGroupSelection = () => {
    setSelectedGroups([]);
  };

  const clearDeviceSelection = () => {
    setSelectedIDs([]);
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
                {inputType === "device_id" ? (
                  <TypeAheadSelect
                    id="add-gateway-deviceid-typeahead"
                    aria-label="Input device ID"
                    aria-describedby="Input device ID"
                    onSelect={onSelectID}
                    onClear={clearDeviceSelection}
                    selected={selectedIDs}
                    typeAheadAriaLabel={"Typeahead to select gateway devices"}
                    isMultiple={true}
                    onChangeInput={onChangeDeviceIdInput}
                  />
                ) : (
                  <TypeAheadSelect
                    id="add-gateway-group-typeahead"
                    onSelect={onSelectGroup}
                    aria-label="Input group name"
                    aria-describedby="Input group name"
                    onClear={clearGroupSelection}
                    selected={selectedGroups}
                    typeAheadAriaLabel={"Typeahead to select gateway groups"}
                    isMultiple={true}
                    onChangeInput={onChangeGroupInput}
                  />
                )}
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
