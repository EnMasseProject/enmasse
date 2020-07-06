/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { IoTProjectDetailHeader } from "modules/iot-project-detail/components";
import { useQuery } from "@apollo/react-hooks";
import {
  RETURN_IOT_PROJECTS,
  DELETE_IOT_PROJECT,
  TOGGLE_IOT_PROJECTS_STATUS
} from "graphql-module/queries/iot_project";
import { IIoTProjectsResponse } from "schema/iot_project";
import { useMutationQuery } from "hooks";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import { useHistory } from "react-router";

interface IIoTProjectDetailHeaderContainerProps {
  projectName: string;
}

export const IoTProjectDetailHeaderContainer: React.FC<IIoTProjectDetailHeaderContainerProps> = ({
  projectName
}) => {
  const { dispatch } = useStoreContext();
  const history = useHistory();

  const redirectToIoTProjectList = () => {
    history.push("/projects");
  };

  const [setDeleteIoTProjectQueryVariables] = useMutationQuery(
    DELETE_IOT_PROJECT,
    undefined,
    undefined,
    redirectToIoTProjectList
  );

  const [
    setToggleIoTProjectQueryVariables
  ] = useMutationQuery(TOGGLE_IOT_PROJECTS_STATUS, ["allProjects"]);

  const { data } = useQuery<IIoTProjectsResponse>(
    RETURN_IOT_PROJECTS({ projectName })
  );

  const { allProjects } = data || {
    allProjects: { iotProjects: [] }
  };

  const { spec, metadata, status, enabled } =
    allProjects?.iotProjects?.[0] || {};

  const namespace = metadata?.namespace;

  const onDeleteProject = () => {
    if (projectName && namespace) {
      const queryVariable = {
        as: [
          {
            name: projectName,
            namespace: namespace
          }
        ]
      };
      setDeleteIoTProjectQueryVariables(queryVariable);
    }
  };

  const handleDelete = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_PROJECT,
      modalProps: {
        selectedItems: [projectName],
        data: projectName,
        onConfirm: onDeleteProject,
        option: "Delete",
        detail: `Are you sure you want to delete this iot project: ${projectName} ?`,
        header: "Delete this IoT Project ?",
        confirmButtonLabel: "Delete",
        iconType: "danger"
      }
    });
  };

  const handleChangeEnabled = (checked: boolean) => {
    const queryVariable = {
      a: [
        {
          name: projectName,
          namespace: namespace
        }
      ],
      status: checked
    };
    setToggleIoTProjectQueryVariables(queryVariable);
  };

  return (
    <IoTProjectDetailHeader
      projectName={metadata?.name}
      type={spec?.downstreamStrategyType}
      status={status?.phase}
      isEnabled={enabled}
      changeEnable={handleChangeEnabled}
      onDelete={handleDelete}
    />
  );
};
