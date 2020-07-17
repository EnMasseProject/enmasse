/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { StatusLabelWithIcon } from "components";

export default {
  title: "StatusLabelWithIcon"
};

export const StatusLabelWithIconAsEnabled = () => (
  <StatusLabelWithIcon id="status-enabled" isEnabled={true} />
);

export const StatusLabelWithIconAsDisabled = () => (
  <StatusLabelWithIcon id="status-disabled" isEnabled={false} />
);
