import React from "react";
import { IMessagingProject } from "./CreateMessagingProject";

interface IConfiguringCertificates {
  projectDetail: IMessagingProject;
  setProjectDetail: (projectDetail: IMessagingProject) => void;
}

const ConfiguringCertificates: React.FunctionComponent<IConfiguringCertificates> = ({
  projectDetail,
  setProjectDetail
}) => {
  return <p>Configuration of Certificates</p>;
};

export { ConfiguringCertificates };
