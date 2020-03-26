/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import {
  IAddressDetailHeaderProps,
  AddressDetailHeader
} from "./AddressDetailHeader";

describe("Address Detail Header", () => {
  test("it renders address space headers at top", () => {
    const props: IAddressDetailHeaderProps = {
      name: "newqueue",
      topic: null,
      type: "queue",
      plan: "Small",
      storedMessages: 1,
      onDelete: () => {},
      onEdit: () => {},
      onPurge: () => {}
    };

    const { getByText } = render(<AddressDetailHeader {...props} />);

    getByText(props.name);
    getByText(props.plan);
    getByText(String(props.storedMessages));
  });
});
