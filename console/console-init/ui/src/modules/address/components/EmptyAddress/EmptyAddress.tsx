/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useParams } from "react-router-dom";
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
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";

export const EmptyAddress: React.FunctionComponent<{}> = () => {
  const { name, namespace, type } = useParams();
  const { dispatch } = useStoreContext();

  const onCreateAddress = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.CREATE_ADDRESS,
      modalProps: {
        name,
        namespace,
        addressSpaceType: type
      }
    });
  };

  return (
    <EmptyState variant={EmptyStateVariant.full}>
      <EmptyStateIcon icon={PlusCircleIcon} />
      <Title headingLevel="h2" id="empty-address-title" size="lg">
        Create an address
      </Title>
      <EmptyStateBody id="empty-address-text">
        There are currently no addresses available. Please click on the button
        below to create one. Learn more about this in the
        <a href={process.env.REACT_APP_DOCS}> documentation</a>
      </EmptyStateBody>
      <Button
        id="empty-address-create-button"
        aria-label="create address"
        variant={ButtonVariant.primary}
        onClick={onCreateAddress}
      >
        Create Address
      </Button>
    </EmptyState>
  );
};
