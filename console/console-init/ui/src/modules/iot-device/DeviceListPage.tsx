/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import { useDocumentTitle } from "use-patternfly";
import {
  Grid,
  GridItem,
  Card,
  CardBody,
  DropdownItem,
  AlertVariant,
  PageSectionVariants,
  PageSection
} from "@patternfly/react-core";
import {
  DeviceFilter,
  DeviceListAlert,
  DeviceListToolbar,
  IDevice,
  IDeviceFilter,
  ManageColumnModal
} from "modules/iot-device/components";
import {
  getHeaderForDialog,
  getDetailForDialog,
  MAX_DEVICE_LIST_COUNT,
  getInitialAlert,
  getInitialFilter,
  getInitialSelectedColumns
} from "modules/iot-device/utils";
import {
  DeviceListContainer,
  EmptyDeviceContainer,
  ISortByWrapper
} from "modules/iot-device/containers";
import { compareObject } from "utils";
import { useStoreContext, MODAL_TYPES, types } from "context-state-reducer";
import { DialogTypes } from "constant";
import { TablePagination } from "components";
import { useLocation, useParams, useHistory } from "react-router";
import { useMutationQuery, useSearchParamsPageChange } from "hooks";
import { DELETE_IOT_DEVICE, TOGGLE_IOT_DEVICE_STATUS } from "graphql-module";

