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
import { GlobeRouteIcon } from "@patternfly/react-icons";

export const EmptyConnection = () => {
  return (
    <EmptyState variant={EmptyStateVariant.full}>
      <EmptyStateIcon icon={GlobeRouteIcon} />
      <Title headingLevel="h2" id="empty-connection-title" size="lg">
        No connections
      </Title>
      <EmptyStateBody id="empty-connection-body">
        You currently don't have any connections
      </EmptyStateBody>
    </EmptyState>
  );
};
