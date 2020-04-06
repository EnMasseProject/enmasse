import { Wizard } from "@patternfly/react-core";
import React from "react";
import { DeviceInformation } from "modules/device/components/DeviceInformation";

export const CreateDevice: React.FunctionComponent<{}> = () => {
  const steps = [
    { name: "Step 1", component: <DeviceInformation /> },
    { name: "Step 2", component: <p>Step 2</p> },
    { name: "Step 3", component: <p>Step 3</p> },
    { name: "Step 4", component: <p>Step 4</p> },
    {
      name: "Final Step",
      component: <p>Final Step</p>,
      hideCancelButton: true,
      nextButtonText: "Close"
    }
  ];
  return (
    <Wizard isInPage onClose={() => console.log("closed")} steps={steps} />
  );
};
