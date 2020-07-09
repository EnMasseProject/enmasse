/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { IoTReview, IIoTProjectInput } from "modules/project/components";
import { Loading } from "use-patternfly";
import { useQuery } from "hooks";
import { IOT_PROJECT_COMMAND_REVIEW_DETAIL } from "graphql-module";

interface IIoTProjectReviewProps {
  projectDetail: IIoTProjectInput;
}

export const IoTProjectReview: React.FunctionComponent<IIoTProjectReviewProps> = ({
  projectDetail
}) => {
  const queryVariable = {
    variables: {
      iotProject: {
        metadata: {
          name: projectDetail.iotProjectName,
          namespace: projectDetail.namespace
        },
        enabled: projectDetail.isEnabled
      }
    }
  };

  const { data, loading } = useQuery(
    IOT_PROJECT_COMMAND_REVIEW_DETAIL,
    queryVariable
  );

  if (loading) return <Loading />;

  return (
    <IoTReview
      name={projectDetail && projectDetail.iotProjectName}
      namespace={(projectDetail && projectDetail.namespace) || ""}
      isEnabled={(projectDetail && projectDetail.isEnabled) || false}
      command={data.iotProjectCommand || ""}
    />
  );
};
