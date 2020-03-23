/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Wizard } from "@patternfly/react-core";
import { Configuration } from "./Configuration";
import { Review } from "./Review";
import { CREATE_ADDRESS_SPACE } from "graphql-module/queries";
import { useMutationQuery } from "hooks";
import { useStoreContext, types } from "context-state-reducer";

export const CreateAddressSpace: React.FunctionComponent<{}> = () => {
  const [addressSpaceName, setAddressSpaceName] = React.useState("");
  // State has been initialized to " " instead of null string
  // due to dropdown arrow positioning issues
  const [addressSpaceType, setAddressSpaceType] = React.useState(" ");
  const [addressSpacePlan, setAddressSpacePlan] = React.useState(" ");
  const [namespace, setNamespace] = React.useState(" ");
  const [authenticationService, setAuthenticationService] = React.useState(" ");
  const [isNameValid, setIsNameValid] = React.useState(true);

  const { state, dispatch } = useStoreContext();
  const { modalProps } = (state && state.modal) || {};
  const { onConfirm, onClose } = modalProps || {};

  const resetFormState = () => {
    setAddressSpaceType("");
    setAddressSpacePlan("");
    setAddressSpaceName("");
    setNamespace("");
    setAuthenticationService("");
  };

  const refetchQueries: string[] = ["all_address_spaces"];
  const [setQueryVariables] = useMutationQuery(
    CREATE_ADDRESS_SPACE,
    refetchQueries,
    resetFormState,
    resetFormState
  );

  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
    if (onClose) {
      onClose();
    }
  };

  const isReviewEnabled = () => {
    if (
      addressSpaceName.trim() !== "" &&
      addressSpaceType.trim() !== "" &&
      authenticationService.trim() !== "" &&
      addressSpacePlan.trim() !== "" &&
      namespace.trim() !== "" &&
      isNameValid
    ) {
      return true;
    }

    return false;
  };

  const isFinishEnabled = () => {
    if (
      addressSpaceName.trim() !== "" &&
      addressSpaceType.trim() !== "" &&
      authenticationService.trim() !== "" &&
      addressSpacePlan.trim() !== "" &&
      namespace.trim() !== "" &&
      isNameValid
    ) {
      return true;
    }

    return false;
  };

  const handleSave = async () => {
    if (isFinishEnabled()) {
      const variables = {
        as: {
          metadata: {
            name: addressSpaceName,
            namespace: namespace
          },
          spec: {
            type: addressSpaceType.toLowerCase(),
            plan: addressSpacePlan.toLowerCase(),
            authenticationService: {
              name: authenticationService
            }
          }
        }
      };
      await setQueryVariables(variables);

      onCloseDialog();
      if (onConfirm) {
        onConfirm();
      }
    }
  };

  const steps = [
    {
      name: "Configuration",
      component: (
        <Configuration
          name={addressSpaceName}
          setName={setAddressSpaceName}
          namespace={namespace}
          setNamespace={setNamespace}
          type={addressSpaceType}
          setType={setAddressSpaceType}
          plan={addressSpacePlan}
          setPlan={setAddressSpacePlan}
          authenticationService={authenticationService}
          setAuthenticationService={setAuthenticationService}
          isNameValid={isNameValid}
          setIsNameValid={setIsNameValid}
        />
      ),
      enableNext: isReviewEnabled(),
      backButton: "hide"
    },
    {
      name: "Review",
      isDisabled: true,
      component: (
        <Review
          name={addressSpaceName}
          namespace={namespace}
          type={addressSpaceType}
          plan={addressSpacePlan}
          authenticationService={authenticationService}
        />
      ),
      enableNext: isFinishEnabled(),
      canJumpTo: isReviewEnabled(),
      nextButtonText: "Finish"
    }
  ];

  return (
    <Wizard
      id="create-as-wizard"
      isOpen={true}
      isFullHeight={true}
      isFullWidth={true}
      onClose={onCloseDialog}
      title="Create an Instance"
      steps={steps}
      onNext={() => {}}
      onSave={handleSave}
    />
  );
};
