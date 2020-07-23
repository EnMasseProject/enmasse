/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useQuery } from "@apollo/react-hooks";
import {
  RETURN_ADDRESS_SPACE_PLANS,
  RETURN_FILTERED_AUTHENTICATION_SERVICES
} from "graphql-module/queries";
import { IAddressSpacePlans } from "modules/address-space";
import { Loading } from "use-patternfly";
import { useStoreContext, types } from "context-state-reducer";
import { useMutationQuery } from "hooks";
import { EDIT_ADDRESS_SPACE } from "graphql-module/queries";
import { IAddressSpaceAuthService } from "modules/address-space/dialogs";
import {
  IProject,
  IPlanOption,
  IAuthenticationServiceOptions
} from "modules/project/components";
import { EditMsgProject } from "modules/messaging-project";

export interface IAddressSpaceAuthServiceResponse {
  addressSpaceSchema_v2: IAddressSpaceAuthService[];
}

export const EditMsgProjectContainer: React.FunctionComponent<{}> = () => {
  const { state, dispatch } = useStoreContext();
  const { modalProps } = (state && state.modal) || {};
  const { onConfirm, onClose } = modalProps || {};

  const [msgProject, setMsgProject] = useState<IProject>(modalProps.project);
  const refetchQueries: string[] = ["allProjects"];
  const [setEditMsgProjectQueryVariables] = useMutationQuery(
    EDIT_ADDRESS_SPACE,
    refetchQueries
  );

  const { loading, data } = useQuery<IAddressSpacePlans>(
    RETURN_ADDRESS_SPACE_PLANS
  );

  const authServices = useQuery<IAddressSpaceAuthServiceResponse>(
    RETURN_FILTERED_AUTHENTICATION_SERVICES,
    {
      variables: {
        t: msgProject?.type
      }
    }
  ).data || { addressSpaceSchema_v2: [] };

  if (loading) return <Loading />;

  const { addressSpacePlans } = data || {
    addressSpacePlans: []
  };

  const getPlanOptions = () => {
    let planOptions: IPlanOption[] = [];
    if (msgProject.type) {
      planOptions =
        addressSpacePlans
          .filter(plan => plan.spec.addressSpaceType === msgProject.type)
          .map(plan => {
            return {
              value: plan.metadata.name,
              label: plan.metadata.name
            };
          })
          .filter(plan => plan !== undefined) || [];
    }
    return planOptions;
  };

  const getAuthServiceOptions = () => {
    let authServiceOptions: IAuthenticationServiceOptions[] = [];
    if (authServices.addressSpaceSchema_v2[0])
      authServiceOptions = authServices.addressSpaceSchema_v2[0].spec.authenticationServices.map(
        authService => {
          return {
            value: authService,
            label: authService
          };
        }
      );
    return authServiceOptions;
  };

  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
    if (onClose) {
      onClose();
    }
  };

  const onPlanChange = (plan: string) => {
    if (msgProject) {
      msgProject.plan = plan;
      setMsgProject({ ...msgProject });
    }
  };

  const onAuthServiceChange = (authService: string) => {
    if (msgProject) {
      msgProject.authService = authService;
      setMsgProject({ ...msgProject });
    }
  };

  const onConfirmDialog = async () => {
    if (msgProject) {
      const variables = {
        a: {
          name: msgProject.name,
          namespace: msgProject.namespace
        },
        jsonPatch:
          '[{"op":"replace","path":"/spec/plan","value":"' +
          msgProject.plan +
          '"},' +
          '{"op":"replace","path":"/spec/authenticationService/name","value":"' +
          msgProject.authService +
          '"}' +
          "]",
        patchType: "application/json-patch+json"
      };
      await setEditMsgProjectQueryVariables(variables);

      onCloseDialog();
      if (onConfirm) {
        onConfirm();
      }
    }
  };

  return (
    <EditMsgProject
      onConfirmDialog={onConfirmDialog}
      onCloseDialog={onCloseDialog}
      onPlanChange={onPlanChange}
      onAuthServiceChange={onAuthServiceChange}
      authServiceOptions={getAuthServiceOptions()}
      planOptions={getPlanOptions()}
      project={msgProject}
    />
  );
};
