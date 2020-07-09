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
import { getInitialCheckedColumns } from "modules/iot-device/utils";

export interface ICheckedColumns {
  isDeviceIdChecked: boolean;
  isConnectionTypeChecked: boolean;
  isStatusChecked: boolean;
  isLastUpdatedChecked: boolean;
  isLastSeenChecked: boolean;
  isAddedDateChecked: boolean;
  isMemberOfChecked: boolean;
  isViaGatewaysChecked: boolean;
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
  const [checkedColumns, setCheckedColumns] = useState<ICheckedColumns>(
    getInitialCheckedColumns()
  );

  const handleChange = (checked: boolean, event: any) => {
    switch (event.target.name) {
      case "deviceId":
        setCheckedColumns({ ...checkedColumns, isDeviceIdChecked: checked });
        break;
      case "connectionType":
        setCheckedColumns({
          ...checkedColumns,
          isConnectionTypeChecked: checked
        });
        break;
      case "status":
        setCheckedColumns({ ...checkedColumns, isStatusChecked: checked });
        break;
      case "lastUpdated":
        setCheckedColumns({ ...checkedColumns, isLastUpdatedChecked: checked });
        break;
      case "lastSeen":
        setCheckedColumns({ ...checkedColumns, isLastSeenChecked: checked });
        break;
      case "addedDate":
        setCheckedColumns({ ...checkedColumns, isAddedDateChecked: checked });
        break;
      case "memberOf":
        setCheckedColumns({ ...checkedColumns, isMemberOfChecked: checked });
        break;
      case "viaGateways":
        setCheckedColumns({ ...checkedColumns, isViaGatewaysChecked: checked });
        break;
    }
  };
  const onSave = () => {
    const columns = [];
    const {
      isDeviceIdChecked,
      isConnectionTypeChecked,
      isStatusChecked,
      isLastUpdatedChecked,
      isLastSeenChecked,
      isAddedDateChecked,
      isMemberOfChecked,
      isViaGatewaysChecked
    } = checkedColumns;

    if (isDeviceIdChecked) {
      columns.push("deviceId");
    }
    if (isConnectionTypeChecked) {
      columns.push("connectionType");
    }
    if (isStatusChecked) {
      columns.push("status");
    }
    if (isLastUpdatedChecked) {
      columns.push("lastUpdated");
    }
    if (isLastSeenChecked) {
      columns.push("lastSeen");
    }
    if (isAddedDateChecked) {
      columns.push("addedDate");
    }
    if (isMemberOfChecked) {
      columns.push("memberOf");
    }
    if (isViaGatewaysChecked) {
      columns.push("viaGateways");
    }
    setSelectedColumns(columns);
    handleModalToggle();
  };

  const {
    isDeviceIdChecked,
    isConnectionTypeChecked,
    isStatusChecked,
    isLastUpdatedChecked,
    isLastSeenChecked,
    isAddedDateChecked,
    isMemberOfChecked,
    isViaGatewaysChecked
  } = checkedColumns;

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
            id="manage-column-save-button"
            variant="primary"
            onClick={onSave}
          >
            Save
          </Button>,
          <Button
            key="cancel"
            id="manage-column-cancel-button"
            variant="secondary"
            onClick={handleModalToggle}
          >
            Cancel
          </Button>
        ]}
      >
        <DataList
          aria-label="Table column management"
          id="device-list-column-management"
          isCompact
        >
          <DataListItem
            id="device-list-col-mangmnt-deviceid"
            aria-labelledby="table-column-management-device-id"
          >
            <DataListItemRow>
              <DataListCheck
                aria-labelledby="device list table column management device id"
                id="device-list-col-mangmnt-deviceid-checkbox"
                isChecked={isDeviceIdChecked}
                name="deviceId"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="device-list-column-management-device-id"
                    key="device-list-column-management-device-id"
                  >
                    Device ID
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem
            id="device-list-col-mangmnt-connection-type"
            aria-labelledby="table-column-management-connection-type"
          >
            <DataListItemRow>
              <DataListCheck
                aria-labelledby="device-list-column-management-connection-type"
                id="device-list-col-mangmnt-connection-type-checkbox"
                isChecked={isConnectionTypeChecked}
                name="connectionType"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="device-list-column-management-connection-type"
                    key="device-list-column-management-connection-type"
                  >
                    Connection type
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem
            id="device-list-col-mangmnt-status"
            aria-labelledby="device-list-column-management-status"
          >
            <DataListItemRow>
              <DataListCheck
                id="device-list-col-mangmnt-status-checkbox"
                aria-labelledby="device-list-column-management-status"
                isChecked={isStatusChecked}
                name="status"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="device-list-column-management-status"
                    key="device-list-column-management-status"
                  >
                    Status
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem
            id="device-list-col-mangmnt-last-updated"
            aria-labelledby="device-list-column-management-last-updated"
          >
            <DataListItemRow>
              <DataListCheck
                id="device-list-col-mangmnt-last-updated-checkbox"
                aria-labelledby="device-list-column-management-last-updated"
                isChecked={isLastUpdatedChecked}
                name="lastUpdated"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="device-list-column-management-last-updated"
                    key="device-list-column-management-last-updated"
                  >
                    Last updated
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem
            id="device-list-col-mangmnt-last-seen"
            aria-labelledby="device-list-column-management-last-seen"
          >
            <DataListItemRow>
              <DataListCheck
                id="device-list-col-mangmnt-last-seen-checkbox"
                aria-labelledby="device-list-column-management-last-seen"
                isChecked={isLastSeenChecked}
                name="lastSeen"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="device-list-column-management-last-seen"
                    key="device-list-column-management-last-seen"
                  >
                    Last seen
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem
            id="device-list-col-mangmnt-added-date"
            aria-labelledby="device-list-column-management-added-date"
          >
            <DataListItemRow>
              <DataListCheck
                id="device-list-col-mangmnt-added-date-checkbox"
                aria-labelledby="device-list-column-management-added-date"
                isChecked={isAddedDateChecked}
                name="addedDate"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="device-list-column-management-added-date"
                    key="device-list-column-management-added-date"
                  >
                    Added date
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem
            id="device-list-col-mangmnt-member-of"
            aria-labelledby="device-list-column-management-member-of"
          >
            <DataListItemRow>
              <DataListCheck
                id="device-list-col-mangmnt-member-of-checkbox"
                aria-labelledby="device-list-column-management-member-of"
                isChecked={isMemberOfChecked}
                name="memberOf"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="device-list-column-management-member-of"
                    key="device-list-column-management-member-of"
                  >
                    Member of
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem
            id="device-list-col-mangmnt-via-gateways"
            aria-labelledby="device-list-column-management-via-gateways"
          >
            <DataListItemRow>
              <DataListCheck
                id="device-list-col-mangmnt-via-gateways-checkbox"
                aria-labelledby="device-list-column-management-via-gateways"
                isChecked={isViaGatewaysChecked}
                name="viaGateways"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="device-list-column-management-via-gateways"
                    key="device-list-column-management-via-gateways"
                  >
                    Via gateways
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
        </DataList>
      </Modal>
    </>
  );
};

export { ManageColumnModal };
