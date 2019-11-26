import * as React from "react";
import {
  PageSection,
  PageSectionVariants,
  BreadcrumbItem,
  Breadcrumb
} from "@patternfly/react-core";
import { useBreadcrumb, useA11yRouteChange } from "use-patternfly";
import { Link } from "react-router-dom";
import { useParams } from "react-router";

export default function AddressDetailPage() {
  const { namespace, name, type } = useParams();
  const breadcrumb = React.useMemo(
    () => (
      <Breadcrumb>
        <BreadcrumbItem>
          <Link to={"/"}>Home</Link>
        </BreadcrumbItem>
        <BreadcrumbItem>
          <Link to={`/address-spaces/${namespace}/${name}/${type}/addresses`}>
            {name}
          </Link>
        </BreadcrumbItem>
        <BreadcrumbItem isActive={true}>Address</BreadcrumbItem>
      </Breadcrumb>
    ),
    [name, namespace]
  );

  useBreadcrumb(breadcrumb);

  useA11yRouteChange();
  // useBreadcrumb(breadcrumb);
  return (
    <>
      <PageSection variant={PageSectionVariants.light}>
        <h1>Address Detail Page</h1>
      </PageSection>
    </>
  );
}
