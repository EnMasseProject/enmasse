import React from "react";
import { Form, Title, FormGroup, Radio } from "@patternfly/react-core";

interface IConnectionTypeProps {
  connectionType?: string;
  showMessage?: boolean;
  onConnectionChange: (_: any, event: any) => void;
}
const ConnectionType: React.FunctionComponent<IConnectionTypeProps> = ({
  connectionType,
  showMessage,
  onConnectionChange
}) => {
  return (
    <>
      <Form>
        <Title headingLevel="h4" size="xl">
          Choose the way to connect your device to the cloud
        </Title>
        <Radio
          value="directly"
          isChecked={connectionType === "directly"}
          onChange={onConnectionChange}
          label={"Directly connected"}
          name="radio-directly-connected-option"
          id="radio-directly-connected-option"
        />
        <Radio
          value="via-device"
          isChecked={connectionType === "via-device"}
          onChange={onConnectionChange}
          label={"Connected via other permitted devices as gateways"}
          name="radio-connected-via-gateway-option"
          id="radio-connected-via-gateway-option"
        />
      </Form>
    </>
  );
};

export { ConnectionType };
