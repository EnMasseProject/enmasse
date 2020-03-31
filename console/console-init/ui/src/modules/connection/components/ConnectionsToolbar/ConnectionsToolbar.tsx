import React from "react";
import {
  DataToolbarItem,
  DataToolbar,
  DataToolbarContent,
  SelectOptionObject,
  DataToolbarChipGroup,
  DataToolbarChip,
  DataToolbarContentProps
} from "@patternfly/react-core";
import { ISortBy } from "@patternfly/react-table";
import { SortForMobileView, useWindowDimensions } from "components";
import { ConnectionsToggleGroup } from "../ConnectionsToggleGroup";

export interface IConnectionsToolbarProps extends DataToolbarContentProps {
  totalRecords: number;
  filterSelected?: string;
  hostnameSelected?: string;
  hostnameInput?: string;
  containerSelected?: string;
  containerInput?: string;
  hostnameOptions?: any[];
  containerOptions?: any[];
  selectedHostnames: Array<{ value: string; isExact: boolean }>;
  selectedContainers: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onHostnameSelect: (e: any, selection: SelectOptionObject) => void;
  onHostnameClear: () => void;
  onHostnameFilter: (e: any) => any[];
  onContainerSelect: (e: any, selection: SelectOptionObject) => void;
  onContainerClear: () => void;
  onContainerFilter: (e: any) => any[];
  onSearch: () => void;
  onDelete: (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onClearAllFilters: () => void;
}
const ConnectionsToolbar: React.FunctionComponent<IConnectionsToolbarProps> = ({
  totalRecords,
  filterSelected,
  hostnameSelected,
  hostnameInput,
  containerSelected,
  containerInput,
  hostnameOptions,
  containerOptions,
  selectedHostnames,
  selectedContainers,
  onFilterSelect,
  onHostnameSelect,
  onHostnameClear,
  onHostnameFilter,
  onContainerSelect,
  onContainerClear,
  onContainerFilter,
  onSearch,
  onDelete,
  sortValue,
  setSortValue,
  onClearAllFilters
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
  const toolbarItems = (
    <>
      <ConnectionsToggleGroup
        totalRecords={totalRecords}
        filterSelected={filterSelected}
        hostnameSelected={hostnameSelected}
        hostnameInput={hostnameInput}
        containerSelected={containerSelected}
        containerInput={containerInput}
        hostnameOptions={hostnameOptions}
        containerOptions={containerOptions}
        selectedHostnames={selectedHostnames}
        selectedContainers={selectedContainers}
        onFilterSelect={onFilterSelect}
        onHostnameSelect={onHostnameSelect}
        onHostnameClear={onHostnameClear}
        onHostnameFilter={onHostnameFilter}
        onContainerSelect={onContainerSelect}
        onContainerClear={onContainerClear}
        onContainerFilter={onContainerFilter}
        onSearch={onSearch}
        onDelete={onDelete}
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
