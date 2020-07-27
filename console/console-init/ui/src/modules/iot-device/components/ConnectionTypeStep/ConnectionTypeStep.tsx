import React from "react";
import { Form, Title, Radio } from "@patternfly/react-core";

interface IConnectionTypeProps {
  connectionType?: string;
  showMessage?: boolean;
  onChangeConnection: (_: any, event: any) => void;
}
const ConnectionType: React.FunctionComponent<IConnectionTypeProps> = ({
  connectionType,
  showMessage,
  onChangeConnection
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
          onChange={onChangeConnection}
          label={"Directly connected"}
          name="radio-directly-connected-option"
          id="connection-type-step-directly-connected-radio"
        />
        <Radio
          value="via-device"
          isChecked={connectionType === "via-device"}
          onChange={onChangeConnection}
          label={"Connected via other permitted devices as gateways"}
          name="radio-connected-via-gateway-option"
          id="connection-type-step-via-gateway-radio"
        />
      </Form>
    </>
  );
};

export { ConnectionType };
