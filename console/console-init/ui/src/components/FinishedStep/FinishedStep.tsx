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
interface IFinishedStepProps {
  onClose: () => void;
  sucess: boolean;
}
const FinishedStep: React.FunctionComponent<IFinishedStepProps> = ({
  onClose,
  sucess
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
    const interval = setInterval(() => tick(), 1000);
    return () => clearInterval(interval);
  }, [percent]);

  return (
    <>
      {!isCompleted || !sucess ? (
        <EmptyState variant={EmptyStateVariant.full} style={{ padding: 100 }}>
          <EmptyStateIcon icon={CogsIcon} style={{ color: "black" }} />
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
        <EmptyState variant={EmptyStateVariant.full} style={{ padding: 100 }}>
          <EmptyStateIcon icon={CheckCircleIcon} style={{ color: "green" }} />
          <Title headingLevel="h5" size="xl">
            Creation successful
          </Title>
          <EmptyStateBody>
            Enter your IoTProject for management, or return to homepage to view
            all projects.
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
