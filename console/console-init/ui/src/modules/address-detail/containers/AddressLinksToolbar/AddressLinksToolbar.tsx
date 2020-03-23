/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  DataToolbar,
  DataToolbarContent
} from "@patternfly/react-core/dist/js/experimental";
import { ISortBy } from "@patternfly/react-table";
import useWindowDimensions from "components/common/WindowDimension";
import { SortForMobileView } from "components/common/SortForMobileView";
import { AddressLinksFilter } from "../AddressLinksFilter";

interface AddressLinksToolbar {
  filterValue: string;
  setFilterValue: (value: string) => void;
  filterNames: any[];
  setFilterNames: (value: Array<any>) => void;
  filterContainers: any[];
  setFilterContainers: (value: Array<any>) => void;
  filterRole?: string;
  setFilterRole: (role: string | undefined) => void;
  totalLinks: number;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  addressName: string;
  namespace: string;
}
const AddressLinksToolbar: React.FunctionComponent<AddressLinksToolbar> = ({
  filterValue,
  setFilterValue,
  filterNames,
  setFilterNames,
  filterContainers,
  setFilterContainers,
  filterRole,
  setFilterRole,
  totalLinks,
  sortValue,
  setSortValue,
  addressName,
  namespace
}) => {
  const { width } = useWindowDimensions();

  const sortMenuItems = [
    { key: "name", value: "Name", index: 2 },
    { key: "deliveryRate", value: "DeliveryRate", index: 3 },
    { key: "backlog", value: "Backlog", index: 4 }
  ];

  const onDeleteAll = () => {
    setFilterValue("Name");
    setFilterNames([]);
    setFilterContainers([]);
    setFilterRole(undefined);
  };

  return (
    <DataToolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onDeleteAll}
    >
      <DataToolbarContent>
        <>
          <AddressLinksFilter
            filterValue={filterValue}
            setFilterValue={setFilterValue}
            filterNames={filterNames}
            setFilterNames={setFilterNames}
            filterContainers={filterContainers}
            setFilterContainers={setFilterContainers}
            filterRole={filterRole}
            setFilterRole={setFilterRole}
            totalLinks={totalLinks}
            addressName={addressName}
            namespace={namespace}
          />
          {width < 769 && (
            <SortForMobileView
              sortMenu={sortMenuItems}
              sortValue={sortValue}
              setSortValue={setSortValue}
            />
          )}
        </>
      </DataToolbarContent>
    </DataToolbar>
  );
};

export { AddressLinksToolbar };
