import React from "react";
import { MemoryRouter } from "react-router";
import { Page } from "@patternfly/react-core";
import { AddDeviceWithJson } from "modules/iot-device/components/AddDeviceWithJson";

export default {
  title: "Add Device With Json"
};

export const AddDevice = () => {
  return (
    <MemoryRouter>
      <Page>
        {/* <div style={{ overflow: "auto" }}> */}
        <AddDeviceWithJson />
        {/* </div> */}
      </Page>
    </MemoryRouter>
  );
};
