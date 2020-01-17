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
  EmptyStateVariant,
  Button
} from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { Link } from "@storybook/router";

export const EmptyAddressSpace = () => {
  return (
    <EmptyState variant={EmptyStateVariant.full}>
      <EmptyStateIcon icon={PlusCircleIcon} />
      <Title id="empty-ad-space-title" size="lg">
        Create an address space
      </Title>
      <EmptyStateBody id="empty-ad-space-body">
        There are currently no address spaces available. Please click on the
        button below to create one.Learn more about this on the
        <Link to="/"> documentation</Link>
      </EmptyStateBody>
      <Button id="empty-ad-space-create-button" variant="primary">
        Create Address Space
      </Button>
    </EmptyState>
  );
};