export default function DeviceListPage() {
  const { projectname, namespace } = useParams();

  useDocumentTitle("Device List");
  const [totalDevices, setTotalDevices] = useState<number>();
  const [isAllSelected, setIsAllSelected] = useState<boolean>(false);
  const location = useLocation();
  const history = useHistory();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [selectedDevices, setSelectedDevices] = useState<IDevice[]>([]);
  const [appliedFilter, setAppliedFilter] = useState<IDeviceFilter>(
    getInitialFilter()
  );
  const [showEmptyDevice, setShowEmptyDevice] = useState<boolean>(false);

  const createDeviceFormLink = `/iot-projects/${namespace}/${projectname}/devices/addform`;
  const createDeviceJsonLink = `/iot-projects/${namespace}/${projectname}/devices/addjson`;

  const [deviceAlert, setDeviceAlert] = useState<{
    isVisible: boolean;
    variant: AlertVariant;
    title: string;
    description: string;
  }>(getInitialAlert());
  const [selectedColumns, setSelectedColumns] = useState<string[]>(
    getInitialSelectedColumns()
  );
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const handleToggleModal = () => {
    setIsModalOpen(!isModalOpen);
  };
  const { dispatch } = useStoreContext();

  useEffect(() => {
    dispatch({
      type: types.RESET_DEVICE_ACTION_TYPE
    });
  }, []);

  const [setDeleteDeviceQueryVariables] = useMutationQuery(DELETE_IOT_DEVICE, [
    "devices_for_iot_project"
  ]);

  const [
    setUpdateDeviceQueryVariables
  ] = useMutationQuery(TOGGLE_IOT_DEVICE_STATUS, ["devices_for_iot_project"]);

  const changeDeviceAlert = () => {
    if (totalDevices && totalDevices < MAX_DEVICE_LIST_COUNT) {
      setDeviceAlert({
        ...deviceAlert,
        isVisible: false
      });
    } else if (
      totalDevices &&
      totalDevices > MAX_DEVICE_LIST_COUNT &&
      compareObject(appliedFilter, getInitialFilter())
    ) {
      setDeviceAlert({
        isVisible: true,
        variant: AlertVariant.info,
        title: "Run filter to view your devices",
        description: `You have a total of ${totalDevices} devices, the system lists
                        the ${MAX_DEVICE_LIST_COUNT} most recently added.`
      });
    } else if (
      totalDevices &&
      totalDevices > MAX_DEVICE_LIST_COUNT &&
      !compareObject(appliedFilter, getInitialFilter())
    ) {
      setDeviceAlert({
        isVisible: true,
        variant: AlertVariant.warning,
        title: "Beyond search capability",
        description: `There are ${totalDevices} devices matching current criteria, system
                        returned ${MAX_DEVICE_LIST_COUNT} results. Add criteria to narrow down.`
      });
    }
  };

  useEffect(() => {
    changeDeviceAlert();
  }, [totalDevices]);

  useEffect(() => {
    setIsAllSelected(false);
  }, [page]);

  useSearchParamsPageChange([appliedFilter]);

  const onSelectDevice = (
    data: IDevice,
    isSelected: boolean,
    isAllSelected?: boolean
  ) => {
    if (!isSelected) {
      setIsAllSelected(false);
    }
    if (isSelected === true && selectedDevices.indexOf(data) === -1) {
      setSelectedDevices(prevState => [...prevState, data]);
    } else if (isSelected === false) {
      setSelectedDevices(prevState =>
        prevState.filter(
          device =>
            !compareObject(
              {
                id: device.deviceId
              },
              {
                id: data.deviceId
              }
            )
        )
      );
    }
    if (isAllSelected) {
      setIsAllSelected(true);
    }
  };

  const [sortDropDownValue, setSortDropdownValue] = useState<ISortByWrapper>();

  const onSelectAllDevices = (isSelected: boolean) => {
    if (isSelected === true) {
      setIsAllSelected(true);
    } else if (isSelected === false) {
      setIsAllSelected(false);
      setSelectedDevices([]);
    }
  };

  const runFilter = (filter: IDeviceFilter) => {
    setAppliedFilter(filter);
  };

  const selectAllDevices = (devices: IDevice[]) => {
    setSelectedDevices(devices);
  };

  const onConfirmDeleteSelectedDevices = async (devices: IDevice[]) => {
    const variable = {
      deviceId: devices.map(({ deviceId }) => deviceId),
      iotproject: {
        name: projectname,
        namespace
      }
    };
    await setDeleteDeviceQueryVariables(variable);
  };

  const onConfirmToggleSelectedDeviceStatus = async (data: any) => {
    const { devices, status } = data;
    setIsAllSelected(false);
    setSelectedDevices([]);
    const variable = {
      a: { name: projectname, namespace },
      b: devices.map((device: IDevice) => device.deviceId),
      status
    };
    await setUpdateDeviceQueryVariables(variable);
  };

  const onSelectEnableDevices = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.UPDATE_DEVICE_STATUS,
      modalProps: {
        onConfirm: onConfirmToggleSelectedDeviceStatus,
        selectedItems: selectedDevices.map(device => device.deviceId),
        option: "Enable",
        data: { devices: selectedDevices, status: true },
        detail: getDetailForDialog(selectedDevices, DialogTypes.ENABLE),
        header: getHeaderForDialog(selectedDevices, DialogTypes.ENABLE),
        confirmButtonLabel: "Enable"
      }
    });
  };

  const onSelectDisableDevice = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.UPDATE_DEVICE_STATUS,
      modalProps: {
        onConfirm: onConfirmToggleSelectedDeviceStatus,
        selectedItems: selectedDevices.map(device => device.deviceId),
        option: "Disable",
        data: { devices: selectedDevices, status: false },
        detail: getDetailForDialog(selectedDevices, DialogTypes.DISABLE),
        header: getHeaderForDialog(selectedDevices, DialogTypes.DISABLE),
        confirmButtonLabel: "Disable"
      }
    });
  };

  const onSelectDeleteDevice = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_IOT_DEVICE,
      modalProps: {
        onConfirm: onConfirmDeleteSelectedDevices,
        selectedItems: selectedDevices.map(device => device.deviceId),
        option: "Delete",
        data: selectedDevices,
        detail: getDetailForDialog(selectedDevices, DialogTypes.DELETE),
        header: getHeaderForDialog(selectedDevices, DialogTypes.DELETE),
        confirmButtonLabel: "Delete",
        iconType: "danger"
      }
    });
  };

  const isDeleteDevicesOptionDisabled = () => {
    if (selectedDevices.length > 0) {
      return false;
    }
    return true;
  };

  const isEnableDevicesOptionDisabled = () => {
    if (selectedDevices.length === 0) return true;
    const isEnabled = (device: IDevice) => device?.enabled === true;
    return selectedDevices.some(isEnabled);
  };

  const isDisableDevicesOptionDisabled = () => {
    if (selectedDevices.length === 0) return true;
    const isDisabled = (device: IDevice) => device?.enabled === false;
    return selectedDevices.some(isDisabled);
  };

  const kebabItems: React.ReactNode[] = [
    <DropdownItem
      id="device-list-page-enable-dropdownitem"
      onClick={onSelectEnableDevices}
      isDisabled={isEnableDevicesOptionDisabled()}
    >
      Enable
    </DropdownItem>,
    <DropdownItem
      id="device-list-page-disable-dropdownitem"
      onClick={onSelectDisableDevice}
      isDisabled={isDisableDevicesOptionDisabled()}
    >
      Disable
    </DropdownItem>,
    <DropdownItem
      id="device-list-page-delete-dropdownitem"
      onClick={onSelectDeleteDevice}
      isDisabled={isDeleteDevicesOptionDisabled()}
    >
      Delete
    </DropdownItem>
  ];

  const resetFilter = () => {
    setAppliedFilter(getInitialFilter());
  };

  const handleInputDeviceInfo = () => {
    history.push(createDeviceFormLink);
  };

  const handleJSONUpload = () => {
    history.push(createDeviceJsonLink);
  };

  const renderPagination = () => {
    return (
      <TablePagination
        id="device-list-page-table-pagination"
        itemCount={totalDevices || 0}
        variant={"top"}
        page={page}
        perPage={perPage}
      />
    );
  };

  const { isVisible, title, description, variant } = deviceAlert;

  return (
    <>
      <EmptyDeviceContainer
        handleInputDeviceInfo={handleInputDeviceInfo}
        handleJSONUpload={handleJSONUpload}
        setShowEmptyDevice={setShowEmptyDevice}
        projectname={projectname}
        namespace={namespace}
      />
      {!showEmptyDevice && (
        <Grid hasGutter>
          <GridItem span={3}>
            <Card>
              <CardBody>
                <DeviceFilter runFilter={runFilter} resetFilter={resetFilter} />
              </CardBody>
            </Card>
          </GridItem>
          <GridItem span={9}>
            <PageSection variant={PageSectionVariants.light}>
              <DeviceListAlert
                visible={isVisible}
                variant={variant}
                isInline={true}
                title={title}
                description={description}
              />
              <br />
              <Grid>
                <GridItem span={5}>
                  <DeviceListToolbar
                    kebabItems={kebabItems}
                    handleInputDeviceInfo={handleInputDeviceInfo}
                    handleJSONUpload={handleJSONUpload}
                    isOpen={isAllSelected}
                    isChecked={isAllSelected}
                    items={[]}
                    onChange={() => {}}
                    onSelectAllDevices={onSelectAllDevices}
                    handleToggleModal={handleToggleModal}
                  />
                </GridItem>
                <GridItem span={7}>{renderPagination()}</GridItem>
              </Grid>
              <DeviceListContainer
                page={page}
                projectname={projectname}
                perPage={perPage}
                setTotalDevices={setTotalDevices}
                setIsAllSelected={setIsAllSelected}
                selectedDevices={selectedDevices}
                onSelectDevice={onSelectDevice}
                selectAllDevices={selectAllDevices}
                areAllDevicesSelected={isAllSelected}
                sortValue={sortDropDownValue}
                setSortValue={setSortDropdownValue}
                appliedFilter={appliedFilter}
                resetFilter={resetFilter}
                namespace={namespace}
                selectedColumns={selectedColumns}
              />
              <br />
              {renderPagination()}
            </PageSection>
            <ManageColumnModal
              isModalOpen={isModalOpen}
              handleModalToggle={handleToggleModal}
              setSelectedColumns={setSelectedColumns}
              setSortValue={setSortDropdownValue}
            />
          </GridItem>
        </Grid>
      )}
    </>
  );
}
