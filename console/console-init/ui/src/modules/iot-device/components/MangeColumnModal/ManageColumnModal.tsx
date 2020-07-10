/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Modal,
  TextContent,
  TextVariants,
  Text,
  Button,
  DataList,
  DataListItem,
  DataListItemRow,
  DataListCheck,
  DataListItemCells,
  DataListCell
} from "@patternfly/react-core";
import { getInitialManageColumnsForDevices } from "modules/iot-device/utils";
import { createDeepCopy } from "utils";

export interface IManageCoulmnOption {
  key: string;
  value: string;
  label: string;
  isChecked: boolean;
}
interface IManageColumnModalProps {
  isModalOpen: boolean;
  handleModalToggle: () => void;
  setSelectedColumns: (columns: string[]) => void;
}

const ManageColumnModal: React.FunctionComponent<IManageColumnModalProps> = ({
  isModalOpen,
  handleModalToggle,
  setSelectedColumns
}) => {
  const [manageColumnsOptions, setManageColumnsOptions] = useState<
    IManageCoulmnOption[]
  >(getInitialManageColumnsForDevices);

  const handleChange = (checked: boolean, event: any) => {
    const manageColumnsOptionsCopy: IManageCoulmnOption[] = createDeepCopy(
      manageColumnsOptions
    );
    manageColumnsOptionsCopy.forEach((column: IManageCoulmnOption) => {
      if (column.value.toLowerCase() === event.target.name?.toLowerCase()) {
        column.isChecked = checked;
      }
    });
    setManageColumnsOptions(manageColumnsOptionsCopy);
  };
  const onSave = () => {
    const columns = manageColumnsOptions
      .filter((column: IManageCoulmnOption) => column.isChecked === true)
      .map((column: IManageCoulmnOption) => column.value);
    setSelectedColumns(columns);
    handleModalToggle();
  };

  const renderDataListItem = (
    isChecked: boolean,
    label: string,
    name: string,
    key: string
  ) => {
    return (
      <DataListItem
        id={`device-list-col-mangmnt-${key}`}
        aria-labelledby={`table column management ${label.toLowerCase()}`}
      >
        <DataListItemRow>
          <DataListCheck
            aria-labelledby={`device list table column management ${label.toLowerCase()}`}
            id={`device-list-col-mangmnt-${key}-checkbox`}
            isChecked={isChecked}
            name={name}
            onChange={handleChange}
          />
          <DataListItemCells
            dataListCells={[
              <DataListCell
                id={`device-list-column-management-${key}`}
                key={`device-list-column-management-${key}`}
              >
                {label}
              </DataListCell>
            ]}
          />
        </DataListItemRow>
      </DataListItem>
    );
  };

  return (
    <>
      <Modal
        title="Manage columns"
        isOpen={isModalOpen}
        id="device-list-manage-column-modal"
        variant="small"
        description={
          <TextContent>
            <Text component={TextVariants.p}>
              Checked categories will be displayed in the table.
            </Text>
          </TextContent>
        }
        onClose={handleModalToggle}
        actions={[
          <Button
            key="save"
            id="manage-column-modal-save-button"
            variant="primary"
            onClick={onSave}
          >
            Save
          </Button>,
          <Button
            key="cancel"
            id="manage-column-modal-cancel-button"
            variant="secondary"
            onClick={handleModalToggle}
          >
            Cancel
          </Button>
        ]}
      >
        <DataList
          aria-label="table column management"
          id="device-list-column-management-datalist"
          isCompact
        >
          {manageColumnsOptions.map((column: IManageCoulmnOption) =>
            renderDataListItem(
              column.isChecked,
              column.label,
              column.value,
              column.key
            )
          )}
        </DataList>
      </Modal>
    </>
  );
};

export { ManageColumnModal };
