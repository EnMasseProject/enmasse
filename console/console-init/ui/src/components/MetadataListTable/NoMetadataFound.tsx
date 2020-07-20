/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  EmptyState,
  EmptyStateVariant,
  EmptyStateIcon,
  Title,
  EmptyStateSecondaryActions,
  Button
} from "@patternfly/react-core";
import { CubesIcon } from "@patternfly/react-icons";

export interface INoMetadataFoundProps {
  id: string;
  addMetadata: () => void;
}

export const NoMetadataFound: React.FC<INoMetadataFoundProps> = ({
  id,
  addMetadata
}) => {
  return (
    <div id={id}>
      <EmptyState variant={EmptyStateVariant.full}>
        <EmptyStateIcon icon={CubesIcon} />
        <Title headingLevel="h1" size="lg">
          There is no device metadata yet
        </Title>
      </EmptyState>
      <EmptyStateSecondaryActions>
        <Button
          id="no-metadata-add-button"
          aria-label="add metadata button"
          variant="primary"
          onClick={addMetadata}
        >
          Add metadata
        </Button>
      </EmptyStateSecondaryActions>
    </div>
  );
};
