import React from "react";
import { MemoryRouter } from "react-router";
import { CreateDevice } from "modules/device/dialogs";
import { action } from "@storybook/addon-actions";
import { text } from "@storybook/addon-knobs";

export default {
  title: "Create Device"
};

export const createDeviceWizard = () => {
  return (
    <MemoryRouter>
      <CreateDevice
        setPropertyInput={action("property set")}
        onPropertyClear={action("property clear clicked")}
        onPropertySelect={action("property select clicked")}
        onChangePropertyInput={async () => {}}
      />
    </MemoryRouter>
  );
};
