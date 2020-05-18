/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Alert, AlertProps } from "@patternfly/react-core";

export interface IDeviceListAlertProps
  extends Pick<AlertProps, "variant" | "isInline" | "id"> {
  visible: boolean;
  title: string;
  description: string;
}

export const DeviceListAlert: React.FunctionComponent<IDeviceListAlertProps> = ({
  visible,
  isInline,
  id,
  variant,
  title,
  description
}) => {
  return (
    <React.Fragment>
      {visible && (
        <Alert variant={variant} title={title} id={id} isInline={isInline}>
          {description}
        </Alert>
      )}
    </React.Fragment>
  );
};
