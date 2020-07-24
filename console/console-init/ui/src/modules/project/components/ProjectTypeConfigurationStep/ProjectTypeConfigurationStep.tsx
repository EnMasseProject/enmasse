import React from "react";
import { Radio, Title, TitleSizes } from "@patternfly/react-core";

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

  if (selectedStep === undefined) {
    setSelectedStep("iot");
  }

  return (
    <div style={{ paddingLeft: 20 }}>
      <Title headingLevel="h2" size={TitleSizes["2xl"]}>
        Choose project type
      </Title>
      <br />
      <span>
        <Radio
          value="iot"
          isChecked={selectedStep === "iot"}
          onChange={onChange}
          label={
            <Title headingLevel="h5" size={TitleSizes.lg}>
              IoT Project
            </Title>
          }
          description="Manages millions of devices and messaging to everywhere"
          name="radio-step-create-iot"
          id="prject-config-step-iot-project-radio"
        />
      </span>
      <br />
      <span>
        <Radio
          value="messaging"
          isChecked={selectedStep === "messaging"}
          onChange={onChange}
          label={
            <Title headingLevel="h5" size={TitleSizes.lg}>
              Messaging Project
            </Title>
          }
          description="Developers can provision messaging with Messaging Project"
          name="radio-step-create-messaging"
          id="prject-config-step-messaging-project-radio"
        />
      </span>
    </div>
  );
};

export { ProjectTypeConfigurationStep };
