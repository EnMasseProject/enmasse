/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  SortByDirection,
  IRowData,
  sortable,
  TableProps
} from "@patternfly/react-table";
import { Link } from "react-router-dom";
import { FormatDistance } from "use-patternfly";
import { StyleSheet, css } from "@patternfly/react-styles";
import {
  StatusTypes,
  ProjectStatus,
  ProjectError,
  ProjectEntities,
  ProjectTypePlan,
  ProjectTypes
} from "modules/project/utils";

export const StyleForTable = StyleSheet.create({
  scroll_overflow: {
    overflowY: "auto",
    paddingBottom: 100
  }
});

export interface IProject {
  projectType: ProjectTypes;
  name?: string;
  displayName?: string;
  namespace?: string;
  plan?: string;
  status?: StatusTypes;
  creationTimestamp?: string;
  errorMessageRate?: number;
  addressCount?: number;
  connectionCount?: number;
  deviceCount?: number;
  activeCount?: number;
  selected?: boolean;
  errorMessages?: string[];
}

export interface IProjectListProps extends Pick<TableProps, "sortBy"> {
  onSort?: (_event: any, index: number, direction: SortByDirection) => void;
  projects: IProject[];
  onEdit: (project: IProject) => void;
  onDelete: (project: IProject) => void;
  onDownload: (project: IProject) => void;
  onSelectProject: (project: IProject, isSelected: boolean) => void;
}

export const ProjectList: React.FunctionComponent<IProjectListProps> = ({
  onSort,
  projects,
  sortBy,
  onEdit,
  onDelete,
  onDownload,
  onSelectProject
}) => {
  const actionResolver = (rowData: IRowData) => {
    const originalData = rowData.originalData as IProject;
    return [
      {
        id: "edit-project",
        title: "Edit",
        onClick: () => onEdit(originalData)
      },
      {
        id: "delete-project",
        title: "Delete",
        onClick: () => onDelete(originalData)
      },
      {
        id: "download-certificate-project",
        title: "Download Certificate",
        onClick: () => onDownload(originalData)
      }
    ];
  };

  const toTableCells = (row: IProject) => {
    const {
      projectType,
      name,
      displayName,
      namespace,
      plan,
      status,
      creationTimestamp,
      errorMessageRate,
      addressCount,
      connectionCount,
      deviceCount,
      activeCount,
      selected,
      errorMessages
    } = row;
    const tableRow: IRowData = {
      selected: selected,
      cells: [
        {
          title: (
            <Link
              to={
                projectType === ProjectTypes.MESSAGING
                  ? `address-space/${namespace}/${name}/addresses`
                  : `iot/${namespace}/${name}`
              }
            >
              {displayName}
            </Link>
          ),
          key: displayName
        },
        {
          title: <ProjectTypePlan type={projectType} plan={plan} />,
          key: displayName + "-" + projectType
        },
        {
          title: <ProjectStatus phase={status || ""} />,
          key: displayName + "-" + status
        },
        {
          title: creationTimestamp && (
            <>
              <FormatDistance date={creationTimestamp || ""} /> ago
            </>
          ),
          key: displayName + "-" + creationTimestamp
        },
        {
          title: (
            <>
              <ProjectError
                errorCount={errorMessageRate}
                errorMessages={errorMessages}
              />
            </>
          ),
          key: displayName + "-" + errorMessageRate
        },
        {
          title: (
            <ProjectEntities
              projectType={projectType}
              activeCount={activeCount}
              deviceCount={deviceCount}
              addressCount={addressCount}
              connectionCount={connectionCount}
            />
          ),
          key: displayName + "-" + addressCount || deviceCount
        }
      ],
      originalData: row
    };
    return tableRow;
  };

  const tableRows = projects.map(toTableCells);
  const tableColumns = [
    { title: "Name", transforms: [sortable] },
    { title: "Type/Plan", transforms: [sortable] },
    { title: "Status", transforms: [sortable] },
    { title: "Time created", transforms: [sortable] },
    {
      title: "Error messages"
    },
    {
      title: "Entities"
    }
  ];

  const onSelect = (
    _: React.MouseEvent,
    isSelected: boolean,
    rowIndex: number
  ) => {
    const rows = [...tableRows];
    rows[rowIndex].selected = isSelected;
    onSelectProject(rows[rowIndex].originalData, isSelected);
  };

  return (
    <>
      <div className={css(StyleForTable.scroll_overflow)}>
        <Table
          variant={TableVariant.compact}
          canSelectAll={false}
          onSelect={onSelect}
          cells={tableColumns}
          rows={tableRows}
          actionResolver={actionResolver}
          aria-label="project list"
          onSort={onSort}
          sortBy={sortBy}
        >
          <TableHeader id="project-list-table-header" />
          <TableBody />
        </Table>
      </div>
    </>
  );
};
