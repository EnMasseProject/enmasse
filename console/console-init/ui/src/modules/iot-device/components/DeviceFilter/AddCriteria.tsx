import React from "react";
import { FormGroup, Grid, GridItem, Button } from "@patternfly/react-core";
import {
  IDeviceFilterCriteria,
  DeviceFilterCriteria,
  operator
} from "../DeviceFilterCriteria";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { findIndexByProperty, uniqueId } from "utils";
import { IDeviceFilter } from "./DeviceFilter";

interface IAddCriteriaProps {
  filter: IDeviceFilter;
  setFilter: (filter: IDeviceFilter) => void;
}
const AddCriteria: React.FunctionComponent<IAddCriteriaProps> = ({
  filter,
  setFilter
}) => {
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

  const { filterCriteria } = filter;
  return (
    <>
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
    </>
  );
};

export { AddCriteria };
