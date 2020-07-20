/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  ToolbarToggleGroup
} from "@patternfly/react-core";
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
    <ToolbarToggleGroup toggleIcon={<SortAmountDownIcon />} breakpoint="xl">
      <ToolbarItem data-codemods="true">
        {width < 769 && (
          <>
            <DropdownWithToggle
              id="sort-mobile-view-dropdowntoggle"
              toggleId="sort-mobile-view-dropdown-toggle"
              position={DropdownPosition.left}
              onSelectItem={onSortSelect}
              value={sortData}
              dropdownItems={sortMenu}
              dropdownItemIdPrefix="sort-mobilevw-dropdown"
            />
            &nbsp;&nbsp;
            {SortIcons}
          </>
        )}
      </ToolbarItem>
    </ToolbarToggleGroup>
  );

  return (
    <Toolbar
      id="sort-mobile-view-data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      data-codemods="true"
    >
      <ToolbarContent style={{ width: 180 }}>{toolbarItems}</ToolbarContent>
    </Toolbar>
  );
};
