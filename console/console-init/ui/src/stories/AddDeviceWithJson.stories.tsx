import React from "react";
import { MemoryRouter } from "react-router";
import { Page } from "@patternfly/react-core";
import { AddDeviceWithJson } from "modules/iot-device/components/AddDeviceWithJson";
import { text } from "@storybook/addon-knobs";
import { action } from "@storybook/addon-actions";

export default {
  title: "Add Device With Json"
};

export const AddDevice = () => {
  return (
    <MemoryRouter>
      <Page>
        <AddDeviceWithJson
          deviceDetail={text("device detail", "")}
          setDeviceDetail={action("setDeviceDetail")}
          onLeave={action("onLeave")}
          onSave={action("onSave")}
          onPreview={action("onPreview")}
        />
      </Page>
    </MemoryRouter>
  );
};
