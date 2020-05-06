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
  GridItem
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
  dropdown_toggle_align: { flex: "1" }
});

const setInitialFilter = () => {
  let filter: IDeviceFilter = {
    deviceId: "",
    deviceType: "gateway",
    status: "enabled",
    filterCriteria: [],
    addedDate: {
      startDate: "",
      endDate: ""
    },
    lastSeen: {
      endTime: {
        form: "hr",
        time: ""
      },
      startTime: {
        form: "min",
        time: ""
      }
    }
  };
  return filter;
};

interface ITimeOption {
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
  const typeOptions: ISelectOption[] = [
    {
      key: "direct",
      value: "direct",
      label: "Directly connected",
      isDisabled: false
    },
    {
      key: "gateway",
      value: "gateway",
      label: "Using gateways",
      isDisabled: false
    }
  ];
  const statusOptions: ISelectOption[] = [
    {
      key: "enabled",
      value: "enabled",
      label: "Enabled",
      isDisabled: false
    },
    {
      key: "disabled",
      value: "disabled",
      label: "Disabled",
      isDisabled: false
    }
  ];
  const timeOptions: ISelectOption[] = [
    {
      key: "hr",
      value: "hr",
      label: "hr",
      isDisabled: false
    },
    {
      key: "min",
      value: "min",
      label: "min",
      isDisabled: false
    },
    {
      key: "sec",
      value: "sec",
      label: "sec",
      isDisabled: false
    }
  ];

  const setTime = (value: string, isStart: boolean, isFormat: boolean) => {
    const filterObj = { ...filter };
    if (isStart) {
      let timeObj = filter.lastSeen.startTime;
      if (isFormat) {
        timeObj = { form: value, time: timeObj.time };
      } else {
        timeObj = { form: timeObj.form, time: value };
      }
      filterObj.lastSeen.startTime = timeObj;
    } else {
      let timeObj = filter.lastSeen.endTime;
      if (isFormat) {
        timeObj = { form: value, time: timeObj.time };
      } else {
        timeObj = { form: timeObj.form, time: value };
      }
      filterObj.lastSeen.endTime = timeObj;
    }
    setFilter(filterObj);
  };

  const onChangeDeviceId = (value: string) => {
    // const deviceId = event.target.value;
    const filterObj = { ...filter };
    if (value != filterObj.deviceId) {
      filterObj.deviceId = value;
      setFilter(filterObj);
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

  const addCriteria = () => {
    const filterObj = { ...filter };
    const list = filterObj.filterCriteria;
    list.push({
      operator: operator.eq,
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
    const filterObj = { ...filter };
    const list = filterObj.filterCriteria;
    const index = findIndexByProperty(list, "key", criteria.key);
    list[index] = criteria;
    filterObj.filterCriteria = list;
    setFilter(filterObj);
  };
  const onStartTimeChange = (value: string) => {
    setTime(value, true, false);
  };
  const onStartTimeFormatChange = (value: string) => {
    setTime(value, true, true);
  };
  const onEndTimeChange = (value: string) => {
    setTime(value, false, false);
  };
  const onEndTimeFormatChange = (value: string) => {
    setTime(value, false, true);
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
  const {
    lastSeen,
    addedDate,
    deviceId,
    deviceType,
    status,
    filterCriteria
  } = filter;
  return (
    <>
      <Form>
        <FormGroup label="Device ID" fieldId="filter-device-id">
          <TextInput
            isRequired
            type="text"
            id="device-id-filter-input"
            name="device-id"
            aria-describedby="Device Id for filter"
            value={deviceId}
            onChange={onChangeDeviceId}
          />
        </FormGroup>
        <FormGroup label="Device Type" fieldId="filter-device-type">
          <DropdownWithToggle
            id={"device-type"}
            name="Device Type"
            className={css(styles.dropdown_align)}
            toggleClass={css(styles.dropdown_toggle_align)}
            position={DropdownPosition.left}
            onSelectItem={value => onTypeSelect(value)}
            dropdownItems={typeOptions}
            value={deviceType}
            isLabelAndValueNotSame={true}
          />
        </FormGroup>
        <FormGroup label="Status" fieldId="filter-device-status">
          <DropdownWithToggle
            id={"device-status"}
            name="Status"
            className={css(styles.dropdown_align)}
            toggleClass={css(styles.dropdown_toggle_align)}
            position={DropdownPosition.left}
            onSelectItem={value => onStatusSelect(value)}
            dropdownItems={statusOptions}
            value={status}
            isLabelAndValueNotSame={true}
          />
        </FormGroup>

        <FormGroup label="Last seen" fieldId="filter-device-last-seen">
          <InputGroup>
            <TextInput
              className={css(styles.time_input_box)}
              name="last-start-time-number"
              id="last-start-time-number"
              type="number"
              aria-describedby="Device Id for filter"
              value={lastSeen.startTime && lastSeen.startTime.time}
              onChange={onStartTimeChange}
            />
            <DropdownWithToggle
              className={css(styles.dropdown_align)}
              toggleClass={css(styles.dropdown_toggle_align)}
              id="last-seen-start-time-format"
              position={DropdownPosition.left}
              onSelectItem={onStartTimeFormatChange}
              value={lastSeen.startTime ? lastSeen.startTime.form : ""}
              dropdownItems={timeOptions}
              dropdownItemIdPrefix="last-seen-start-time-format"
            />
            <TextContent>
              <span style={{ fontSize: 24, margin: 20 }}>{" - "}</span>
            </TextContent>
            <TextInput
              className={css(styles.time_input_box)}
              name="last-end-time-number"
              id="last-end-time-number"
              type="number"
              aria-describedby="Device Id for filter"
              value={lastSeen.endTime && lastSeen.endTime.time}
              onChange={onEndTimeChange}
            />
            <DropdownWithToggle
              className={css(styles.dropdown_align)}
              id="last-seen-end-time-format"
              position={DropdownPosition.left}
              onSelectItem={onEndTimeFormatChange}
              value={lastSeen.endTime ? lastSeen.endTime.form : ""}
              dropdownItems={timeOptions}
              dropdownItemIdPrefix="last-seen-end-time-format"
            />
          </InputGroup>
        </FormGroup>

        <FormGroup label="Added date" fieldId="filter-device-added-date">
          <InputGroup>
            <InputGroupText component="label" htmlFor="added-date">
              <CalendarAltIcon />
            </InputGroupText>
            <TextInput
              name="added-start-date"
              id="added-start-date"
              type="date"
              aria-label="Added Start Date"
              value={addedDate.startDate || "2020-01-01"}
              onChange={onChangeStartDate}
            />
            <TextInput
              name="added-end-date"
              id="added-end-date"
              type="date"
              aria-label="Added End Date"
              value={addedDate.endDate || "2018-06-02"}
              onChange={onChangeEndDate}
            />
          </InputGroup>
        </FormGroup>
        <Divider />
        <FormGroup label="" fieldId="filter-criteria-paramter">
          {filterCriteria && filterCriteria.length > 0 && (
            <>
              <Grid>
                <GridItem span={5}>
                  <FormGroup
                    label="Parameter"
                    fieldId="filter-criteria-paramter"
                  />
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
          <a style={{ padding: 10 }} onClick={addCriteria}>
            <PlusCircleIcon /> Add criteria
          </a>
        </FormGroup>
      </Form>
    </>
  );
};

export { DeviceFilter };
