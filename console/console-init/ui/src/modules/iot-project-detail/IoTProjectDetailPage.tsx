/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { ProjectNavigation } from "./components";
import { Routes } from "./Routes";
import {
  Page,
  PageSectionVariants,
  PageSection,
  Breadcrumb,
  BreadcrumbItem
} from "@patternfly/react-core";
import { Link } from "react-router-dom";
import { useBreadcrumb, useDocumentTitle } from "use-patternfly";
import { useParams } from "react-router";

const breadcrumb = (
  <Breadcrumb>
    <BreadcrumbItem>
      <Link to={"/"}>Home</Link>
    </BreadcrumbItem>
    <BreadcrumbItem isActive={true}>IoT Project</BreadcrumbItem>
  </Breadcrumb>
);
export default function IoTProjectDetailPage() {
  useBreadcrumb(breadcrumb);
  useDocumentTitle("IoT Project Detail");
  const { sublist } = useParams();
  return (
    <>
      <PageSection variant={PageSectionVariants.light}>
        <ProjectNavigation activeItem={sublist || "detail"} />
      </PageSection>
      <PageSection>
        <Routes />
      </PageSection>
    </>
  );
}
