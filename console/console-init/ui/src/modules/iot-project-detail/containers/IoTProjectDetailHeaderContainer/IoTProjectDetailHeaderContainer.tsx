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
import { useHistory, useParams } from "react-router";

export const IoTProjectDetailHeaderContainer: React.FC = () => {
  const { dispatch } = useStoreContext();
  const { projectname } = useParams();
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
    RETURN_IOT_PROJECTS({ projectname })
  );

  const { allProjects } = data || {
    allProjects: { iotProjects: [] }
  };

  const { spec, metadata, status, enabled } =
    allProjects?.iotProjects?.[0] || {};

  const namespace = metadata?.namespace;

  const onDeleteProject = () => {
    if (projectname && namespace) {
      const queryVariable = {
        as: [
          {
            name: projectname,
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
        selectedItems: [projectname],
        data: projectname,
        onConfirm: onDeleteProject,
        option: "Delete",
        detail: `Are you sure you want to delete this iot project: ${projectname} ?`,
        header: "Delete this IoT Project ?",
        confirmButtonLabel: "Delete",
        iconType: "danger"
      }
    });
  };

  const handleChangeStatus = async (checked: boolean) => {
    const queryVariable = {
      a: [
        {
          name: projectname,
          namespace: namespace
        }
      ],
      status: checked
    };
    await setToggleIoTProjectQueryVariables(queryVariable);
  };

  const handleChangeEnableSwitch = (checked: boolean) => {
    const title = checked ? "Enable" : "Disable";
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_PROJECT,
      modalProps: {
        selectedItems: [projectname],
        data: projectname,
        onConfirm: () => handleChangeStatus(checked),
        option: title,
        detail: `Are you sure you want to ${title} this iot project: ${projectname} ?`,
        header: `${title} this IoT Project ?`,
        confirmButtonLabel: title,
        iconType: "danger"
      }
    });
  };

  return (
    <IoTProjectDetailHeader
      projectName={metadata?.name}
      timeCreated={metadata?.creationTimestamp}
      status={status?.phase}
      isEnabled={enabled}
      changeStatus={handleChangeEnableSwitch}
      onDelete={handleDelete}
    />
  );
};
