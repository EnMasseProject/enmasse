/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Split,
  SplitItem,
  Title,
  Flex,
  FlexItem,
  DropdownItem,
  DropdownPosition,
  DropdownSeparator,
  PageSection,
  PageSectionVariants,
  Divider,
  Switch
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { FormatDistance } from "use-patternfly";
import { DropdownWithKebabToggle } from "components";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import { DeviceActionType } from "modules/iot-device-detail/utils";
import { DeviceConnectionType } from "constant";

interface IDeviceDetailHeaderProps {
  deviceName?: string;
  addedDate?: string;
  lastSeen?: string;
  deviceStatus?: boolean;
  onChange: (enabled: boolean) => void;
  onDelete: () => void;
  onClone: () => void;
  viaGateway?: boolean;
  credentials?: any[];
  connectiontype?:
    | DeviceConnectionType.CONNECTED_DIRECTLY
    | DeviceConnectionType.VIA_GATEWAYS
    | DeviceConnectionType.NA;
  memberOf?: string[];
}

const styles = StyleSheet.create({
  kebab_toggle_margin: {
    marginTop: 20,
    marginLeft: 10,
    fontSize: 15
  },
  namespace_info_margin: {
    marginTop: 16,
    marginBottom: 24
  },
  no_bottom_padding: {
    paddingBottom: 0
  }
});

