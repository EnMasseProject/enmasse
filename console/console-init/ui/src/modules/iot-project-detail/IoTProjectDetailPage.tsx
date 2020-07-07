import React from "react";
import {
  PageSection,
  PageSectionVariants,
  Breadcrumb,
  BreadcrumbItem
} from "@patternfly/react-core";
import { ProjectNavigation } from "modules/iot-project-detail/components";
import { useParams } from "react-router";
import { Routes } from "./Routes";
import { Link } from "react-router-dom";
import { useBreadcrumb, useDocumentTitle } from "use-patternfly";
import { IoTProjectDetailHeaderContainer } from "modules/iot-project-detail/containers";

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
        <IoTProjectDetailHeaderContainer />
        <ProjectNavigation activeItem={sublist || "detail"} />
      </PageSection>
      <PageSection>
        <Routes />
      </PageSection>
    </>
  );
}
