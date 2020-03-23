/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { Loading } from "use-patternfly";
import { IDropdownOption } from "components";
import {
  RETURN_ADDRESS_PLANS,
  RETURN_ADDRESS_TYPES,
  RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION
} from "graphql-module/queries";
import { IAddressResponse } from "types/ResponseTypes";
import { AddressConfiguration } from "modules/address/components";

export interface IAddressDefinition {
  addressspaceName: string;
  namespace: string;
  addressName: string;
  addressSpacePlan: string;
  handleAddressChange: (name: string) => void;
  type: string;
  setType: (value: any) => void;
  plan: string;
  setPlan: (value: any) => void;
  topic: string;
  addressSpaceType: string;
  setTopic: (value: string) => void;
  typeOptions: IDropdownOption[];
  setTypeOptions: (values: IDropdownOption[]) => void;
  planOptions: IDropdownOption[];
  setPlanOptions: (values: IDropdownOption[]) => void;
  topicsForSubscription: IDropdownOption[];
  setTopicForSubscripitons: (values: IDropdownOption[]) => void;
  isNameValid: boolean;
}
interface IAddressPlans {
  addressPlans: Array<{
    metadata: {
      name: string;
    };
    spec: {
      addressType: string;
      displayName: string;
      shortDescription: string;
      longDescription: string;
    };
  }>;
}
interface IAddressTypes {
  addressTypes_v2: Array<{
    spec: {
      displayName: string;
      longDescription: string;
      shortDescription: string;
    };
  }>;
}
export const AddressDefinition: React.FunctionComponent<IAddressDefinition> = ({
  addressspaceName,
  namespace,
  addressName,
  addressSpacePlan,
  handleAddressChange,
  isNameValid,
  type,
  setType,
  plan,
  setPlan,
  addressSpaceType,
  topic,
  setTopic,
  typeOptions,
  setTypeOptions,
  topicsForSubscription,
  setTopicForSubscripitons,
  planOptions,
  setPlanOptions
}) => {
  const [isTypeOpen, setIsTypeOpen] = useState<boolean>(false);
  const [isTopicOpen, setIsTopicOpen] = useState<boolean>(false);
  const client = useApolloClient();

  const onTypeSelect = async (event: any) => {
    if (
      event.currentTarget.childNodes[0] &&
      event.currentTarget.childNodes[0].value
    ) {
      const type = event.currentTarget.childNodes[0].value;
      setType(type);
      const addressPlans = await client.query<IAddressPlans>({
        query: RETURN_ADDRESS_PLANS(addressSpacePlan, type)
      });
      if (addressPlans.data && addressPlans.data.addressPlans.length > 0) {
        const planOptions = addressPlans.data.addressPlans.map(plan => {
          return {
            value: plan.metadata.name,
            label: plan.spec.displayName || plan.metadata.name,
            description: plan.spec.shortDescription || plan.spec.longDescription
          };
        });
        setPlan(" ");
        setTopic(" ");
        setPlanOptions(planOptions);
      }
      if (type === "subscription") {
        const topics_addresses = await client.query<IAddressResponse>({
          query: RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION(
            addressspaceName,
            namespace,
            type
          )
        });
        if (
          topics_addresses.data &&
          topics_addresses.data.addresses &&
          topics_addresses.data.addresses.addresses.length > 0
        ) {
          const topics = topics_addresses.data.addresses.addresses.map(
            address => {
              return {
                value: address.spec.address,
                label: address.metadata.name
              };
            }
          );
          setTopicForSubscripitons(topics);
        }
      }
      setIsTypeOpen(!isTypeOpen);
    }
  };

  const [isPlanOpen, setIsPlanOpen] = useState(false);
  const onPlanSelect = (event: any) => {
    event.currentTarget.childNodes[0] &&
      setPlan(event.currentTarget.childNodes[0].value);
    setIsPlanOpen(!isPlanOpen);
  };
  const onTopicSelect = (event: any) => {
    event.currentTarget.childNodes[0] &&
      setTopic(event.currentTarget.childNodes[0].value);
    setIsTopicOpen(!isTopicOpen);
  };
  const { loading, error, data } = useQuery<IAddressTypes>(
    RETURN_ADDRESS_TYPES,
    {
      variables: {
        a: addressSpaceType
      }
    }
  );
  if (loading) return <Loading />;

  const { addressTypes_v2 } = data || {
    addressTypes_v2: []
  };
  const types: IDropdownOption[] = addressTypes_v2.map(type => {
    return {
      value: type.spec.displayName,
      label: type.spec.displayName,
      description: type.spec.shortDescription
    };
  });
  if (typeOptions.length === 0) setTypeOptions(types);

  return (
    <AddressConfiguration
      addressName={addressName}
      isNameValid={isNameValid}
      handleAddressChange={handleAddressChange}
      type={type}
      plan={plan}
      topic={topic}
      isTypeOpen={isTypeOpen}
      setIsTypeOpen={setIsTypeOpen}
      isPlanOpen={isPlanOpen}
      setIsPlanOpen={setIsPlanOpen}
      isTopicOpen={isTopicOpen}
      setIsTopicOpen={setIsTopicOpen}
      onTypeSelect={onTypeSelect}
      onPlanSelect={onPlanSelect}
      onTopicSelect={onTopicSelect}
      typeOptions={typeOptions}
      planOptions={planOptions}
      topicsForSubscription={topicsForSubscription}
    />
  );
};
