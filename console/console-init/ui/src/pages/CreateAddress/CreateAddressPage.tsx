/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { Wizard } from "@patternfly/react-core";
import { AddressDefinition } from "pages/CreateAddress/CreateAddressDefinition";
import { PreviewAddress } from "./PreviewAddress";
import { useApolloClient } from "@apollo/react-hooks";
import { CREATE_ADDRESS } from "queries";
import { IDropdownOption } from "components/common/FilterDropdown";
import { messagingAddressNameRegexp } from "types/Configs";
import { useMutationQuery } from "hooks";

interface ICreateAddressProps {
  name: string;
  namespace: string;
  addressSpace: string;
  addressSpacePlan: string;
  addressSpaceType: string;
  isCreateWizardOpen: boolean;
  setIsCreateWizardOpen: (value: boolean) => void;
  setOnCreationRefetch?: (value: boolean) => void;
}
export const CreateAddressPage: React.FunctionComponent<ICreateAddressProps> = ({
  name,
  namespace,
  addressSpace,
  addressSpacePlan,
  addressSpaceType,
  isCreateWizardOpen,
  setIsCreateWizardOpen,
  setOnCreationRefetch
}) => {
  const [addressName, setAddressName] = React.useState("");
  const [addressType, setAddressType] = React.useState(" ");
  const [plan, setPlan] = React.useState(" ");
  const [topic, setTopic] = React.useState(" ");
  const client = useApolloClient();
  const [addressTypes, setAddressTypes] = React.useState<IDropdownOption[]>([]);
  const [addressPlans, setAddressPlans] = React.useState<IDropdownOption[]>([]);
  const [topicsForSubscription, setTopicForSubscription] = React.useState<
    IDropdownOption[]
  >([]);
  const [isNameValid, setIsNameValid] = React.useState(true);

  const resetFormState = () => {
    setIsCreateWizardOpen(false);
    setAddressType("");
    setPlan("");
    setOnCreationRefetch && setOnCreationRefetch(true);
  };

  const [setAddressQueryVariables] = useMutationQuery(
    CREATE_ADDRESS,
    resetFormState,
    resetFormState
  );

  const handleAddressChange = (name: string) => {
    setAddressName(name);
    !messagingAddressNameRegexp.test(name)
      ? setIsNameValid(false)
      : setIsNameValid(true);
  };

  const isReviewEnabled = () => {
    if (
      addressName.trim() !== "" &&
      isNameValid &&
      plan.trim() !== "" &&
      addressType.trim() !== "" &&
      (addressType === "subscription") === (topic.trim() !== "")
    ) {
      return true;
    }

    return false;
  };

  const isFinishEnabled = () => {
    if (
      addressName.trim() !== "" &&
      plan.trim() !== "" &&
      addressType.trim() !== "" &&
      (addressType === "subscription") === (topic.trim() !== "") &&
      isNameValid
    ) {
      return true;
    }

    return false;
  };

  const handleSave = async () => {
    if (addressSpace && isFinishEnabled()) {
      const getVariables = () => {
        let variable: any = {
          metadata: {
            namespace: namespace
          },
          spec: {
            type: addressType.toLowerCase(),
            plan: plan,
            address: addressName,
            addressSpace: addressSpace
          }
        };
        if (addressType && addressType.trim().toLowerCase() === "subscription")
          variable.spec.topic = topic;
        return variable;
      };
      const variables = {
        a: getVariables(),
        as: addressSpace
      };
      setAddressQueryVariables(variables);
    }
  };
  const steps = [
    {
      name: "Definition",
      component: (
        <AddressDefinition
          addressspaceName={name}
          namespace={namespace}
          addressSpacePlan={addressSpacePlan}
          addressName={addressName}
          handleAddressChange={handleAddressChange}
          isNameValid={isNameValid}
          type={addressType}
          setType={setAddressType}
          plan={plan}
          setPlan={setPlan}
          topic={topic}
          addressSpaceType={addressSpaceType}
          setTopic={setTopic}
          typeOptions={addressTypes}
          setTypeOptions={setAddressTypes}
          planOptions={addressPlans}
          setPlanOptions={setAddressPlans}
          topicsForSubscription={topicsForSubscription}
          setTopicForSubscripitons={setTopicForSubscription}
        />
      ),
      enableNext: isReviewEnabled(),
      backButton: "hide"
    },
    {
      name: "Review",
      component: (
        <PreviewAddress
          name={addressName}
          plan={plan}
          type={addressType}
          topic={topic}
          namespace={namespace || ""}
          addressspace={addressSpace}
        />
      ),
      enableNext: isFinishEnabled(),
      canJumpTo: isReviewEnabled(),
      nextButtonText: "Finish"
    }
  ];
  return (
    <Wizard
      id="create-addr-wizard"
      isOpen={isCreateWizardOpen}
      isFullHeight={true}
      isFullWidth={true}
      onClose={() => {
        setIsCreateWizardOpen(!isCreateWizardOpen);
        setAddressName("");
      }}
      title="Create new Address"
      steps={steps}
      onNext={() => {}}
      onSave={handleSave}
    />
  );
};
