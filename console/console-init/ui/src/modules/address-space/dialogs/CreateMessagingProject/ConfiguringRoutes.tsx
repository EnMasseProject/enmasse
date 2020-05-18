import React from "react";
import { IMessagingProject } from "./CreateMessagingProject";

interface IConfiguringRoutes {
  projectDetail: IMessagingProject;
  setProjectDetail: (projectDetail: IMessagingProject) => void;
}

const ConfiguringRoutes: React.FunctionComponent<IConfiguringRoutes> = ({
  projectDetail,
  setProjectDetail
}) => {
  return <p>Configuration of Routes</p>;
};

export { ConfiguringRoutes };
