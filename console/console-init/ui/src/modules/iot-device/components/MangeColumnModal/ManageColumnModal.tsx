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
          <Button key="save" variant="primary" onClick={onSave}>
            Save
          </Button>,
          <Button key="cancel" variant="secondary" onClick={handleModalToggle}>
            Cancel
          </Button>
        ]}
      >
        <DataList
          aria-label="Table column management"
          id="table-column-management"
          isCompact
        >
          <DataListItem aria-labelledby="table-column-management-device-id">
            <DataListItemRow>
              <DataListCheck
                aria-labelledby="table-column-management-device-id"
                isChecked={isDeviceIdChecked}
                name="deviceId"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="table-column-management-device-id"
                    key="table-column-management-device-id"
                  >
                    Device ID
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem aria-labelledby="table-column-management-connection-type">
            <DataListItemRow>
              <DataListCheck
                aria-labelledby="table-column-management-connection-type"
                isChecked={isConnectionTypeChecked}
                name="connectionType"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="table-column-management-connection-type"
                    key="table-column-management-connection-type"
                  >
                    Connection type
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem aria-labelledby="table-column-management-status">
            <DataListItemRow>
              <DataListCheck
                aria-labelledby="table-column-management-status"
                isChecked={isStatusChecked}
                name="status"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="table-column-management-status"
                    key="table-column-management-status"
                  >
                    Status
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem aria-labelledby="table-column-management-last-updated">
            <DataListItemRow>
              <DataListCheck
                aria-labelledby="table-column-management-last-updated"
                isChecked={isLastUpdatedChecked}
                name="lastUpdated"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="table-column-management-last-updated"
                    key="table-column-management-last-updated"
                  >
                    Last updated
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem aria-labelledby="table-column-management-last-seen">
            <DataListItemRow>
              <DataListCheck
                aria-labelledby="table-column-management-last-seen"
                isChecked={isLastSeenChecked}
                name="lastSeen"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="table-column-management-last-seen"
                    key="table-column-management-last-seen"
                  >
                    Last seen
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem aria-labelledby="table-column-management-added-date">
            <DataListItemRow>
              <DataListCheck
                aria-labelledby="table-column-management-added-date"
                isChecked={isAddedDateChecked}
                name="addedDate"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="table-column-management-added-date"
                    key="table-column-management-added-date"
                  >
                    Added date
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem aria-labelledby="table-column-management-member-of">
            <DataListItemRow>
              <DataListCheck
                aria-labelledby="table-column-management-member-of"
                isChecked={isMemberOfChecked}
                name="memberOf"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="table-column-management-member-of"
                    key="table-column-management-member-of"
                  >
                    Member of
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem aria-labelledby="table-column-management-via-gateways">
            <DataListItemRow>
              <DataListCheck
                aria-labelledby="table-column-management-via-gateways"
                isChecked={isViaGatewaysChecked}
                name="viaGateways"
                onChange={handleChange}
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id="table-column-management-via-gateways"
                    key="table-column-management-via-gateways"
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
