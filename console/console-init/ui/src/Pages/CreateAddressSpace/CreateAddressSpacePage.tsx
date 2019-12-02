import * as React from "react";
import { Button, Wizard } from "@patternfly/react-core";

export const CreateAddressSpace: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);

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
  return (
    <React.Fragment>
      <Button
        variant="primary"
        onClick={() => {
          setIsOpen(!isOpen);
        }}
      >
        Create Address
      </Button>
      {isOpen && (
        <Wizard
          isOpen={isOpen}
          isFullHeight={true}
          isFullWidth={true}
          onClose={() => {
            setIsOpen(!isOpen);
          }}
          title="Create new Address"
          steps={steps}
          onNext={() => {
            console.log("next");
          }}
          //   onSave={handleSave}
        />
      )}
    </React.Fragment>
  );
};
