/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  Form,
  FormGroup,
  TextInput,
  DropdownPosition,
  Divider,
  Button,
  ButtonVariant,
  Split,
  SplitItem,
  DropdownItem,
  Grid,
  GridItem,
  ChipGroup,
  Chip
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { DropdownWithToggle, DropdownWithKebabToggle } from "components";
import { compareObject, createDeepCopy } from "utils";
import { IDeviceFilterCriteria } from "modules/iot-device";
import { AddCriteria } from "./AddCriteria";
import { LastSeenFilterSection } from "./LastSeenFilterSection";
import { DateFilterSection } from "./DateFilterSection";
import { GatewayGroupTypeAheadSelect } from "containers";
import {
  deviceTypeOptions,
  deviceStatusOptions,
  getInitialFilter,
  deviceGatewayConnectionOptions
} from "modules/iot-device/utils";

const styles = StyleSheet.create({
  time_input_box: {
    padding: 20
  },
  dropdown_align: { display: "flex" },
  dropdown_toggle_align: { flex: "1" },
  grid_margin: { marginLeft: 10 }
});

export interface ITimeOption {
  time: string;
  form: string;
}

export interface IDeviceFilter {
  deviceId: string;
  deviceType: string;
  status: string;
  lastSeen: {
    startTime: ITimeOption;
    endTime: ITimeOption;
  };
  addedDate: {
    startDate: string;
    endDate: string;
  };
  gatewayGroups: string[];
  gatewayConnections: string[];
  filterCriteria: IDeviceFilterCriteria[];
}

export interface IDeviceFilterProps {
  runFilter?: (filter: IDeviceFilter) => void;
  resetFilter?: () => void;
}

