/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  IMessagingProjectHeaderProps,
  MessagingProjectHeader
} from "./MessagingProjectHeader";
import { render } from "@testing-library/react";

describe("<MessagingProjectHeader />", () => {
  it("should render messaging project header at top", () => {
    const props: IMessagingProjectHeaderProps = {
      name: "jBoss",
      namespace: "devops_jbosstest1",
      createdOn: "2019-11-25T05:24:05.755Z",
      type: "Standard",
      onDownload: () => {},
      onDelete: () => {},
      onEdit: () => {}
    };

    const { getByText } = render(<MessagingProjectHeader {...props} />);

    getByText(props.name);
    getByText(props.namespace);
    getByText(props.type);
    // getByText(props.createdOn);
  });
});
