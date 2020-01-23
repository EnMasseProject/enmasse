/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { Wizard } from "@patternfly/react-core";
import { AddressDefinitaion } from "src/Pages/CreateAddress/CreateAddressDefinition";
import { PreviewAddress } from "./PreviewAddress";
import { useApolloClient } from "@apollo/react-hooks";
import { CREATE_ADDRESS } from "src/Queries/Queries";
import {
  IDropdown,
  IDropdownOption
} from "src/Components/Common/FilterDropdown";
import { type } from "os";
interface ICreateAddressProps {
  name: string;
  namespace: string;
  addressSpace: string;
  addressSpacePlan: string;
  addressSpaceType: string;
  refetch?: () => void;
  isCreateWizardOpen: boolean;
  setIsCreateWizardOpen: (value: boolean) => void;
}
export const CreateAddressPage: React.FunctionComponent<ICreateAddressProps> = ({
  name,
  namespace,
  addressSpace,
  addressSpacePlan,
  addressSpaceType,
  refetch,
  isCreateWizardOpen,
  setIsCreateWizardOpen
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
  const handleAddressChange = (name: string) => {
    setAddressName(name);
  };

  const handleSave = async () => {
    if (
      addressSpace &&
      addressName.trim() !== "" &&
      plan.trim() !== "" &&
      addressType.trim() !== "" &&
        (
          (addressType === "subscription") === 
            (topic.trim() !== "")
        )
    ) {
      const getVariables = () => {
        let variable:any = {
          ObjectMeta: {
            Name: addressSpace + "." + addressName,
            Namespace: namespace
          },
          Spec: {
            Type: addressType.toLowerCase(),
            Plan: plan,
            Address: addressName,
            AddressSpace: addressSpace
          }
        };
        if (addressType && addressType.trim().toLowerCase() === "subscription")
          variable.Spec.Topic = topic;
        return variable;
      };
      const data = await client.mutate({
        mutation: CREATE_ADDRESS,
        variables: {
          a: getVariables()
        }
      });
      if (data.data) {
        setIsCreateWizardOpen(false);
        setAddressType("");
        setPlan("");
      }
      if (refetch) refetch();
    }
  };
  const steps = [
    {
      name: "Definition",
      component: (
        <AddressDefinitaion
          addressspaceName={name}
          namespace={namespace}
          addressSpacePlan={addressSpacePlan}
          addressName={addressName}
          handleAddressChange={handleAddressChange}
          type={addressType}
          setType={setAddressType}
          plan={plan}
          setPlan={setPlan}
          topic={topic}
          setTopic={setTopic}
          typeOptions={addressTypes}
          setTypeOptions={setAddressTypes}
          planOptions={addressPlans}
          setPlanOptions={setAddressPlans}
          topicsForSubscription={topicsForSubscription}
          setTopicForSubscripitons={setTopicForSubscription}
        />
      ),
      enableNext : (
        addressName.trim() !== "" &&
        plan.trim() !== "" &&
        addressType.trim() !== "" &&
        (
          (addressType === "subscription") === 
            (topic.trim() !== "")
        )
      ),
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
      enableNext : (
        addressName.trim() !== "" &&
        plan.trim() !== "" &&
        addressType.trim() !== "" &&
        (
          (addressType === "subscription") === 
            (topic.trim() !== "")
        )
      ),
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
      onNext={() => {
        console.log("next");
      }}
      onSave={handleSave}
    />
  );
};
