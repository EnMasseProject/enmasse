/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Wizard } from "@patternfly/react-core";
import { useQuery } from "@apollo/react-hooks";
import { useMutationQuery } from "hooks";
import { AddressDefinitionContainer } from "modules/address/containers";
import { PreviewAddress } from "./Preview";
import {
  CREATE_ADDRESS,
  RETURN_ADDRESS_SPACE_DETAIL
} from "graphql-module/queries";
import { IDropdownOption } from "components";
import { useStoreContext, types } from "context-state-reducer";
import { IAddressSpacesResponse } from "schema/ResponseTypes";
import { FetchPolicy, AddressTypes } from "constant";
import { messagingAddressNameRegexp } from "types/Configs";

export const CreateAddress: React.FunctionComponent = () => {
  const { dispatch, state } = useStoreContext();
  const { modalProps } = (state && state.modal) || {};
  const { onConfirm, onClose, name, namespace, addressSpaceType } =
    modalProps || {};

  const [addressName, setAddressName] = useState("");
  const [addressType, setAddressType] = useState(" ");
  const [plan, setPlan] = useState(" ");
  const [topic, setTopic] = useState(" ");
  const [deadletterAddress, setDeadletterAddress] = useState(" ");
  const [expiryAddress, setExpiryAddress] = useState(" ");
  const [addressTypes, setAddressTypes] = useState<IDropdownOption[]>([]);
  const [addressPlans, setAddressPlans] = useState<IDropdownOption[]>([]);
  const [topicsForSubscription, setTopicForSubscription] = useState<
    IDropdownOption[]
  >([]);
  const [deadletterOptions, setDeadletterOptions] = useState<IDropdownOption[]>(
    []
  );
  const [isNameValid, setIsNameValid] = useState<boolean>(true);

  const resetFormState = () => {
    setAddressType("");
    setPlan("");
  };

  const refetchQueries: string[] = ["all_addresses_for_addressspace_view"];
  const [setAddressQueryVariables] = useMutationQuery(
    CREATE_ADDRESS,
    refetchQueries,
    resetFormState,
    resetFormState
  );

  const { data } = useQuery<IAddressSpacesResponse>(
    RETURN_ADDRESS_SPACE_DETAIL(name, namespace),
    { fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  const getAddressSpacePlan = () => {
    let addressSpacePlan: string | null = null;
    if (
      data &&
      data.addressSpaces &&
      data.addressSpaces.addressSpaces.length > 0
    ) {
      const plan = data.addressSpaces.addressSpaces[0].spec.plan.metadata.name;
      if (plan) {
        addressSpacePlan = plan;
      }
    }
    return addressSpacePlan;
  };

  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
    onClose && onClose();
  };

  const handleAddressChange = (name: string) => {
    setAddressName(name);
    !messagingAddressNameRegexp.test(name)
      ? setIsNameValid(false)
      : setIsNameValid(true);
  };

  const isReviewButtonEnabled = () => {
    if (
      addressName.trim() !== "" &&
      isNameValid &&
      plan.trim() !== "" &&
      addressType.trim() !== "" &&
      (addressType.toLowerCase() === AddressTypes.SUBSCRIPTION) ===
        (topic.trim() !== "")
    ) {
      return true;
    }
    return false;
  };

  const isFinishButtonEnabled = () => {
    if (
      addressName.trim() !== "" &&
      plan.trim() !== "" &&
      addressType.trim() !== "" &&
      (addressType.toLowerCase() === AddressTypes.SUBSCRIPTION) ===
        (topic.trim() !== "") &&
      isNameValid
    ) {
      return true;
    }
    return false;
  };

  const handleSave = async () => {
    if (name) {
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
        if (
          addressType &&
          addressType.trim().toLowerCase() === AddressTypes.SUBSCRIPTION
        ) {
          variable.spec.topic = topic;
        }
        if (
          deadletterAddress &&
          deadletterAddress.trim() !== "" &&
          ((addressType &&
            addressType.trim().toLowerCase() === AddressTypes.QUEUE) ||
            addressType.trim().toLowerCase() === AddressTypes.SUBSCRIPTION)
        ) {
          variable.spec.deadletter = deadletterAddress.trim();
        }
        if (
          expiryAddress &&
          expiryAddress.trim() !== "" &&
          addressType &&
          (addressType.trim().toLowerCase() === AddressTypes.QUEUE ||
            addressType.trim().toLowerCase() === AddressTypes.TOPIC)
        ) {
          variable.spec.expiry = expiryAddress.trim();
        }
        return variable;
      };
      const variables = {
        a: getVariables(),
        as: name
      };
      await setAddressQueryVariables(variables);
    }

    onCloseDialog();
    onConfirm && onConfirm();
  };
  const steps = [
    {
      name: "Definition",
      component: (
        <AddressDefinitionContainer
          addressspaceName={name}
          namespace={namespace}
          addressSpacePlan={getAddressSpacePlan()}
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
          setDeadletter={setDeadletterAddress}
          deadletter={deadletterAddress}
          setExpiryAddress={setExpiryAddress}
          expiryAddress={expiryAddress}
          topicsForSubscription={topicsForSubscription}
          setTopicForSubscription={setTopicForSubscription}
          deadletterOptions={deadletterOptions}
          setDeadletterOptions={setDeadletterOptions}
        />
      ),
      enableNext: isReviewButtonEnabled(),
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
          deadletter={deadletterAddress}
          expiry={expiryAddress}
          namespace={namespace || ""}
          addressspace={name}
        />
      ),
      enableNext: isFinishButtonEnabled(),
      canJumpTo: isReviewButtonEnabled(),
      nextButtonText: "Finish"
    }
  ];
  return (
    <Wizard
      id="create-addr-wizard"
      isOpen={true}
      isFullHeight={true}
      isFullWidth={true}
      onClose={onCloseDialog}
      title="Create new Address"
      steps={steps}
      onNext={() => {}}
      onSave={handleSave}
    />
  );
};
