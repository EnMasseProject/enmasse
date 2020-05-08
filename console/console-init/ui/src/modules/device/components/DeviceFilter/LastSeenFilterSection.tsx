import React from "react";
import {
  TextInput,
  InputGroup,
  TextContent,
  DropdownPosition
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";
import { IDeviceFilter } from "./DeviceFilter";
import { DropdownWithToggle } from "components";
import { ISelectOption } from "utils";

const styles = StyleSheet.create({
  time_input_box: {
    padding: 20,
    marginRight: 10
  },
  dropdown_align: { display: "flex", marginRight: 10 },
  dropdown_toggle_align: { flex: "1" }
});

interface ILastSeenFilterSectionProps {
  filter: IDeviceFilter;
  setFilter: (filter: IDeviceFilter) => void;
}
const LastSeenFilterSection: React.FunctionComponent<ILastSeenFilterSectionProps> = ({
  filter,
  setFilter
}) => {
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
  const { lastSeen } = filter;
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

  return (
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
  );
};

export { LastSeenFilterSection };
