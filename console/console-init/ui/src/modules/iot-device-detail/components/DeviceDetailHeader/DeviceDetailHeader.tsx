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
import { useStoreContext, types } from "context-state-reducer";
import { DeviceActionType } from "modules/iot-device-detail/utils";

interface IDeviceDetailHeaderProps {
  deviceName?: string;
  addedDate: string;
  lastTimeSeen: string;
  deviceStatus?: boolean;
  onChange: (enabled: boolean) => void;
  onDelete: () => void;
  onClone: () => void;
  viaGateway?: boolean;
  credentials?: any[];
  connectiontype?: string;
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
  addedDate,
  lastTimeSeen,
  onDelete,
  onChange,
  onClone,
  deviceStatus,
  credentials = [],
  viaGateway = false,
  connectiontype
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

  const onSelectChangeConnectionType = () => {
    dispatch({
      type: types.SET_DEVICE_ACTION_TYPE,
      payload: { actionType: DeviceActionType.CHANGE_CONNECTION_TYPE }
    });
  };

  const DeviceDetailLayout = () => (
    <>
      <SplitItem>
        <Split hasGutter>
          <SplitItem>
            <Title headingLevel="h1" size="4xl" id="title-device-name">
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
              <FormatDistance date={lastTimeSeen} />
            </b>
          </FlexItem>
        </Flex>
      </SplitItem>
    </>
  );

  const additionalKebebOptions = () => {
    const additionalKebabOptions: React.ReactNode[] = [];
    if (!(credentials.length > 0) && viaGateway) {
      additionalKebabOptions.push(
        <DropdownItem
          id="device-detail-header-kebab-option-edit-gateways"
          key="edit-gateways"
          aria-label="edit gateways"
          onClick={onSelctEditGateways}
        >
          Edit gateways
        </DropdownItem>
      );
    } else if (!viaGateway && credentials?.length > 0) {
      additionalKebabOptions.push(
        <DropdownItem
          id="device-detail-header-kebab-option-edit-credentials"
          key="edit-credentials"
          aria-label="edit credentials"
          onClick={onSelectEditCredentials}
        >
          Edit credentials
        </DropdownItem>
      );
    }

    if (
      (!(credentials.length > 0) && viaGateway) ||
      (!viaGateway && credentials?.length > 0)
    ) {
      additionalKebabOptions.push(
        <DropdownItem
          id="device-detail-header-kebab-option-change-connection-type"
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
        id="device-detail-header-kebab-option-edit-metadata"
        key="edit-metadata"
        aria-label="edit metadata"
        onClick={onSelectEditMetadata}
      >
        Edit metadata
      </DropdownItem>,
      <DropdownItem
        id="device-detail-header-kebab-option-edit-json"
        key="edit-in-json"
        aria-label="edit device in json"
        onClick={onSelectEditDeviceInJson}
      >
        Edit device in JSON
      </DropdownItem>,
      <DropdownSeparator key="separator" />,
      <DropdownItem
        id="device-detail-header-kebab-option-delete"
        key="delete-device"
        aria-label="delete device"
        onClick={onDelete}
      >
        Delete Device
      </DropdownItem>,
      <DropdownItem
        id="device-detail-header-kebab-option-clone"
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
        toggleId="device-detail-header-kebab-toggle"
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
