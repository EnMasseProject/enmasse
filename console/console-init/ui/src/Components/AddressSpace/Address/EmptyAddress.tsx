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

export const EmptyAddress = () => {
  return (
    <EmptyState variant={EmptyStateVariant.full}>
      <EmptyStateIcon icon={PlusCircleIcon} />
      <Title id='empty-address-title' size="lg">Create an address</Title>
      <EmptyStateBody id='empty-address-text'>
        There are currently no addresses available. Please click on the button
        below to create one.Learn more about this on the
        <Link to="/"> documentation</Link>
      </EmptyStateBody>
      <Button id='empty-address-create-button' variant="primary">Create Address</Button>
    </EmptyState>
  );
};
