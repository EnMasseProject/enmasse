/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  DataToolbarItem,
  DataToolbar,
  DataToolbarContent,
  DataToolbarContentProps,
  DropdownItem
} from "@patternfly/react-core";
import { ISortBy } from "@patternfly/react-table";
import {
  SortForMobileView,
  useWindowDimensions,
  DropdownWithKebabToggle
} from "components";
import {
  ConnectionsToggleGroup,
  IConnectionsToggleGroupProps
} from "modules/connection/components";
import { IConnection } from "modules/connection";

export interface IConnectionsToolbarProps extends IConnectionsToggleGroupProps {
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onClearAllFilters: () => void;
  selectedConnections: IConnection[];
  onCloseAll: () => void;
}
const ConnectionsToolbar: React.FunctionComponent<IConnectionsToolbarProps &
  DataToolbarContentProps> = ({
  totalRecords,
  filterSelected,
  hostnameSelected,
  hostnameInput,
  containerSelected,
  containerInput,
  selectedHostnames,
  selectedContainers,
  onFilterSelect,
  onHostnameSelect,
  onHostnameClear,
  onContainerSelect,
  onContainerClear,
  onSearch,
  onDelete,
  sortValue,
  setSortValue,
  onClearAllFilters,
  onChangeHostNameInput,
  onChangeContainerInput,
  setHostNameInput,
  setHostContainerInput,
  selectedConnections,
  onCloseAll
}) => {
  const { width } = useWindowDimensions();
  const sortMenuItems = [
    { key: "hostname", value: "Hostname", index: 0 },
    { key: "containerId", value: "Container ID", index: 1 },
    { key: "protocol", value: "Protocol", index: 2 },
    { key: "messageIn", value: "Message In", index: 3 },
    { key: "messageOut", value: "Message Out", index: 4 },
    { key: "sender", value: "Senders", index: 5 },
    { key: "receiver", value: "Receivers", index: 6 }
  ];
  const dropdownItems = [
    <DropdownItem
      id="cl-filter-dropdown-item-closeAll"
      key="close-all"
      value="closeAll"
      component="button"
      isDisabled={selectedConnections.length <= 0}
    >
      Close Selected
    </DropdownItem>
  ];

  const onKebabSelect = async (event: any) => {
    const kebabOptionValue = event.target.value;
    if (kebabOptionValue && kebabOptionValue === "closeAll") {
      await onCloseAll();
    }
  };

  const toolbarItems = (
    <>
      <ConnectionsToggleGroup
        totalRecords={totalRecords}
        filterSelected={filterSelected}
        hostnameSelected={hostnameSelected}
        hostnameInput={hostnameInput}
        containerSelected={containerSelected}
        containerInput={containerInput}
        selectedHostnames={selectedHostnames}
        selectedContainers={selectedContainers}
        onFilterSelect={onFilterSelect}
        onHostnameSelect={onHostnameSelect}
        onHostnameClear={onHostnameClear}
        onContainerSelect={onContainerSelect}
        onContainerClear={onContainerClear}
        onSearch={onSearch}
        onDelete={onDelete}
        onChangeHostNameInput={onChangeHostNameInput}
        setHostContainerInput={setHostContainerInput}
        onChangeContainerInput={onChangeContainerInput}
        setHostNameInput={setHostNameInput}
      />
      <DataToolbarItem>
        {width < 769 && (
          <SortForMobileView
            sortMenu={sortMenuItems}
            sortValue={sortValue}
            setSortValue={setSortValue}
          />
        )}
      </DataToolbarItem>
      <DataToolbarItem>
        <DropdownWithKebabToggle
          id="cl-select-kebab-overflow-dropdown-"
          toggleId="cl-filter-overflow-kebab"
          onSelect={onKebabSelect}
          dropdownItems={dropdownItems}
          isPlain
        />
      </DataToolbarItem>
    </>
  );

  return (
    <DataToolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onClearAllFilters}
    >
      <DataToolbarContent>{toolbarItems}</DataToolbarContent>
    </DataToolbar>
  );
};
export { ConnectionsToolbar };
