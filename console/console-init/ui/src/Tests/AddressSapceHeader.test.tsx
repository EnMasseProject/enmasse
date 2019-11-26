import * as React from "react";
import {
  IAddressSpaceHeaderProps,
  AddressSpaceHeader
} from "src/Components/AddressSpace/AddressSpaceHeader";
import { render } from "@testing-library/react";

describe("Address Space Detail", () => {
  test("it renders address space headers at top", () => {
    const props: IAddressSpaceHeaderProps = {
      name: "jBoss",
      namespace: "devops_jbosstest1",
      createdOn: "2019-11-25T05:24:05.755Z",
      type: "Standard",
      onDownload: () => {},
      onDelete: () => {}
    };

    const { getByText } = render(<AddressSpaceHeader {...props} />);

    getByText(props.name);
    getByText(props.namespace);
    getByText(props.type);
    // getByText(props.createdOn);
  });
});
