import * as React from "react";
import {
  IAddressSpaceProps,
  AddressSpaceHeader
} from "src/Components/AddressSpace/AddressSpaceHeader";
import { render } from "@testing-library/react";

describe("Address Space Detail", () => {
  test("it renders address space headers at top", () => {
    const props: IAddressSpaceProps = {
      name: "jBoss",
      namespace: "devops_jbosstest1",
      createdOn: "2 days ago",
      type: "Brokered",
      onDownload: () => {},
      onDelete: () => {}
    };

    const { getByText } = render(<AddressSpaceHeader {...props} />);

    getByText(props.name);
    getByText(props.namespace);
    getByText(props.type);
    getByText(props.createdOn);
  });
});
