/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { HashRouter as Router } from "react-router-dom";
import { UpdatePassword } from "components";

export default {
  title: "Update password dialog"
};

export const UpdatePasswordDialog = () => {
  return (
    <Router>
      <UpdatePassword />
    </Router>
  );
};
