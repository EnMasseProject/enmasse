import React from "react";
import { IMessagingProject } from "./CreateMessagingProject";

interface IConfiguringEndPoint {
  projectDetail: IMessagingProject;
  setProjectDetail: (projectDetail: IMessagingProject) => void;
}

const ConfiguringEndpoint: React.FunctionComponent<IConfiguringEndPoint> = ({
  projectDetail,
  setProjectDetail
}) => {
  return <p>Configuration of Endpoint</p>;
};

export { ConfiguringEndpoint };
