import React from "react";
import { Radio, Title } from "@patternfly/react-core";

interface IProjectTypeConfigurationStepProps {
  selectedStep?: string | "messaging" | "iot";
  setSelectedStep: (value: "messaging" | "iot") => void;
}
const ProjectTypeConfigurationStep: React.FunctionComponent<IProjectTypeConfigurationStepProps> = ({
  selectedStep,
  setSelectedStep
}) => {
  const onChange = (_: any, event: any) => {
    const data = event.currentTarget.value;
    if (data === "messaging") {
      setSelectedStep("messaging");
    } else if (data === "iot") {
      setSelectedStep("iot");
    }
  };
  return (
    <div style={{ paddingLeft: 20 }}>
      <Title headingLevel="h2" size="3xl">
        Choose project type
      </Title>
      <br />
      <span>
        <Radio
          value="iot"
          isChecked={selectedStep === "iot"}
          onChange={onChange}
          label={
            <Title headingLevel="h5" size="lg">
              <b>Create a IoT Project</b>
            </Title>
          }
          description="Manages millions of devices and messaging to everywhere"
          name="radio-step-create-iot"
          id="radio-step-create-iot"
        />
      </span>
      <br />
      <span>
        <Radio
          value="messaging"
          isChecked={selectedStep === "messaging"}
          onChange={onChange}
          label={
            <Title headingLevel="h5" size="lg">
              <b>Create a Messaging Project</b>
            </Title>
          }
          description="Developers can provision messaging with Messaging Project"
          name="radio-step-create-messaging"
          id="radio-step-create-messaging"
        />
      </span>
    </div>
  );
};

export { ProjectTypeConfigurationStep };
