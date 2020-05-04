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
import { StyleSheet, css } from "@patternfly/react-styles";
interface IFinishedStepProps {
  onClose: () => void;
  success: boolean;
  projectType?: "IoT" | "Messaging";
}

const styles = StyleSheet.create({
  empty_state: { padding: 100 },
  cog_green_color: { color: "green" },
  cog_black_color: { color: "black" }
});

const FinishedStep: React.FunctionComponent<IFinishedStepProps> = ({
  onClose,
  success,
  projectType
}) => {
  const [percent, setPercent] = useState<number>(0);
  const [isCompleted, setIsCompleted] = useState<boolean>(false);
  const tick = () => {
    if (percent < 100) {
      setPercent(percent + 20);
    } else {
      if (!isCompleted) {
        setIsCompleted(true);
      }
    }
  };
  useEffect(() => {
    const interval = setInterval(() => tick(), 500);
    return () => clearInterval(interval);
  }, [percent]);

  return (
    <>
      {!isCompleted || !success ? (
        <EmptyState
          variant={EmptyStateVariant.full}
          className={css(styles.empty_state)}
        >
          <EmptyStateIcon
            icon={CogsIcon}
            className={css(styles.cog_black_color)}
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
          className={css(styles.empty_state)}
        >
          <EmptyStateIcon
            icon={CheckCircleIcon}
            className={css(styles.cog_green_color)}
          />
          <Title headingLevel="h5" size="xl">
            Creation successful
          </Title>
          <EmptyStateBody>
            Enter your {projectType} Project for management, or return to
            homepage to view all projects.
          </EmptyStateBody>
          <Button variant={ButtonVariant.primary} component="a" href="/">
            View the project
          </Button>
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
