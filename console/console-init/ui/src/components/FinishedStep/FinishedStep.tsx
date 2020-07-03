import React from "react";
import { CheckCircleIcon, ErrorCircleOIcon } from "@patternfly/react-icons";
import {
  EmptyState,
  EmptyStateVariant,
  EmptyStateIcon,
  Title,
  EmptyStateBody,
  Button,
  ButtonVariant
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { Link } from "react-router-dom";
import { ProjectType } from "modules/project";
interface IFinishedStepProps {
  onClose: () => void;
  routeDetail?: { name: string; namespace: string; type?: string };
  success: boolean;
  projectType?: ProjectType.IOT_PROJECT | ProjectType.MESSAGING_PROJECT;
}

const styles = StyleSheet.create({
  empty_state: { padding: 100 },
  cog_green_color: { color: "green" },
  cog_black_color: { color: "black" },
  cog_red_color: { color: "red" }
});

const FinishedStep: React.FunctionComponent<IFinishedStepProps> = ({
  onClose,
  routeDetail,
  success,
  projectType
}) => {
  const { namespace, name, type } = routeDetail || {};
  const projectDetailUrl = () => {
    if (routeDetail && projectType === ProjectType.IOT_PROJECT) {
      return `/iot-projects/${namespace}/${name}/detail`;
    } else {
      return `/messaging-projects/${namespace}/${name}/${type}/addresses`;
    }
  };
  return (
    <>
      {!success ? (
        <EmptyState
          variant={EmptyStateVariant.full}
          className={css(styles.empty_state)}
        >
          <EmptyStateIcon
            icon={ErrorCircleOIcon}
            className={css(styles.cog_red_color)}
          />
          <Title headingLevel="h5" size="xl">
            Creation error
          </Title>
          <br />
          <EmptyStateBody>
            Error occured while {projectType} Project creation for management,
            or return to homepage to view all projects
          </EmptyStateBody>
          {/* <Link to={projectDetailUrl()}>
          <Button variant={ButtonVariant.primary} component="a">
            View the project
          </Button>
        </Link> */}
          <br />
          <br />
          <Button variant="link" onClick={onClose}>
            Back to list
          </Button>
        </EmptyState>
      ) : (
        <EmptyState
          variant={EmptyStateVariant.full}
          className={css(styles.empty_state)}
        >
          <EmptyStateIcon
            icon={CheckCircleIcon}
            className={css(styles.cog_green_color)}
          />
          <Title headingLevel="h5" size="xl">
            Creation successful
          </Title>
          <br />
          <EmptyStateBody>
            Enter your {projectType} Project for management, or return to
            homepage to view all projects.
          </EmptyStateBody>
          <br />
          <Link to={projectDetailUrl()}>
            <Button variant={ButtonVariant.primary} component="a">
              View the project
            </Button>
          </Link>
          <br />
          <br />
          <Button variant="link" onClick={onClose}>
            Back to list
          </Button>
        </EmptyState>
      )}
    </>
  );
};

export { FinishedStep };