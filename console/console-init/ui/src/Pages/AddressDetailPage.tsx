import * as React from "react";
import { ConnectionDetailHeader } from "src/Components/ConnectionDetail/ConnectionDetailHeader";
import { PageSection, PageSectionVariants } from "@patternfly/react-core";
import {
  ConnectionList,
  IConnection
} from "src/Components/AddressSpace/ConnectionList";

export default function AddressDetailPage() {
  return (
    <>
      <PageSection variant={PageSectionVariants.light}>
        <h1>Address Detail Page</h1>
      </PageSection>
    </>
  );
}
