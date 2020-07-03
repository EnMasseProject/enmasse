/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Loading } from "use-patternfly";
import { useQuery } from "@apollo/react-hooks";
import { ADDRESS_SPACE_COMMAND_REVIEW_DETAIL } from "graphql-module/queries";
import { MessagingReview } from "modules/project/components";
import { IMessagingProject } from "./CreateProject";
import { getQueryVariableForCreateMessagingProject } from "modules/project/utils";

interface IMessagingProjectReviewProps {
  projectDetail: IMessagingProject;
}

export const MessagingProjectReview: React.FunctionComponent<IMessagingProjectReviewProps> = ({
  projectDetail
}) => {
  const {
    name,
    namespace,
    type,
    plan,
    authService,
    protocols,
    customizeEndpoint,
    addRoutes,
    tlsCertificate
  } = projectDetail || {};

  const queryVariable = {
    variables: getQueryVariableForCreateMessagingProject(projectDetail)
  };

  const { data, loading } = useQuery(
    ADDRESS_SPACE_COMMAND_REVIEW_DETAIL,
    queryVariable
  );

  if (loading) return <Loading />;

  return (
    <MessagingReview
      name={name}
      plan={plan}
      type={type}
      namespace={namespace || ""}
      authenticationService={authService || ""}
      data={data}
      protocols={protocols}
      customizeEndpoint={customizeEndpoint}
      addRoutes={addRoutes}
      tlsCertificate={tlsCertificate}
    />
  );
};
