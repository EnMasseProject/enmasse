/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import { DeviceFilter } from "modules/iot-device";
import {
  DeviceListAlert,
  DeviceList,
  IDevice,
  DeviceListToolbar
} from "modules/iot-device";
import { text, boolean } from "@storybook/addon-knobs";
import { action } from "@storybook/addon-actions";
import {
  getTableCells,
  getInitialFilter,
  getInitialSelectedColumns
} from "modules/iot-device/utils";
import {
  PageSection,
  Page,
  PageSectionVariants,
  GridItem,
  Grid,
  DropdownItem,
  CardBody,
  Card,
  Divider
} from "@patternfly/react-core";
import { IRowData } from "@patternfly/react-table";

export default {
  title: "Device List Page"
};

const deviceRows: IDevice[] = [
  {
    deviceId: "littlesensor1",
    via: ["device-1", "device-2"],
    enabled: true,
    selected: true,
    lastSeen: "2020-01-20T11:44:28.607Z",
    updated: "2020-01-20T11:44:28.607Z",
    created: "2020-01-20T11:44:28.607Z",
    credentials: '[{"auth-id":"10-id","type":"psk"}]',
    viaGroups: ["Group 1", "Group 2"]
  },
  {
    deviceId: "jboss20",
    via: [],
    enabled: false,
    selected: false,
    lastSeen: "2020-04-20T11:44:28.607Z",
    updated: "2020-04-29T11:44:28.607Z",
    credentials: '[{"auth-id":"10-id","type":"psk"}]',
    created: "2020-04-30T11:44:28.607Z",
    viaGroups: []
  },
  {
    deviceId: "jboss20",
    selected: true
  },
  {
    deviceId: "jboss20",
    via: [],
    enabled: null,
    selected: null,
    lastSeen: "2020-04-20T11:44:28.607Z",
    updated: undefined,
    created: "2020-04-30T11:44:28.607Z"
  },
  ...new Array(15).fill({
    deviceId: "amq12",
    via: [],
    enabled: false,
    selected: false,
    lastSeen: "2020-04-20T11:44:28.607Z",
    updated: "2020-04-29T11:44:28.607Z",
    created: "2020-04-30T11:44:28.607Z",
    credentials: '[{"auth-id":"10-id","type":"psk"}]',
    viaGroups: []
  })
];

const actionResolver = (_rowData: IRowData) => [
  {
    title: "Delete",
    onClick: () => {}
  },
  {
    title: "Disable",
    onClick: () => {}
  }
];

const kebabItems: React.ReactNode[] = [
  <DropdownItem onClick={action("kebab enable devices")}>Enable</DropdownItem>,
  <DropdownItem onClick={action("kebab disable devices")}>
    Disable
  </DropdownItem>,
  <DropdownItem onClick={action("kebab delete devices")}>Delete</DropdownItem>
];

const bulkSelectItems: React.ReactNode[] = [
  <DropdownItem key="item-1" onClick={action("Deselect all")}>
    Select none (0 items)
  </DropdownItem>,
  <DropdownItem key="item-2" onClick={action("Select all items in the page")}>
    Select page (10 items)
  </DropdownItem>,
  <DropdownItem key="item-3" onClick={action("Select all items")}>
    Select all (100 items)
  </DropdownItem>
];

const Data = (
  <Grid hasGutter>
    <GridItem span={3}>
      <Card>
        <CardBody>
          <DeviceFilter filter={getInitialFilter()} setFilter={() => {}} />
        </CardBody>
      </Card>
    </GridItem>
    <GridItem span={9}>
      <DeviceListAlert
        visible={true}
        variant={"info"}
        isInline={true}
        title={text("Alert title", "Run filter to view your devices")}
        description={text(
          "Alert description",
          "You have a total of 36,300 devices"
        )}
      />
      <br />
      <DeviceListToolbar
        kebabItems={kebabItems}
        handleInputDeviceInfo={action("input device info handler clicked")}
        handleJSONUpload={action("json upload handler clicked")}
        isOpen={boolean("is Open", false)}
        onSelect={action("On select handler for bulk select component")}
        onToggle={action("On toggle handler for bulk select component")}
        isChecked={boolean("isChecked", false)}
        items={bulkSelectItems}
        onSelectAllDevices={action("All devices selected")}
        onChange={action("checkbox dropdown changed")}
        handleToggleModal={action("on toggle manage columns")}
      />
      <Divider />
      <DeviceList
        deviceRows={deviceRows}
        onSelectDevice={async () => {}}
        actionResolver={actionResolver}
        selectedColumns={getInitialSelectedColumns()}
      />
    </GridItem>
  </Grid>
);

export const deviceListPage = () => {
  return (
    <MemoryRouter>
      <Page>
        <PageSection variant={PageSectionVariants.default}>{Data}</PageSection>
      </Page>
    </MemoryRouter>
  );
};
