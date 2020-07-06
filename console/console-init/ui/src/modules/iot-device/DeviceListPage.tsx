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
import { ISortBy } from "@patternfly/react-table";
import {
  DeviceFilter,
  DeviceListAlert,
  DeviceListToolbar,
  IDevice,
  IDeviceFilter
} from "modules/iot-device/components";
import {
  getHeaderForDialog,
  getDetailForDialog,
  MAX_DEVICE_LIST_COUNT,
  getInitialAlert,
  getInitialFilter
} from "modules/iot-device/utils";
import {
  DeviceListContainer,
  EmptyDeviceContainer
} from "modules/iot-device/containers";
import { compareObject } from "utils";
import { useStoreContext, MODAL_TYPES, types } from "context-state-reducer";
import { DialogTypes } from "constant";
import { TablePagination } from "components";
import { useLocation, useParams } from "react-router";
import { useMutationQuery } from "hooks";
import { DELETE_IOT_DEVICE } from "graphql-module";

export default function DeviceListPage() {
  const { projectname } = useParams();

  useDocumentTitle("Device List");

  const [totalDevices, setTotalDevices] = useState<number>();
  const [isAllSelected, setIsAllSelected] = useState<boolean>(false);
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [selectedDevices, setSelectedDevices] = useState<IDevice[]>([]);
  const [filter, setFilter] = useState<IDeviceFilter>(getInitialFilter());
  const [appliedFilter, setAppliedFilter] = useState<IDeviceFilter>(
    getInitialFilter()
  );

  const [deviceAlert, setDeviceAlert] = useState<{
    isVisible: boolean;
    variant: AlertVariant;
    title: string;
    description: string;
  }>(getInitialAlert());

  const { dispatch } = useStoreContext();

  const [setDeleteDeviceQueryVariables] = useMutationQuery(DELETE_IOT_DEVICE, [
    "devices_for_iot_project"
  ]);

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

  const onSelectDevice = (
    data: IDevice,
    isSelected: boolean,
    isAllSelected?: boolean
  ) => {
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

  const [sortDropDownValue, setSortDropdownValue] = useState<ISortBy>();

  const onSelectAllDevices = (isSelected: boolean) => {
    if (isSelected === true) {
      setIsAllSelected(true);
    } else if (isSelected === false) {
      setIsAllSelected(false);
      setSelectedDevices([]);
    }
  };

  const runFilter = () => {
    setAppliedFilter(filter);
  };

  const selectAllDevices = (devices: IDevice[]) => {
    setSelectedDevices(devices);
  };

  const onConfirmDeleteSelectedDevices = async (devices: IDevice[]) => {
    const variable = {
      deviceId: devices.map(({ deviceId }) => deviceId),
      iotproject: projectname
    };
    await setDeleteDeviceQueryVariables(variable);
  };

  const onConfirmEnableSelectedDevices = () => {
    // TODO: TO BE DONE AFTER BACKEND IS READY
  };

  const onConfirmDisableSelectedDevices = () => {
    // TODO: TO BE DONE AFTER BACKEND IS READY
  };

  const onSelectEnableDevices = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.UPDATE_DEVICE_STATUS,
      modalProps: {
        onConfirm: onConfirmEnableSelectedDevices,
        selectedItems: selectedDevices.map(device => device.deviceId),
        option: "Enable",
        data: selectedDevices,
        detail: getDetailForDialog(selectedDevices, DialogTypes.ENABLE),
        header: getHeaderForDialog(selectedDevices, DialogTypes.ENABLE)
      }
    });
  };

  const onSelectDisableDevice = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.UPDATE_DEVICE_STATUS,
      modalProps: {
        onConfirm: onConfirmDisableSelectedDevices,
        selectedItems: selectedDevices.map(device => device.deviceId),
        option: "Disable",
        data: selectedDevices,
        detail: getDetailForDialog(selectedDevices, DialogTypes.DISABLE),
        header: getHeaderForDialog(selectedDevices, DialogTypes.DISABLE)
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
        header: getHeaderForDialog(selectedDevices, DialogTypes.DELETE)
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
    return selectedDevices.every((device: IDevice) => {
      return device?.enabled === true;
    });
  };

  const isDisableDevicesOptionDisabled = () => {
    return selectedDevices.every((device: IDevice) => {
      return device?.enabled === false;
    });
  };

  const kebabItems: React.ReactNode[] = [
    <DropdownItem
      onClick={onSelectEnableDevices}
      isDisabled={isEnableDevicesOptionDisabled()}
    >
      Enable
    </DropdownItem>,
    <DropdownItem
      onClick={onSelectDisableDevice}
      isDisabled={isDisableDevicesOptionDisabled()}
    >
      Disable
    </DropdownItem>,
    <DropdownItem
      onClick={onSelectDeleteDevice}
      isDisabled={isDeleteDevicesOptionDisabled()}
    >
      Delete
    </DropdownItem>
  ];

  const resetFilter = () => {
    setFilter(getInitialFilter());
    setAppliedFilter(getInitialFilter());
  };

  const handleInputDeviceInfo = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.CREATE_DEVICE,
      modalProps: {}
    });
  };

  const handleJSONUpload = () => {
    // TODO: After create device is ready
  };

  const renderPagination = () => {
    return (
      <TablePagination
        itemCount={totalDevices || 0}
        variant={"top"}
        page={page}
        perPage={perPage}
      />
    );
  };

  const { isVisible, title, description, variant } = deviceAlert;

  if (totalDevices === 0 && compareObject(appliedFilter, getInitialFilter())) {
    return (
      <EmptyDeviceContainer
        handleInputDeviceInfo={handleInputDeviceInfo}
        handleJSONUpload={handleJSONUpload}
        setTotalDevices={setTotalDevices}
      />
    );
  }

  return (
    <Grid hasGutter>
      <GridItem span={3}>
        <Card>
          <CardBody>
            <DeviceFilter
              filter={filter}
              setFilter={setFilter}
              runFilter={runFilter}
              resetFilter={resetFilter}
            />
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
              />
            </GridItem>
            <GridItem span={7}>{renderPagination()}</GridItem>
          </Grid>
          <DeviceListContainer
            page={page}
            projectname={projectname}
            perPage={perPage}
            setTotalDevices={setTotalDevices}
            selectedDevices={selectedDevices}
            onSelectDevice={onSelectDevice}
            selectAllDevices={selectAllDevices}
            areAllDevicesSelected={isAllSelected}
            sortValue={sortDropDownValue}
            setSortValue={setSortDropdownValue}
            appliedFilter={appliedFilter}
            resetFilter={resetFilter}
          />
          <br />
          {renderPagination()}
        </PageSection>
      </GridItem>
    </Grid>
  );
}
