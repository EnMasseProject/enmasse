/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  DataToolbar,
  DataToolbarContent,
  DataToolbarItem,
  DataToolbarToggleGroup
} from "@patternfly/react-core/dist/js/experimental";
import {
  SortAmountDownAltIcon,
  SortAmountUpAltIcon,
  SortAmountDownIcon
} from "@patternfly/react-icons";
import { ISortBy } from "@patternfly/react-table";
import { DropdownPosition } from "@patternfly/react-core";
import { useWindowDimensions, DropdownWithToggle } from "components";

interface ISortForMobileViewProps {
  sortMenu: any[];
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
}
export const SortForMobileView: React.FunctionComponent<ISortForMobileViewProps> = ({
  sortMenu,
  sortValue,
  setSortValue
}) => {
  const { width } = useWindowDimensions();
  const [sortData, setSortData] = useState("");
  const [sortDirection, setSortDirection] = useState<string>();

  useEffect(() => {
    if (sortValue) {
      const data = sortMenu.filter(data => data.index === sortValue.index);
      if (data && sortData !== data[0].value) {
        setSortData(data[0].value);
        setSortDirection(sortValue.direction);
      }
    }
  }, [sortValue]);

  const onSortSelect = (value: string) => {
    setSortData(value);
    setSortDirection(undefined);
  };

  const onSortUp = () => {
    if (sortData) {
      const sortItem = sortMenu.filter(object => object.value === sortData);
      setSortValue({ index: sortItem[0].index, direction: "asc" });
      setSortDirection("asc");
    }
  };
  const onSortDown = () => {
    if (sortData) {
      const sortItem = sortMenu.filter(object => object.value === sortData);
      setSortValue({ index: sortItem[0].index, direction: "desc" });
      setSortDirection("desc");
    }
  };

  const SortIcons = (
    <>
      {!sortDirection ? (
        <SortAmountDownAltIcon color="grey" onClick={onSortUp} />
      ) : sortDirection === "asc" ? (
        <SortAmountUpAltIcon color="blue" onClick={onSortDown} />
      ) : (
        <SortAmountDownAltIcon color="blue" onClick={onSortUp} />
      )}
    </>
  );

  const toolbarItems = (
    <DataToolbarToggleGroup toggleIcon={<SortAmountDownIcon />} breakpoint="xl">
      <DataToolbarItem>
        {width < 769 && (
          <>
            <DropdownWithToggle
              id="sort-mobilevw-dropdown"
              position={DropdownPosition.left}
              onSelectItem={onSortSelect}
              value={sortData}
              dropdownItems={sortMenu}
              dropdownItemId="sort-mobilevw-dropdown"
            />
            &nbsp;&nbsp;
            {SortIcons}
          </>
        )}
      </DataToolbarItem>
    </DataToolbarToggleGroup>
  );

  return (
    <DataToolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
    >
      <DataToolbarContent style={{ width: 180 }}>
        {toolbarItems}
      </DataToolbarContent>
    </DataToolbar>
  );
};
