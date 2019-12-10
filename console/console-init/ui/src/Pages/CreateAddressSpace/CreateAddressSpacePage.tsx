import * as React from "react";
import { Button, Wizard } from "@patternfly/react-core";

interface ICreateAddressSpaceProps {
  isCreateWizardOpen: boolean;
  setIsCreateWizardOpen: (value: boolean) => void;
}
export const CreateAddressSpace: React.FunctionComponent<ICreateAddressSpaceProps> = ({
  isCreateWizardOpen,
  setIsCreateWizardOpen
}) => {
  const steps = [
    {
      name: "Definition",
      component: <h1>Definition</h1>,
      backButton: "hide"
    },
    {
      name: "Review",
      component: <h1>Preview</h1>,
      nextButtonText: "Finish"
    }
  ];
  const onClose = () => {
    setIsCreateWizardOpen(!isCreateWizardOpen);
  };
  return (
    <React.Fragment>
      <Wizard
        id="create-as-wizard"
        isOpen={true}
        isFullHeight={true}
        isFullWidth={true}
        onClose={onClose}
        title="Create new Address"
        steps={steps}
        onNext={() => {
          console.log("next");
        }}
        onSave={() => {
          console.log("Save");
          setIsCreateWizardOpen(!isCreateWizardOpen);
        }}
      />
      )}
    </React.Fragment>
  );
};
