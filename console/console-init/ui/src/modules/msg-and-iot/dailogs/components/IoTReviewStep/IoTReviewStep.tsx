/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { isIoTProjectValid } from "modules/msg-and-iot/dailogs";
import { IIoTProjectInput } from "modules/msg-and-iot/dailogs";
import { IoTReview } from "../IoTReview";

const IoTReviewStep = (projectDetail?: IIoTProjectInput) => {
  const isReviewEnabled = () => {
    isIoTProjectValid(projectDetail);
  };
  const isFinishEnabled = () => {
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
    enableNext: isFinishEnabled(),
    canJumpTo: isReviewEnabled(),
    nextButtonText: "Finish"
  };
};

export { IoTReviewStep };
