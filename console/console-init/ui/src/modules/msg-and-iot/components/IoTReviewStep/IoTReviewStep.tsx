/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  isIoTProjectValid,
  IIoTProjectInput,
  IoTReview
} from "modules/msg-and-iot";

const IoTReviewStep = (projectDetail?: IIoTProjectInput) => {
  const isEnabled = () => {
    isIoTProjectValid(projectDetail);
  };
  return {
    name: "Review",
    isDisabled: true,
    component: (
      <IoTReview
        name={projectDetail && projectDetail.iotProjectName}
        namespace={(projectDetail && projectDetail.namespace) || ""}
        isEnabled={(projectDetail && projectDetail.isEnabled) || false}
      />
    ),
    enableNext: isEnabled(),
    canJumpTo: isEnabled(),
    nextButtonText: "Finish"
  };
};

export { IoTReviewStep };
