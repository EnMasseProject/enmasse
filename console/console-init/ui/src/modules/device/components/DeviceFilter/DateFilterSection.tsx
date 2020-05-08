import React from "react";
import {
  TextInput,
  InputGroup,
  TextContent,
  DropdownPosition,
  InputGroupText
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";
import { IDeviceFilter } from "./DeviceFilter";
import { DropdownWithToggle } from "components";
import { ISelectOption } from "utils";
import { CalendarAltIcon } from "@patternfly/react-icons";

const styles = StyleSheet.create({
  time_input_box: {
    padding: 20,
    marginRight: 10
  },
  dropdown_align: { display: "flex", marginRight: 10 },
  dropdown_toggle_align: { flex: "1" }
});

interface IDateFilterSectionProps {
  filter: IDeviceFilter;
  setFilter: (filter: IDeviceFilter) => void;
}
const DateFilterSection: React.FunctionComponent<IDateFilterSectionProps> = ({
  filter,
  setFilter
}) => {
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
  const { addedDate } = filter;
  return (
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
  );
};

export { DateFilterSection };
