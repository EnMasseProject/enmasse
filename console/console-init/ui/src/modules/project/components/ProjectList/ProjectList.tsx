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
import { StyleSheet, css } from "aphrodite";
import {
  StatusTypes,
  ProjectStatus,
  ProjectEntities,
  ProjectTypeLabel,
  ProjectTypes
} from "modules/project/utils";
import { EmptyProject } from "modules/project";

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
  type?: string;
  authService?: string;
  plan?: string;
  status?: StatusTypes;
  isReady?: boolean;
  isEnabled?: boolean;
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
  onEnable: (project: IProject) => void;
  onDisable: (project: IProject) => void;
  onDownload: (project: IProject) => void;
  onSelectProject: (project: IProject, isSelected: boolean) => void;
  onSelectAllProject: (isSelected: boolean) => void;
}

export const ProjectList: React.FunctionComponent<IProjectListProps> = ({
  onSort,
  projects,
  sortBy,
  onEdit,
  onDelete,
  onDownload,
  onEnable,
  onDisable,
  onSelectProject,
  onSelectAllProject
}) => {
  const actionResolver = (rowData: IRowData) => {
    const originalData = rowData.originalData as IProject;
    let actions: { id: string; title: string; onClick: () => void }[] = [];
    const { projectType, isEnabled } = originalData;
    if (projectType === ProjectTypes.IOT) {
      actions = [
        {
          id: "delete-project",
          title: "Delete",
          onClick: () => onDelete(originalData)
        }
      ];
      if (isEnabled !== undefined) {
        actions.push({
          id: isEnabled ? "disable-project" : "enable-project",
          title: isEnabled ? "Disable" : "Enable",
          onClick: () => {
            isEnabled ? onDisable(originalData) : onEnable(originalData);
          }
        });
      }
    } else if (projectType === ProjectTypes.MESSAGING) {
      actions = [
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
    }
    return actions;
  };

  const toTableCells = (row: IProject) => {
    const {
      projectType,
      name,
      displayName,
      namespace,
      status,
      creationTimestamp,
      // errorMessageRate,
      addressCount,
      connectionCount,
      deviceCount,
      activeCount,
      selected,
      // errorMessages,
      type
    } = row;
    const tableRow: IRowData = {
      selected: selected,
      cells: [
        {
          title: (
            <>
              <Link
                to={
                  projectType === ProjectTypes.MESSAGING
                    ? `messaging-projects/${namespace}/${name}/${type}/addresses`
                    : `iot-projects/${namespace}/${name}/detail`
                }
              >
                {displayName}
              </Link>
              <br />
              {namespace}
            </>
          ),
          key: displayName
        },
        {
          title: <ProjectTypeLabel projectType={projectType} />,
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
        // TODO: Will be added if backend support Error Percentage
        // {
        //   title: (
        //     <>
        //       <ProjectError
        //         errorCount={errorMessageRate}
        //         errorMessages={errorMessages}
        //       />
        //     </>
        //   ),
        //   key: displayName + "-" + errorMessageRate
        // },
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

  const tableRows = projects?.map(toTableCells);
  const tableColumns = [
    { title: "Name / Namespace", transforms: [sortable] },
    { title: "Type", transforms: [sortable] },
    { title: "Status", transforms: [sortable] },
    { title: "Time created", transforms: [sortable] },
    // TODO: Will be added if backend support Error Percentage
    // {
    //   title: "Error messages"
    // },
    {
      title: "Entities"
    }
  ];

  const onSelect = (
    _: React.FormEvent<HTMLInputElement>,
    isSelected: boolean,
    rowIndex: number
  ) => {
    const rows = [...tableRows];
    if (rowIndex === -1) {
      rows.map(row => (row.selected = isSelected));
      onSelectAllProject(isSelected);
    } else {
      rows[rowIndex].selected = isSelected;
      onSelectProject(rows[rowIndex].originalData, isSelected);
    }
  };

  return (
    <>
      <div className={css(StyleForTable.scroll_overflow)}>
        <Table
          variant={TableVariant.compact}
          canSelectAll={true}
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
      {projects?.length <= 0 && <EmptyProject />}
    </>
  );
};
