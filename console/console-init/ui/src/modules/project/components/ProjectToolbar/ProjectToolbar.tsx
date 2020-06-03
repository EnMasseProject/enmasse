/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  DataToolbarItem,
  DataToolbar,
  DataToolbarContent,
  DataToolbarContentProps
} from "@patternfly/react-core";
import { ISortBy } from "@patternfly/react-table";
import {
  ProjectListKebab,
  ProjectToolbarToggleGroup,
  IProjectToolbarToggleGroupProps
} from "modules/project";
import { SortForMobileView, useWindowDimensions } from "components";
import { sortMenuItems } from "modules/project/utils";
export interface IProjectToolbarProps extends IProjectToolbarToggleGroupProps {
  onNamespaceClear: () => void;
  isDeleteAllDisabled: boolean;
  onSelectDeleteAll: (event: any) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onClearAllFilters: () => void;
  isAllProjectSelected: boolean;
}
const ProjectToolbar: React.FunctionComponent<IProjectToolbarProps &
  DataToolbarContentProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  namespaceSelected,
  namespaceInput,
  nameOptions,
  namespaceOptions,
  typeSelected,
  selectedNames,
  selectedNamespaces,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onNamespaceSelect,
  onNamespaceClear,
  onTypeSelect,
  onDeleteAll,
  onSearch,
  onDelete,
  isDeleteAllDisabled,
  onSelectDeleteAll,
  sortValue,
  setSortValue,
  onClearAllFilters,
  onChangeNameInput,
  onChangeNameSpaceInput,
  setNameInput,
  setNameSpaceInput,
  onSelectAllProjects,
  isAllProjectSelected
}) => {
  const { width } = useWindowDimensions();
  const toolbarItems = (
    <>
      <ProjectToolbarToggleGroup
        totalRecords={totalRecords}
        filterSelected={filterSelected}
        nameSelected={nameSelected}
        nameInput={nameInput}
        namespaceSelected={namespaceSelected}
        namespaceInput={namespaceInput}
        nameOptions={nameOptions}
        namespaceOptions={namespaceOptions}
        typeSelected={typeSelected}
        selectedNames={selectedNames}
        selectedNamespaces={selectedNamespaces}
        onFilterSelect={onFilterSelect}
        onNameSelect={onNameSelect}
        onNameClear={onNameClear}
        onNamespaceSelect={onNamespaceSelect}
        onNamespaceClear={onNamespaceClear}
        onTypeSelect={onTypeSelect}
        onDeleteAll={onDeleteAll}
        onSearch={onSearch}
        onDelete={onDelete}
        onChangeNameInput={onChangeNameInput}
        onChangeNameSpaceInput={onChangeNameSpaceInput}
        setNameInput={setNameInput}
        setNameSpaceInput={setNameSpaceInput}
        isAllProjectSelected={isAllProjectSelected}
        onSelectAllProjects={onSelectAllProjects}
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
        <ProjectListKebab
          onSelectDeleteAll={onSelectDeleteAll}
          isDeleteAllDisabled={isDeleteAllDisabled}
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
export { ProjectToolbar };
