import React from "react";
import { MemoryRouter } from "react-router";
import { CreateDevice } from "modules/device/dialogs";

export default {
  title: "Create Device"
};

export const createDeviceWizard = () => {
  return (
    <MemoryRouter>
      <CreateDevice />
    </MemoryRouter>
  );
};
