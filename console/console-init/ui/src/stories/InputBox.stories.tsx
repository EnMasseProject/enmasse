/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import { text, number, select, boolean } from "@storybook/addon-knobs";
import { action } from "@storybook/addon-actions";
import { InputText } from "components";

export default {
  title: "Input Box"
};

export const InputTextBox = () => {
  return (
    <MemoryRouter>
      <InputText
        label={text("Label", "label to display")}
        value={text("value of input", "text")}
        setValue={action("change text")}
        enableCopy={boolean("Enable copy", false)}
        type={"text"}
        isReadOnly={boolean("isReadOnly", false)}
        ariaLabel={"editable-input-box"}
        isExpandable={boolean("isExpandable", false)}
      />
    </MemoryRouter>
  );
};

export const EncryptedTextBox = () => {
  return (
    <MemoryRouter>
      <InputText
        label={text("Label", "label to display")}
        value={text("value of input", "text")}
        enableCopy={boolean("Enable copy", false)}
        type={"password"}
        ariaLabel={"editable-input-box"}
        isExpandable={boolean("isExpandable", false)}
      />
    </MemoryRouter>
  );
};
