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
  EmptyStatePrimary,
  Button
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";

export interface INoResultFound {
  clearFilters: () => void;
}

export const NoResultFound: React.FunctionComponent<INoResultFound> = ({
  clearFilters
}) => {
  return (
    <EmptyState variant={EmptyStateVariant.full} id="no-result-state">
      <EmptyStateIcon icon={SearchIcon} id="no-result-icon" />
      <Title id="no-result-title" size="lg">
        No results found
      </Title>
      <EmptyStateBody id="no-result-body">
        No results match the filter criteria.
      </EmptyStateBody>
      <EmptyStatePrimary>
        <Button
          id="no-result-btn"
          onClick={() => clearFilters()}
          variant="link"
        >
          Clear all filters
        </Button>
      </EmptyStatePrimary>
    </EmptyState>
  );
};
