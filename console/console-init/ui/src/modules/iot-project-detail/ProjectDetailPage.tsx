import React from "react";
import {
  PageSection,
  PageSectionVariants,
  Breadcrumb,
  BreadcrumbItem
} from "@patternfly/react-core";
import { IoTProjectDetailHeader, ProjectNavigation } from "./components";
import { useParams } from "react-router";
import { Routes } from "./Routes";
import { Link } from "react-router-dom";
import { useBreadcrumb, useDocumentTitle } from "use-patternfly";

const breadcrumb = (
  <Breadcrumb>
    <BreadcrumbItem>
      <Link to={"/"}>Home</Link>
    </BreadcrumbItem>
    <BreadcrumbItem isActive={true}>IoT Project</BreadcrumbItem>
  </Breadcrumb>
);
export default function ProjectDetailPage() {
  useBreadcrumb(breadcrumb);
  useDocumentTitle("IoT Project Detail");
  const { projectname, sublist } = useParams();

  const handleChangeEnabled = (isEnabled: boolean) => {};

  const handleEdit = (name: string) => {};

  const handleDelete = (name: string) => {};

  return (
    <>
      <PageSection variant={PageSectionVariants.light}>
        <IoTProjectDetailHeader
          projectName={projectname}
          type="Managed"
          status="Ready"
          isEnabled={true}
          changeEnable={handleChangeEnabled}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
        <ProjectNavigation activeItem={sublist || "detail"} />
      </PageSection>
      <PageSection>
        <Routes />
      </PageSection>
    </>
  );
}
