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
  InputGroup,
  InputGroupText,
  TextContent,
  Divider,
  Grid,
  GridItem,
  Button,
  ButtonVariant,
  Split,
  SplitItem,
  DropdownItem,
  KebabToggle,
  Dropdown
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";
import { CalendarAltIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { DropdownWithToggle } from "components";
import { ISelectOption, uniqueId, findIndexByProperty } from "utils";
import {
  IDeviceFilterCriteria,
  DeviceFilterCriteria,
  operator
} from "modules/device";

const styles = StyleSheet.create({
  time_input_box: {
    padding: 20,
    marginRight: 10
  },
  dropdown_align: { display: "flex", marginRight: 10 },
  dropdown_toggle_align: { flex: "1" },
  margin_Left_20: { marginLeft: 20 }
});

const setInitialFilter = () => {
  let filter: IDeviceFilter = {
    deviceId: "",
    deviceType: "",
    status: "",
    filterCriteria: [],
    addedDate: {
      startDate: "",
      endDate: ""
    },
    lastSeen: {
      startTime: {
        form: "hr",
        time: ""
      },
      endTime: {
        form: "hr",
        time: ""
      }
    }
  };
  return filter;
};

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
}

const DeviceFilter: React.FunctionComponent<IDeviceFilterProps> = (
  {
    // filter,
    // setFilter
  }
) => {
  const [filter, setFilter] = useState<IDeviceFilter>(setInitialFilter());
  const [lastAppliedFilter, setLastAppliedFilter] = useState<IDeviceFilter[]>([
    setInitialFilter()
  ]);
  const [isKebabOpen, setIsKebabOpen] = useState<boolean>(false);
  const typeOptions: ISelectOption[] = [
    {
      key: "direct",
      value: "direct",
      label: "Directly connected"
    },
    {
      key: "gateway",
      value: "gateway",
      label: "Using gateways"
    }
  ];
  const statusOptions: ISelectOption[] = [
    {
      key: "enabled",
      value: "enabled",
      label: "Enabled"
    },
    {
      key: "disabled",
      value: "disabled",
      label: "Disabled"
    }
  ];
  const timeOptions: ISelectOption[] = [
    {
      key: "hr",
      value: "hr",
      label: "hr"
    },
    {
      key: "min",
      value: "min",
      label: "min"
    },
    {
      key: "sec",
      value: "sec",
      label: "sec"
    }
  ];
  const onClearFilter = () => {
    setFilter(setInitialFilter());
    setIsKebabOpen(false);
  };
  const onRedoFilter = () => {
    // redoFilter();
    const length = lastAppliedFilter.length;
    const data = JSON.parse(
      JSON.stringify({ ...lastAppliedFilter[length - 2] })
    );
    setFilter(data);
    const dataList = [...lastAppliedFilter];
    dataList.splice(length - 2, 1);
    setLastAppliedFilter(dataList);
    setIsKebabOpen(false);
  };
  const setTime = (
    value: string,
    propertyName: "startTime" | "endTime",
    isFormat: boolean
  ) => {
    const filterObj = JSON.parse(JSON.stringify(filter));
    let timeObj = filter.lastSeen[propertyName];
    isFormat
      ? (timeObj = { form: value, time: timeObj.time })
      : (timeObj = { form: timeObj.form, time: value });
    filterObj.lastSeen[propertyName] = timeObj;
    setFilter(filterObj);
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

  const addCriteria = () => {
    const filterObj = JSON.parse(JSON.stringify(filter));
    const list = filterObj.filterCriteria;
    list.push({
      operator: operator.EQ,
      parameter: "",
      value: "",
      key: uniqueId()
    });
    filterObj.filterCriteria = list;
    setFilter(filterObj);
  };

  const deleteCriteria = (criteria: IDeviceFilterCriteria) => {
    const filterObj = { ...filter };
    const list = filterObj.filterCriteria;
    const index = findIndexByProperty(list, "key", criteria.key);
    list.splice(index, 1);
    filterObj.filterCriteria = list;
    setFilter(filterObj);
  };

  const updateCriteria = (criteria: IDeviceFilterCriteria) => {
    const filterObj = JSON.parse(JSON.stringify(filter));
    const list = filterObj.filterCriteria;
    const index = findIndexByProperty(list, "key", criteria.key);
    list[index] = criteria;
    filterObj.filterCriteria = list;
    setFilter(filterObj);
  };
  const onStartTimeChange = (value: string) => {
    setTime(value, "startTime", false);
  };
  const onStartTimeFormatChange = (value: string) => {
    setTime(value, "startTime", true);
  };
  const onEndTimeChange = (value: string) => {
    setTime(value, "endTime", false);
  };
  const onEndTimeFormatChange = (value: string) => {
    setTime(value, "endTime", true);
  };
  const onChangeStartDate = (value: string) => {
    const filterObj = { ...filter };
    filterObj.addedDate.startDate = value;
    setFilter(filterObj);
  };
  const onChangeEndDate = (value: string) => {
    const filterObj = { ...filter };
    filterObj.addedDate.endDate = value;
    setFilter(filterObj);
  };
  const onKebabToggle = () => {
    setIsKebabOpen(!isKebabOpen);
  };

  const onRunFilter = () => {
    const dataList = JSON.parse(JSON.stringify(lastAppliedFilter));
    const dataObj = JSON.parse(JSON.stringify(filter));
    dataList.push(dataObj);
    setLastAppliedFilter(dataList);
  };
  const isEnabledRunFilter = () => {
    if (JSON.stringify(filter) != JSON.stringify(setInitialFilter())) {
      return false;
    }
    return true;
  };

  const kebabDropdownItems = [
    <DropdownItem
      key="redo-last-filter"
      id="redo-last-filter"
      isDisabled={lastAppliedFilter.length <= 1}
      onClick={onRedoFilter}
    >
      Redo last filter
    </DropdownItem>,
    <DropdownItem
      key="clear-all-filter"
      id="clear-all-filter"
      component="button"
      onClick={onClearFilter}
    >
      Clear all
    </DropdownItem>
  ];
  const {
    lastSeen,
    addedDate,
    deviceId,
    deviceType,
    status,
    filterCriteria
  } = filter;

  const DeviceId = () => (
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
  );
  const DeviceType = () => (
    <FormGroup label="Device Type" fieldId="filter-device-type">
      <DropdownWithToggle
        id={"device-filter-dropdown-device-type"}
        name="Device Type"
        className={css(styles.dropdown_align)}
        toggleClass={css(styles.dropdown_toggle_align)}
        position={DropdownPosition.left}
        onSelectItem={onTypeSelect}
        dropdownItems={typeOptions}
        value={deviceType}
        isLabelAndValueNotSame={true}
      />
    </FormGroup>
  );
  const DeviceStatus = () => (
    <FormGroup label="Status" fieldId="filter-device-status">
      <DropdownWithToggle
        id={"device-filter-dropdown-device-status"}
        name="Status"
        className={css(styles.dropdown_align)}
        toggleClass={css(styles.dropdown_toggle_align)}
        position={DropdownPosition.left}
        onSelectItem={onStatusSelect}
        dropdownItems={statusOptions}
        value={status}
        isLabelAndValueNotSame={true}
      />
    </FormGroup>
  );
  const LastSeen = () => (
    <FormGroup label="Last seen" fieldId="filter-device-last-seen">
      <InputGroup>
        <TextInput
          className={css(styles.time_input_box)}
          name="last-start-time-number"
          id="device-filter-text-input-last-start-time-number"
          type="number"
          aria-describedby="Device Id for filter"
          value={lastSeen.startTime && lastSeen.startTime.time}
          onChange={onStartTimeChange}
        />
        <DropdownWithToggle
          className={css(styles.dropdown_align)}
          toggleClass={css(styles.dropdown_toggle_align)}
          id="device-filter-dropdown-last-seen-start-time-format"
          position={DropdownPosition.left}
          onSelectItem={onStartTimeFormatChange}
          value={lastSeen.startTime ? lastSeen.startTime.form : ""}
          dropdownItems={timeOptions}
          dropdownItemIdPrefix="device-filter-dropdown-item-last-seen-start-time-format"
        />
        <TextContent>{" - "}</TextContent>
        <TextInput
          className={css(styles.time_input_box)}
          name="last-end-time-number"
          id="device-filter-text-input-last-end-time-number"
          type="number"
          aria-describedby="Device Id for filter"
          value={lastSeen.endTime && lastSeen.endTime.time}
          onChange={onEndTimeChange}
        />
        <DropdownWithToggle
          className={css(styles.dropdown_align)}
          id="device-filter-dropdown-last-seen-end-time-format"
          position={DropdownPosition.left}
          onSelectItem={onEndTimeFormatChange}
          value={lastSeen.endTime ? lastSeen.endTime.form : ""}
          dropdownItems={timeOptions}
          dropdownItemIdPrefix="device-filter-dropdown-item-last-seen-end-time-format"
        />
      </InputGroup>
    </FormGroup>
  );
  const AddedDate = () => (
    <FormGroup label="Added date" fieldId="filter-device-added-date">
      <InputGroup>
        <InputGroupText component="label" htmlFor="added-date">
          <CalendarAltIcon />
        </InputGroupText>
        <TextInput
          name="added-start-date"
          id="device-filter-text-input-added-start-date"
          type="date"
          aria-label="Added Start Date"
          value={addedDate.startDate}
          onChange={onChangeStartDate}
        />
        <TextInput
          name="added-end-date"
          id="device-filter-text-input-added-end-date"
          type="date"
          aria-label="Added End Date"
          value={addedDate.endDate}
          onChange={onChangeEndDate}
        />
      </InputGroup>
    </FormGroup>
  );

  const FilterActions = () => (
    <Split>
      <SplitItem>
        <Button
          className={css(styles.margin_Left_20)}
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
  const FilterCriteria = () => (
    <FormGroup label="" fieldId="filter-criteria-paramter">
      {filterCriteria && filterCriteria.length > 0 && (
        <>
          <Grid>
            <GridItem span={5}>
              <FormGroup label="Parameter" fieldId="filter-criteria-paramter" />
            </GridItem>
            <GridItem span={2}>
              <FormGroup label="" fieldId="filter-criteria-operator" />
            </GridItem>
            <GridItem span={5}>
              <FormGroup label="Value" fieldId="filter-criteria-value" />
            </GridItem>
          </Grid>
          {filterCriteria.map((cr: IDeviceFilterCriteria) => (
            <DeviceFilterCriteria
              criteria={cr}
              deleteCriteria={deleteCriteria}
              setCriteria={updateCriteria}
            />
          ))}
        </>
      )}
      <Button
        variant="link"
        id="device-filter-btn-add-criteria"
        icon={<PlusCircleIcon />}
        onClick={addCriteria}
      >
        Add criteria
      </Button>
      <br />
      {filterCriteria &&
        filterCriteria.length === 0 &&
        "To fitler across millions of devices, please add criteria to narrow down"}
    </FormGroup>
  );

  return (
    <>
      <Form>
        <DeviceId />
        <DeviceType />
        <DeviceStatus />
        <LastSeen />
        <AddedDate />
        <Divider />
        <FilterCriteria />
        <Divider />
        <FilterActions />
      </Form>
    </>
  );
};

export { DeviceFilter };
