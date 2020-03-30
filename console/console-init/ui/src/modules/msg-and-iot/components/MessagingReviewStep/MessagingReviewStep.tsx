/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Review } from "modules/address-space";
import {
  IMessagingProjectInput,
  isMessagingProjectValid
} from "modules/msg-and-iot";

const MessagingReviewStep = (projectDetail?: IMessagingProjectInput) => {
  const isReviewEnabled = () => {
    isMessagingProjectValid(projectDetail);
  };

  const isFinishEnabled = () => {
    isMessagingProjectValid(projectDetail);
  };
  return {
    name: "Review",
    isDisabled: true,
    component: (
      <Review
        name={projectDetail && projectDetail.messagingProjectName}
        namespace={(projectDetail && projectDetail.namespace) || ""}
        type={(projectDetail && projectDetail.messagingProjectType) || ""}
        plan={(projectDetail && projectDetail.messagingProjectPlan) || ""}
        authenticationService={
          (projectDetail && projectDetail.authenticationService) || ""
        }
      />
    ),
    enableNext: isFinishEnabled(),
    canJumpTo: isReviewEnabled(),
    nextButtonText: "Finish"
  };
};

export { MessagingReviewStep };