const DeviceDetailHeader: React.FunctionComponent<IDeviceDetailHeaderProps> = ({
  deviceName,
  addedDate = "",
  lastSeen = "",
  onDelete,
  onChange,
  onClone,
  deviceStatus,
  credentials = [],
  viaGateway = false,
  connectiontype,
  memberOf = []
}) => {
  const { dispatch } = useStoreContext();

  const onSelectEditMetadata = () => {
    dispatch({
      type: types.SET_DEVICE_ACTION_TYPE,
      payload: { actionType: DeviceActionType.EDIT_METADATA }
    });
  };

  const onSelectEditDeviceInJson = () => {
    dispatch({
      type: types.SET_DEVICE_ACTION_TYPE,
      payload: { actionType: DeviceActionType.EDIT_DEVICE_IN_JSON }
    });
  };

  const onSelctEditGateways = () => {
    dispatch({
      type: types.SET_DEVICE_ACTION_TYPE,
      payload: { actionType: DeviceActionType.EDIT_GATEWAYS }
    });
  };

  const onSelectEditCredentials = () => {
    dispatch({
      type: types.SET_DEVICE_ACTION_TYPE,
      payload: { actionType: DeviceActionType.EDIT_CREDENTIALS }
    });
  };

  const onConfirmConnectionType = () => {
    let actionType;
    if (connectiontype === DeviceConnectionType.CONNECTED_DIRECTLY) {
      actionType = DeviceActionType.CHANGE_CONNECTION_TYPE_VIA_GATEWAYS;
    } else if (connectiontype === DeviceConnectionType.VIA_GATEWAYS) {
      actionType = DeviceActionType.CHANGE_CONNECTION_TYPE_CONNECTED_DIRECTLY;
    }

    if (actionType) {
      dispatch({
        type: types.SET_DEVICE_ACTION_TYPE,
        payload: { actionType }
      });
    }
  };

  const onSelectChangeConnectionType = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.CHANGE_CONNECTION_TYPE,
      modalProps: {
        iconType: "danger",
        confirmButtonLabel: "Change",
        onConfirm: onConfirmConnectionType,
        option: "change-button",
        detail:
          "This action can not be undone. After you change the setting, all data under this setting will also be deleted and is unrecoverable.",
        header: "Do you want to change the connection type?"
      }
    });
  };

  const onSelectGatewayGroupMembership = () => {
    dispatch({
      type: types.SET_DEVICE_ACTION_TYPE,
      payload: { actionType: DeviceActionType.EDIT_GATEWAY_GROUP_MEMBERSHIP }
    });
  };

  const DeviceDetailLayout = () => (
    <>
      <SplitItem>
        <Split hasGutter>
          <SplitItem>
            <Title headingLevel="h1" size="4xl" id="device-detail-name-title">
              {deviceName}
            </Title>
          </SplitItem>
        </Split>
        <Flex className={css(styles.namespace_info_margin)}>
          <FlexItem id="device-detail-header-connection-type-flexitem">
            Connection type : <b>{connectiontype}</b>
          </FlexItem>
          <Divider isVertical />
          <FlexItem id="device-detail-header-added-date-flexitem">
            Added Date :{" "}
            <b>
              <FormatDistance date={addedDate} />
            </b>
          </FlexItem>
          <Divider isVertical />
          <FlexItem id="device-detail-header-last-time-seen-flexitem">
            Last time seen :{" "}
            <b>
              <FormatDistance date={lastSeen} />
            </b>
          </FlexItem>
        </Flex>
      </SplitItem>
    </>
  );

  const additionalKebebOptions = () => {
    const additionalKebabOptions: React.ReactNode[] = [];
    if (viaGateway) {
      additionalKebabOptions.push(
        <DropdownItem
          id="device-detail-edit-gateways-dropdownitem"
          key="edit-gateways"
          aria-label="edit gateways"
          onClick={onSelctEditGateways}
        >
          Edit connection gateways
        </DropdownItem>
      );
    }
    if (credentials?.length > 0) {
      additionalKebabOptions.push(
        <DropdownItem
          id="device-detail-edit-credentials-dropdownitem"
          key="edit-credentials"
          aria-label="edit credentials"
          onClick={onSelectEditCredentials}
        >
          Edit credentials
        </DropdownItem>
      );
    }
    if (memberOf?.length > 0) {
      additionalKebabOptions.push(
        <DropdownItem
          id="device-detail-edit-gateway-group-membership-dropdownitem"
          key="edit-gateway-group-membership"
          aria-label="edit gateway group membership"
          onClick={onSelectGatewayGroupMembership}
        >
          Edit gateway group membership
        </DropdownItem>
      );
    }

    if (
      (!(credentials.length > 0) && viaGateway) ||
      (!viaGateway && credentials?.length > 0)
    ) {
      additionalKebabOptions.push(
        <DropdownItem
          id="device-detail-change-connection-type-dropdownitem"
          key="change-connection-type"
          aria-label="change connection type"
          onClick={onSelectChangeConnectionType}
        >
          Change connection type
        </DropdownItem>
      );
    }

    return additionalKebabOptions;
  };

  const KebabOptionsLayout = () => {
    const dropdownItems: React.ReactNode[] = [
      ...additionalKebebOptions(),
      <DropdownItem
        id="device-detail-edit-metadata-dropdownitem"
        key="edit-metadata"
        aria-label="edit metadata"
        onClick={onSelectEditMetadata}
      >
        Edit metadata
      </DropdownItem>,
      <DropdownItem
        id="device-detail-edit-json-dropdownitem"
        key="edit-in-json"
        aria-label="edit device in json"
        onClick={onSelectEditDeviceInJson}
      >
        Edit device in JSON
      </DropdownItem>,
      <DropdownSeparator key="separator" />,
      <DropdownItem
        id="device-detail-delete-dropdownitem"
        key="delete-device"
        aria-label="delete device"
        onClick={onDelete}
      >
        Delete Device
      </DropdownItem>,
      <DropdownItem
        id="device-detail-clone-dropdownitem"
        key="clone-device"
        aria-label="clone"
        onClick={onClone}
      >
        Clone Device
      </DropdownItem>
    ];
    return (
      <DropdownWithKebabToggle
        id="device-detail-header-kebab-dropdown"
        isPlain={true}
        position={DropdownPosition.right}
        toggleId="device-detail-header-dropdowntoggle"
        dropdownItems={dropdownItems}
      />
    );
  };

  return (
    <PageSection
      variant={PageSectionVariants.light}
      className={css(styles.no_bottom_padding)}
    >
      <Split>
        <DeviceDetailLayout />
        <SplitItem isFilled />
        <SplitItem className={css(styles.kebab_toggle_margin)}>
          <Switch
            id="device-detail-header-status-switch-button"
            label="Enabled"
            labelOff="Disabled"
            onChange={onChange}
            isChecked={deviceStatus}
          />
        </SplitItem>
        <SplitItem className={css(styles.kebab_toggle_margin)}>
          <KebabOptionsLayout />
        </SplitItem>
      </Split>
    </PageSection>
  );
};

export { DeviceDetailHeader };