const DeviceFilter: React.FunctionComponent<IDeviceFilterProps> = ({
  runFilter,
  resetFilter
}) => {
  const [filter, setFilter] = useState<IDeviceFilter>(getInitialFilter());
  const [lastAppliedFilter, setLastAppliedFilter] = useState<IDeviceFilter[]>([
    getInitialFilter()
  ]);
  const [isKebabOpen, setIsKebabOpen] = useState<boolean>(false);
  const [isRedoEnabled, setIsRedoEnabled] = useState<boolean>(false);
  const [gatewayGroupConnection, setGatewayGroupConnection] = useState<string>(
    deviceGatewayConnectionOptions[0].value
  );
  const [gatewayInput, setGatewayInput] = useState<string>("");

  const onClearFilter = () => {
    resetFilter && resetFilter();
    setIsKebabOpen(false);
  };

  const onRedoFilter = () => {
    const lastFilterLength = lastAppliedFilter.length;
    const lastFilter = createDeepCopy({
      ...lastAppliedFilter[lastFilterLength - 2]
    });
    setFilter(lastFilter);
    const filterList = [...lastAppliedFilter];
    filterList.splice(lastFilterLength - 2, 1);
    setIsRedoEnabled(false);
    setLastAppliedFilter(filterList);
    runFilter && runFilter(lastFilter);
  };
  const onChangeDeviceId = (value: string) => {
    const filterObj = { ...filter };
    filterObj.deviceId = value;
    setFilter(filterObj);
  };

  const setSelectedGatewayGroups = (connections: string[]) => {
    const filterObj = { ...filter };
    filterObj.gatewayGroups = connections;
    setFilter(filterObj);
  };

  const onSelectGatewayGroup = (_: any, selection: any) => {
    const { gatewayGroups } = filter;
    if (gatewayGroups?.includes(selection)) {
      setSelectedGatewayGroups(
        gatewayGroups.filter((item: string) => item !== selection)
      );
    } else {
      setSelectedGatewayGroups([...gatewayGroups, selection]);
    }
  };
  const onTypeSelect = (value: string) => {
    const filterObj = { ...filter };
    filterObj.deviceType = value;
    setFilter(filterObj);
  };

  const onStatusSelect = (value: string) => {
    const filterObj = { ...filter };
    filterObj.status = value;
    setFilter(filterObj);
  };

  const onRunFilter = () => {
    const lastFilter = createDeepCopy(lastAppliedFilter);
    const filterCopy = createDeepCopy(filter);
    lastFilter.push(filterCopy);
    setIsRedoEnabled(true);
    setLastAppliedFilter(lastFilter);
    runFilter && runFilter(filter);
  };

  const isEnabledRunFilter = () => {
    return compareObject(Object.assign({}, filter), getInitialFilter());
  };
  const onClear = () => {
    setSelectedGatewayGroups([]);
  };

  const onChangeGatewayConnection = (value: string) => {
    setGatewayInput(value);
  };

  const onGatewayConnectionSelect = (value: string) => {
    setGatewayGroupConnection(value);
  };

  const setSelectedGroupConnections = (connections: string[]) => {
    const filterObj = { ...filter };
    filterObj.gatewayConnections = connections;
    setFilter(filterObj);
  };

  const deleteGatewayConnection = (chip?: string) => {
    const { gatewayConnections } = filter;
    if (gatewayConnections?.length > 0 && chip && chip.trim() !== "") {
      let index = gatewayConnections.indexOf(chip.toString());
      if (index >= 0) gatewayConnections.splice(index, 1);
      setSelectedGroupConnections([...gatewayConnections]);
    }
  };

  const addGatewayConnection = () => {
    const { gatewayConnections } = filter;
    if (gatewayInput && gatewayInput.trim() !== "") {
      setSelectedGroupConnections([...gatewayConnections, gatewayInput]);
      setGatewayInput("");
    }
  };

  const kebabDropdownItems = [
    <DropdownItem
      key="redo-last-filter"
      id="device-filter-redo-last-dropdownitem"
      isDisabled={!isRedoEnabled}
      onClick={onRedoFilter}
    >
      Redo last filter
    </DropdownItem>,
    <DropdownItem
      key="clear-all-filter"
      id="device-filter-clear-all-dropdownitem"
      component="button"
      isDisabled={isEnabledRunFilter()}
      onClick={onClearFilter}
    >
      Clear all
    </DropdownItem>
  ];
  const {
    deviceId,
    deviceType,
    status,
    gatewayGroups,
    gatewayConnections
  } = filter;

  const FilterActions = () => (
    <Split>
      <SplitItem>
        <Button
          id="device-filter-run-filter-button"
          variant={ButtonVariant.secondary}
          onClick={onRunFilter}
          isDisabled={isEnabledRunFilter()}
        >
          Run Filter
        </Button>
      </SplitItem>
      <SplitItem>&nbsp;</SplitItem>
      <SplitItem>
        <DropdownWithKebabToggle
          id="device-filter-options-kebab-dropdown"
          isPlain={true}
          toggleId="device-filter-options-kebab-dropdowntoggle"
          dropdownItems={kebabDropdownItems}
          isOpen={isKebabOpen}
          position={DropdownPosition.left}
        />
      </SplitItem>
    </Split>
  );

  return (
    <>
      <Form>
        <FormGroup label="Device ID" fieldId="device-filter-id-input">
          <TextInput
            isRequired
            type="text"
            id="device-filter-id-input"
            name="device-id"
            aria-describedby="Device Id for filter"
            value={deviceId}
            onChange={onChangeDeviceId}
          />
        </FormGroup>
        <FormGroup
          label="Device Type"
          fieldId="device-filter-type-dropdowntoggle"
        >
          <DropdownWithToggle
            id={"device-filter-type-dropdown"}
            toggleId="device-filter-type-dropdowntoggle"
            name="Device Type"
            className={css(styles.dropdown_align)}
            toggleClass={css(styles.dropdown_toggle_align)}
            position={DropdownPosition.left}
            onSelectItem={onTypeSelect}
            dropdownItems={deviceTypeOptions}
            value={deviceType}
            isLabelAndValueNotSame={true}
          />
        </FormGroup>
        <FormGroup label="Status" fieldId="device-filter-status-dropdowntoggle">
          <DropdownWithToggle
            id={"device-filter-status-dropdown"}
            toggleId="device-filter-status-dropdowntoggle"
            name="Status"
            className={css(styles.dropdown_align)}
            toggleClass={css(styles.dropdown_toggle_align)}
            position={DropdownPosition.left}
            onSelectItem={onStatusSelect}
            dropdownItems={deviceStatusOptions}
            value={status}
            isLabelAndValueNotSame={true}
          />
        </FormGroup>
        <FormGroup label="Last seen" fieldId="filter-device-last-seen">
          <LastSeenFilterSection filter={filter} setFilter={setFilter} />
        </FormGroup>
        <FormGroup label="Added date" fieldId="filter-device-added-date">
          <DateFilterSection filter={filter} setFilter={setFilter} />
        </FormGroup>
        <FormGroup
          label="Gateway group membership"
          fieldId="device-filter-gateway-membership-group-input"
        >
          <GatewayGroupTypeAheadSelect
            id="add-gateway-group-membership-typeahead-select"
            aria-label="gateway group membership dropdown"
            aria-describedby="multi typeahead for gateway groups membership"
            onSelect={onSelectGatewayGroup}
            onClear={onClear}
            selected={gatewayGroups}
            typeAheadAriaLabel={"typeahead to select gateway group membership"}
            isMultiple={true}
            placeholderText={"Input gateway group name"}
          />
        </FormGroup>

        <FormGroup
          label="Gateway connections"
          fieldId="device-gateway-connection-dropdowntoggle"
        >
          <Grid>
            <GridItem span={4}>
              <DropdownWithToggle
                id={"device-filter-gateway-conn-dropdown"}
                toggleId="device-filter-gateway-conn-dropdowntoggle"
                name="Device Connections"
                className={css(styles.dropdown_align)}
                toggleClass={css(styles.dropdown_toggle_align)}
                position={DropdownPosition.left}
                onSelectItem={onGatewayConnectionSelect}
                dropdownItems={deviceGatewayConnectionOptions}
                value={gatewayGroupConnection}
                isLabelAndValueNotSame={true}
              />
            </GridItem>
            <GridItem span={5} className={css(styles.grid_margin)}>
              <TextInput
                isRequired
                type="text"
                id="device-filter-gateway-connection-input"
                placeholder="Input gateway"
                name="gateway-connection"
                aria-describedby="gateway connection for filter"
                value={gatewayInput}
                onChange={onChangeGatewayConnection}
              />
            </GridItem>
            <GridItem span={3} className={css(styles.grid_margin)}>
              <Button
                variant={ButtonVariant.primary}
                id="device-filter-add-gateway-conn-btn"
                isDisabled={
                  gatewayGroupConnection.trim() === "" ||
                  gatewayInput.trim() === ""
                }
                onClick={addGatewayConnection}
              >
                Add
              </Button>
            </GridItem>
          </Grid>
          <br />
          <ChipGroup>
            {gatewayConnections.map(currentChip => (
              <Chip
                key={currentChip}
                onClick={() => deleteGatewayConnection(currentChip)}
              >
                {currentChip}
              </Chip>
            ))}
          </ChipGroup>
        </FormGroup>
        <Divider />
        <FormGroup label="" fieldId="filter-criteria-paramter">
          <AddCriteria filter={filter} setFilter={setFilter} />
        </FormGroup>
        <Divider />
        <FilterActions />
      </Form>
    </>
  );
};

export { DeviceFilter };
