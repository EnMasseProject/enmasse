import * as React from "react";
import {
  EmptyState,
  EmptyStateVariant,
  EmptyStateIcon,
  Title,
  EmptyStateBody,
  PageSection
} from "@patternfly/react-core";
import { Link } from "react-router-dom";
import { CubesIcon, GlobeRouteIcon } from "@patternfly/react-icons";

interface INoDataFoundProps {
  type: string;
  name: string;
  routeLink: string;
}
export const NoDataFound: React.FunctionComponent<INoDataFoundProps> = ({
  type,
  name,
  routeLink
}) => {
  return (
    <PageSection>
      <PageSection variant="light">
        <EmptyState variant={EmptyStateVariant.full}>
          <EmptyStateIcon icon={GlobeRouteIcon} />
          <Title headingLevel="h5" size="lg">
            {type} Not Found
          </Title>
          <EmptyStateBody>
            {type} <b>{name}</b> no longer exists.
          </EmptyStateBody>
          <Link to={routeLink}>Go to {type} list</Link>
        </EmptyState>
      </PageSection>
    </PageSection>
  );
};
