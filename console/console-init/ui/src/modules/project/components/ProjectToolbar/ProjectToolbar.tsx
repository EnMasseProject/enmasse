/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  ToolbarItem,
  Toolbar,
  ToolbarContent,
  ToolbarContentProps
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
  onClearNamespace: () => void;
  isDeleteAllDisabled: boolean;
  onSelectDeleteAll: (event: any) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onClearAllFilters: () => void;
  isAllProjectSelected: boolean;
}
const ProjectToolbar: React.FunctionComponent<IProjectToolbarProps &
  ToolbarContentProps> = ({
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
  onSelectFilter,
  onSelectName,
  onClearName,
  onSelectNamespace,
  onClearNamespace,
  onSelectType,
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
        onSelectFilter={onSelectFilter}
        onSelectName={onSelectName}
        onClearName={onClearName}
        onSelectNamespace={onSelectNamespace}
        onClearNamespace={onClearNamespace}
        onSelectType={onSelectType}
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
      <ToolbarItem data-codemods="true">
        {width < 769 && (
          <SortForMobileView
            sortMenu={sortMenuItems}
            sortValue={sortValue}
            setSortValue={setSortValue}
          />
        )}
      </ToolbarItem>
      <ToolbarItem data-codemods="true">
        <ProjectListKebab
          onSelectDeleteAll={onSelectDeleteAll}
          isDeleteAllDisabled={isDeleteAllDisabled}
        />
      </ToolbarItem>
    </>
  );

  return (
    <Toolbar
      id="project-toolbar-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onClearAllFilters}
      data-codemods="true"
    >
      <ToolbarContent>{toolbarItems}</ToolbarContent>
    </Toolbar>
  );
};
export { ProjectToolbar };
