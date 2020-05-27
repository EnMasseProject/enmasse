/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
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
  KebabToggle,
  Dropdown
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";
import { DropdownWithToggle } from "components";
import { compareObject, createDeepCopy } from "utils";
import { IDeviceFilterCriteria } from "modules/device";
import { AddCriteria } from "./AddCriteria";
import { LastSeenFilterSection } from "./LastSeenFilterSection";
import { DateFilterSection } from "./DateFilterSection";
import {
  deviceTypeOptions,
  deviceStatusOptions,
  getInitialFilter
} from "modules/device/utils";

const styles = StyleSheet.create({
  time_input_box: {
    padding: 20,
    marginRight: 10
  },
  dropdown_align: { display: "flex", marginRight: 10 },
  dropdown_toggle_align: { flex: "1" }
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
  filterCriteria: IDeviceFilterCriteria[];
}

export interface IDeviceFilterProps {
  filter?: IDeviceFilter;
  setFilter?: (filter: IDeviceFilter) => void;
  runFilter?: (filter: IDeviceFilter) => void;
}

const DeviceFilter: React.FunctionComponent<IDeviceFilterProps> = ({
  // filter,
  // setFilter,
  runFilter
}) => {
  const [filter, setFilter] = useState<IDeviceFilter>(getInitialFilter());
  const [lastAppliedFilter, setLastAppliedFilter] = useState<IDeviceFilter[]>([
    getInitialFilter()
  ]);
  const [isKebabOpen, setIsKebabOpen] = useState<boolean>(false);
  const [isRedoEnabled, setIsRedoEnabled] = useState<boolean>(false);
  const onClearFilter = () => {
    setFilter(getInitialFilter());
    setIsKebabOpen(false);
  };
  const onRedoFilter = () => {
    // redoFilter();
    const lastFilterLength = lastAppliedFilter.length;
    const lastFilter = createDeepCopy({
      ...lastAppliedFilter[lastFilterLength - 2]
    });
    setFilter(lastFilter);
    const filterList = [...lastAppliedFilter];
    filterList.splice(lastFilterLength - 2, 1);
    setIsRedoEnabled(false);
    setLastAppliedFilter(filterList);
    setIsKebabOpen(false);
  };
  const onChangeDeviceId = (value: string) => {
    const filterObj = { ...filter };
    filterObj.deviceId = value;
    setFilter(filterObj);
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

  const onKebabToggle = () => {
    setIsKebabOpen(!isKebabOpen);
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

  const kebabDropdownItems = [
    <DropdownItem
      key="redo-last-filter"
      id="dropdown-item-redo-last-filter"
      isDisabled={!isRedoEnabled}
      onClick={onRedoFilter}
    >
      Redo last filter
    </DropdownItem>,
    <DropdownItem
      key="clear-all-filter"
      id="dropdown-item-clear-all-filter"
      component="button"
      isDisabled={isEnabledRunFilter()}
      onClick={onClearFilter}
    >
      Clear all
    </DropdownItem>
  ];
  const { deviceId, deviceType, status } = filter;

  const FilterActions = () => (
    <Split>
      <SplitItem>
        <Button
          id="device-filter-btn-run-filter"
          variant={ButtonVariant.secondary}
          onClick={onRunFilter}
          isDisabled={isEnabledRunFilter()}
        >
          Run Filter
        </Button>
      </SplitItem>
      <SplitItem>&nbsp;</SplitItem>
      <SplitItem>
        <Dropdown
          id="filter-kebab-dropdown"
          position={DropdownPosition.left}
          isPlain
          dropdownItems={kebabDropdownItems}
          isOpen={isKebabOpen}
          toggle={
            <KebabToggle id="filter-kebab-toggle" onToggle={onKebabToggle} />
          }
        />
      </SplitItem>
    </Split>
  );

  return (
    <>
      <Form>
        <FormGroup label="Device ID" fieldId="filter-device-id">
          <TextInput
            isRequired
            type="text"
            id="device-filter-text-input-device-id"
            name="device-id"
            aria-describedby="Device Id for filter"
            value={deviceId}
            onChange={onChangeDeviceId}
          />
        </FormGroup>
        <FormGroup label="Device Type" fieldId="filter-device-type">
          <DropdownWithToggle
            id={"device-filter-dropdown-device-type"}
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
        <FormGroup label="Status" fieldId="filter-device-status">
          <DropdownWithToggle
            id={"device-filter-dropdown-device-status"}
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
