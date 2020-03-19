/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Wizard } from "@patternfly/react-core";
import { useMutationQuery } from "hooks";
import { AddressDefinition } from "modules/address/dialogs/CreateAddress/Configuration";
import { PreviewAddress } from "./Preview";
import { CREATE_ADDRESS } from "graphql-module/queries";
import { IDropdownOption } from "components/common/FilterDropdown";
import { messagingAddressNameRegexp } from "types/Configs";

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
  const [addressName, setAddressName] = useState("");
  const [addressType, setAddressType] = useState(" ");
  const [plan, setPlan] = useState(" ");
  const [topic, setTopic] = useState(" ");
  const [addressTypes, setAddressTypes] = useState<IDropdownOption[]>([]);
  const [addressPlans, setAddressPlans] = useState<IDropdownOption[]>([]);
  const [topicsForSubscription, setTopicForSubscription] = useState<
    IDropdownOption[]
  >([]);
  const [isNameValid, setIsNameValid] = useState(true);

  const resetFormState = () => {
    setIsCreateWizardOpen(false);
    setAddressType("");
    setPlan("");
    setOnCreationRefetch && setOnCreationRefetch(true);
  };

  const [setAddressQueryVariables] = useMutationQuery(
    CREATE_ADDRESS,
    undefined,
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
            address: addressName
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
