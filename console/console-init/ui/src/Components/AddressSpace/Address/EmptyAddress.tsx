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
  Button,
  ButtonVariant
} from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
interface IEmptyAddressProps {
  isWizardOpen: boolean;
  setIsWizardOpen: (value: boolean) => void;
}
export const EmptyAddress: React.FunctionComponent<IEmptyAddressProps> = ({
  isWizardOpen,
  setIsWizardOpen
}) => {
  const createAddressOnClick = async () => {
    setIsWizardOpen(true);
  };
  return (
    <EmptyState variant={EmptyStateVariant.full}>
      <EmptyStateIcon icon={PlusCircleIcon} />
      <Title id="empty-address-title" size="lg">
        Create an address
      </Title>
      <EmptyStateBody id="empty-address-text">
        There are currently no addresses available. Please click on the button
        below to create one.Learn more about this in the
        <a href={process.env.REACT_APP_DOCS}> documentation</a>
      </EmptyStateBody>
      <Button
        id="empty-address-create-button"
        variant={ButtonVariant.primary}
        onClick={createAddressOnClick}
      >
        Create Address
      </Button>
    </EmptyState>
  );
};
