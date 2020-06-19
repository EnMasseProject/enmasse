import React, { useState, useEffect } from "react";
import { CogsIcon, CheckCircleIcon } from "@patternfly/react-icons";
import {
  EmptyState,
  EmptyStateVariant,
  EmptyStateIcon,
  Title,
  EmptyStateBody,
  Button,
  Progress,
  ProgressSize,
  ButtonVariant
} from "@patternfly/react-core";
import {} from "@patternfly/react-styles";
import { Link } from "react-router-dom";
import { ProjectType } from "modules/project";
interface IFinishedStepProps {
  onClose: () => void;
  routeDetail?: { name: string; namespace: string; type?: string };
  success: boolean;
  projectType?: ProjectType.IOT_PROJECT | ProjectType.MESSAGING_PROJECT;
}

// const styles = StyleSheet.create({
//   empty_state: { padding: 100 },
//   cog_green_color: { color: "green" },
//   cog_black_color: { color: "black" }
// });

const FinishedStep: React.FunctionComponent<IFinishedStepProps> = ({
  onClose,
  routeDetail,
  success,
  projectType
}) => {
  const [percent, setPercent] = useState<number>(0);
  const [isCompleted, setIsCompleted] = useState<boolean>(false);

  const { namespace, name, type } = routeDetail || {};
  useEffect(() => {
    const interval = setInterval(() => {
      if (percent < 100) {
        setPercent(percent + 20);
      } else {
        if (!isCompleted) {
          setIsCompleted(true);
        }
      }
    }, 500);
    return () => clearInterval(interval);
  }, [percent, isCompleted]);

  const projectDetailUrl = () => {
    if (routeDetail && projectType === ProjectType.IOT_PROJECT) {
      return `/iot-projects/${namespace}/${name}`;
    } else {
      return `/messaging-projects/${namespace}/${name}/${type}/addresses`;
    }
  };
  return (
    <>
      {!isCompleted || !success ? (
        <EmptyState
          variant={EmptyStateVariant.full}
          // className={css(styles.empty_state)}
        >
          <EmptyStateIcon
            icon={CogsIcon}
            // className={css(styles.cog_black_color)}
          />
          <Title headingLevel="h5" size="xl">
            Configuration in Progress
          </Title>
          <EmptyStateBody>
            <Progress value={percent} size={ProgressSize.lg} />
            <br />
            Wait a moment for your configuration progress or back to the project
            list
          </EmptyStateBody>
          <br />
          <br />
          <Button variant={ButtonVariant.secondary} onClick={onClose}>
            Back to list
          </Button>
          <br />
          <br />
          <Button variant="link" onClick={onClose}>
            Close
          </Button>
        </EmptyState>
      ) : (
        <EmptyState
          variant={EmptyStateVariant.full}
          // className={css(styles.empty_state)}
        >
          <EmptyStateIcon
            icon={CheckCircleIcon}
            // className={css(styles.cog_green_color)}
          />
          <Title headingLevel="h5" size="xl">
            Creation successful
          </Title>
          <EmptyStateBody>
            Enter your {projectType} Project for management, or return to
            homepage to view all projects.
          </EmptyStateBody>
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
