/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { Wizard } from "@patternfly/react-core";
import { AddressSpaceConfiguration } from "pages/CreateAddressSpace/CreateAddressSpaceConfiguration";
import { ReviewAddressSpace } from "pages/CreateAddressSpace/ReviewAddressSpace";
import { CREATE_ADDRESS_SPACE } from "queries";
import { useMutationQuery } from "hooks";

interface ICreateAddressSpaceProps {
  isCreateWizardOpen: boolean;
  setIsCreateWizardOpen: (value: boolean) => void;
  setOnCreationRefetch?: (value: boolean) => void;
}
export const CreateAddressSpace: React.FunctionComponent<ICreateAddressSpaceProps> = ({
  isCreateWizardOpen,
  setIsCreateWizardOpen,
  setOnCreationRefetch
}) => {
  const [addressSpaceName, setAddressSpaceName] = React.useState("");
  // State has been initialized to " " instead of null string
  // due to dropdown arrow positioning issues
  const [addressSpaceType, setAddressSpaceType] = React.useState(" ");
  const [addressSpacePlan, setAddressSpacePlan] = React.useState(" ");
  const [namespace, setNamespace] = React.useState(" ");
  const [authenticationService, setAuthenticationService] = React.useState(" ");
  const [isNameValid, setIsNameValid] = React.useState(true);

  const resetFormState = () => {
    setIsCreateWizardOpen(false);
    setAddressSpaceType("");
    setAddressSpacePlan("");
    setAddressSpaceName("");
    setNamespace("");
    setAuthenticationService("");
    if (setOnCreationRefetch) setOnCreationRefetch(true);
  };
  const [setQueryVariables] = useMutationQuery(
    CREATE_ADDRESS_SPACE,
    resetFormState,
    resetFormState
  );

  const handleSave = () => {
    if (
      addressSpaceName &&
      authenticationService &&
      addressSpaceType &&
      addressSpacePlan &&
      namespace
    ) {
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
      setQueryVariables(variables);
    }
  };

  const steps = [
    {
      name: "Configuration",
      component: (
        <AddressSpaceConfiguration
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
      enableNext:
        addressSpaceName.trim() !== "" &&
        addressSpaceType.trim() !== "" &&
        authenticationService.trim() !== "" &&
        addressSpacePlan.trim() !== "" &&
        namespace.trim() !== "",
      backButton: "hide"
    },
    {
      name: "Review",
      isDisabled: true,
      component: (
        <ReviewAddressSpace
          name={addressSpaceName}
          namespace={namespace}
          type={addressSpaceType}
          plan={addressSpacePlan}
          authenticationService={authenticationService}
        />
      ),
      enableNext:
        addressSpaceName.trim() !== "" &&
        addressSpaceType.trim() !== "" &&
        authenticationService.trim() !== "" &&
        addressSpacePlan.trim() !== "" &&
        namespace.trim() !== "" &&
        isNameValid,
      canJumpTo:
        addressSpaceName.trim() !== "" &&
        addressSpaceType.trim() !== "" &&
        authenticationService.trim() !== "" &&
        addressSpacePlan.trim() !== "" &&
        namespace.trim() !== "",
      nextButtonText: "Finish"
    }
  ];
  const onClose = () => {
    setIsCreateWizardOpen(!isCreateWizardOpen);
  };

  return (
    <React.Fragment>
      <Wizard
        id="create-as-wizard"
        isOpen={true}
        isFullHeight={true}
        isFullWidth={true}
        onClose={onClose}
        title="Create an Instance"
        steps={steps}
        onNext={() => {}}
        onSave={handleSave}
      />
      )}
    </React.Fragment>
  );
};
