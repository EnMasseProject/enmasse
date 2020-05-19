/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { UploadFile } from "components";
import { action } from "@storybook/addon-actions";
import { boolean } from "@storybook/addon-knobs";

export default {
  title: "Upload File"
};

export const FileUpload = () => {
  return (
    <UploadFile
      value={""}
      setValue={action("set File value")}
      isRejected={boolean("rejected", true)}
      setIsRejected={action("set Rejected")}
      fileRestriction=".crt"
    />
  );
};
