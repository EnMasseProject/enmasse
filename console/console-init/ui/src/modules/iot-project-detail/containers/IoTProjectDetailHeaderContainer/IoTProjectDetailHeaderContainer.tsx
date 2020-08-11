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
import { IIotProjectType } from "schema";

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

  const queryResolver = `
    total
    objects{
      ... on IoTProject_iot_enmasse_io_v1alpha1 {
        kind
        metadata {
          name
          namespace
          creationTimestamp
        }
        iotStatus: status{
          phase
          phaseReason 
        }
        spec{
          tenantId
          addresses{
            Telemetry{
              name
              plan
              type
            }
            Event{
              name
              plan
              type
            }
            Command{
              name
              plan
              type
            }
          }
          configuration
        }
        endpoints{
          name
          url
          host
          port
          tls
        }
        enabled
      }
    }`;
  const { data } = useQuery<IIoTProjectsResponse>(
    RETURN_IOT_PROJECTS({ projectname }, queryResolver)
  );

  const objects: IIotProjectType[] = [];

  const { allProjects } = data || {
    allProjects: { objects: objects }
  };
  const { metadata, iotStatus, enabled } = allProjects?.objects[0] || {};

  const namespace = metadata?.namespace;

  const onDeleteProject = () => {
    if (projectname && namespace) {
      const queryVariable = {
        a: [
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
        confirmButtonLabel: "Delete"
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

  const handleProjectStatus = (checked: boolean) => {
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
        confirmButtonLabel: title
      }
    });
  };

  return (
    <IoTProjectDetailHeader
      projectName={metadata?.name}
      timeCreated={metadata?.creationTimestamp}
      status={iotStatus?.phase}
      isEnabled={enabled}
      changeStatus={handleProjectStatus}
      onDelete={handleDelete}
    />
  );
};
