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
import {
  IAddressSpacePlans,
  IAddressSpaceAuthServiceResponse,
  EditAddressSpace
} from "modules/address-space";
import { Loading } from "use-patternfly";
import { useStoreContext, types } from "context-state-reducer";
import { useMutationQuery } from "hooks";
import { EDIT_ADDRESS_SPACE } from "graphql-module/queries";

export const EditAddressSpaceContainer: React.FunctionComponent<{}> = () => {
  const { state, dispatch } = useStoreContext();
  let planOptions: any[] = [];
  let authServiceOptions: any[] = [];

  const { modalProps } = (state && state.modal) || {};
  const { onConfirm, onClose } = modalProps || {};

  const [addressSpace, setAddressSpace] = useState(modalProps.addressSpace);
  const refetchQueries: string[] = ["all_address_spaces"];
  const [setEditAddressSpaceQueryVariables] = useMutationQuery(
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
        t: addressSpace.type
      }
    }
  ).data || { addressSpaceSchema_v2: [] };

  if (loading) return <Loading />;

  const { addressSpacePlans } = data || {
    addressSpacePlans: []
  };

  if (addressSpace.type) {
    planOptions =
      addressSpacePlans
        .map(plan => {
          if (plan.spec.addressSpaceType === addressSpace.type) {
            return {
              value: plan.metadata.name,
              label: plan.metadata.name
            };
          }
        })
        .filter(plan => plan !== undefined) || [];
  }

  if (authServices.addressSpaceSchema_v2[0])
    authServiceOptions = authServices.addressSpaceSchema_v2[0].spec.authenticationServices.map(
      authService => {
        return {
          value: authService,
          label: authService
        };
      }
    );

  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
    if (onClose) {
      onClose();
    }
  };

  const onPlanChange = (plan: string) => {
    if (addressSpace) {
      addressSpace.planValue = plan;
      setAddressSpace({ ...addressSpace });
    }
  };

  const onAuthServiceChange = (authService: string) => {
    if (addressSpace) {
      addressSpace.authenticationService = authService;
      setAddressSpace({ ...addressSpace });
    }
  };

  const onConfirmDialog = async () => {
    if (addressSpace) {
      const variables = {
        a: {
          name: addressSpace.name,
          namespace: addressSpace.nameSpace
        },
        jsonPatch:
          '[{"op":"replace","path":"/spec/plan","value":"' +
          addressSpace.planValue +
          '"},' +
          '{"op":"replace","path":"/spec/authenticationService/name","value":"' +
          addressSpace.authenticationService +
          '"}' +
          "]",
        patchType: "application/json-patch+json"
      };
      await setEditAddressSpaceQueryVariables(variables);

      onCloseDialog();
      if (onConfirm) {
        onConfirm();
      }
    }
  };

  return (
    <EditAddressSpace
      onConfirmDialog={onConfirmDialog}
      onCloseDialog={onCloseDialog}
      onPlanChange={onPlanChange}
      onAuthServiceChange={onAuthServiceChange}
      authServiceOptions={authServiceOptions}
      planOptions={planOptions}
      addressSpace={addressSpace}
    />
  );
};
