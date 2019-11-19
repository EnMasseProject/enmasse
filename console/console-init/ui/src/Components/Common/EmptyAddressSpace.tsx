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
      <Title size="lg">Create an address space</Title>
      <EmptyStateBody>
        There are currently no address spaces available. Please click on the button
        below to create one.Learn more about this on the
        <Link to="/"> documentation</Link>
      </EmptyStateBody>
      <Button variant="primary">Create Address Space</Button>
    </EmptyState>
  );
};
