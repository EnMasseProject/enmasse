/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Title,
  EmptyState,
  EmptyStateIcon,
  EmptyStateBody,
  EmptyStateVariant
} from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { CreateProject } from "modules/project/dailogs/CreateProject";

export const EmptyProject: React.FunctionComponent<{}> = () => {
  return (
    <EmptyState variant={EmptyStateVariant.full} id="empty-ad-space">
      <EmptyStateIcon icon={PlusCircleIcon} />
      <Title headingLevel="h2" id="empty-project-title" size="lg">
        Create a project
      </Title>
      <EmptyStateBody id="empty-project-body">
        There are currently no projects available. Please click on the button
        below to create one. Learn more about this in the
        <a href={process.env.REACT_APP_DOCS}> documentation</a>
      </EmptyStateBody>
      <CreateProject />
    </EmptyState>
  );
};
