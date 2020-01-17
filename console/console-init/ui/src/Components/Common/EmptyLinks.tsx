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

export const EmptyLinks = () => {
  return (
    <EmptyState variant={EmptyStateVariant.full}>
      <EmptyStateIcon icon={GlobeRouteIcon} />
      <Title id="empty-ad-space-title" size="lg">No Links</Title>
      <EmptyStateBody id="empty-ad-space-body">You currently don't have any links</EmptyStateBody>
    </EmptyState>
  );
};
