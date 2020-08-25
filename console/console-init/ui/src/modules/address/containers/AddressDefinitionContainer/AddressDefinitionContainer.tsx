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
  RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION,
  RETURN_DLQ_ADDRESSES_FOR_SUBSCRIPTION_AND_QUEUE
} from "graphql-module/queries";
import { IAddressResponse } from "schema/ResponseTypes";
import {
  AddressConfiguration,
  IAddressConfigurationProps
} from "modules/address/components";
import { FetchPolicy } from "constant";

export interface IAddressDefinition extends IAddressConfigurationProps {
  addressspaceName: string;
  namespace: string;
  addressSpacePlan: string | null;
  setType: (value: any) => void;
  setPlan: (value: any) => void;
  addressSpaceType?: string;
  setTopic: (value: string) => void;
  setDeadletter: (value: string) => void;
  setExpiryAddress: (value: string) => void;
  setTypeOptions: (values: IDropdownOption[]) => void;
  setPlanOptions: (values: IDropdownOption[]) => void;
  setTopicForSubscription: (values: IDropdownOption[]) => void;
  setDeadletterOptions: (values: IDropdownOption[]) => void;
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

export const AddressDefinitionContainer: React.FunctionComponent<IAddressDefinition> = ({
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
  deadletter,
  expiryAddress,
  setExpiryAddress,
  setDeadletter,
  planOptions,
  setPlanOptions,
  topicsForSubscription,
  setTopicForSubscription,
  deadletterOptions,
  setDeadletterOptions
}) => {
  const client = useApolloClient();
  const { loading, data } = useQuery<IAddressTypes>(RETURN_ADDRESS_TYPES, {
    variables: {
      a: addressSpaceType
    }
  });

  if (loading) return <Loading />;

  const { addressTypes_v2 } = data || {
    addressTypes_v2: []
  };

  const onTypeSelect = async (value: string) => {
    if (value) {
      const type = value;
      setType(type);
      const addressPlans = await client.query<IAddressPlans>({
        query: RETURN_ADDRESS_PLANS(addressSpacePlan, type),
        fetchPolicy: FetchPolicy.NETWORK_ONLY
      });

      if (addressPlans.data && addressPlans.data.addressPlans.length > 0) {
        const planOptions = addressPlans.data.addressPlans.map(plan => {
          return {
            key: plan.metadata.name,
            value: plan.metadata.name,
            label: plan.spec.displayName || plan.metadata.name,
            description: plan.spec.shortDescription || plan.spec.longDescription
          };
        });
        setPlan(" ");
        setTopic(" ");
        setPlanOptions(planOptions);
      }

      if (type.toLowerCase() === "subscription") {
        const topicsAddresses = await client.query<IAddressResponse>({
          query: RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION(
            addressspaceName,
            namespace,
            type
          ),
          fetchPolicy: FetchPolicy.NETWORK_ONLY
        });
        if (
          topicsAddresses.data &&
          topicsAddresses.data.addresses &&
          topicsAddresses.data.addresses.addresses.length > 0
        ) {
          const topics = topicsAddresses.data.addresses.addresses.map(
            address => {
              return {
                key: address.spec.address,
                value: address.spec.address,
                label: address.metadata.name
              };
            }
          );
          setTopicForSubscription(topics);
        }
      }
      if (
        type?.toLowerCase() === "subscription" ||
        type?.toLowerCase() === "queue"
      ) {
        const deadletterAddresses = await client.query<IAddressResponse>({
          query: RETURN_DLQ_ADDRESSES_FOR_SUBSCRIPTION_AND_QUEUE(
            addressspaceName,
            namespace,
            type
          ),
          fetchPolicy: FetchPolicy.NETWORK_ONLY
        });
        let deadletters: IDropdownOption[] = [];
        if (
          deadletterAddresses.data &&
          deadletterAddresses.data.addresses &&
          deadletterAddresses.data.addresses.addresses.length > 0
        ) {
          deadletters = deadletterAddresses.data.addresses.addresses.map(
            address => {
              return {
                key: address.spec.address,
                value: address.spec.address,
                label: address.metadata.name
              };
            }
          );
        }
        deadletters.push({ key: " ", value: " ", label: "- None -" });
        setDeadletterOptions(deadletters);
      }
    }
  };

  const onPlanSelect = (value: string) => {
    setPlan(value);
  };

  const onTopicSelect = (value: string) => {
    setTopic(value);
  };
  const onDeadletterSelect = (value: string) => {
    setDeadletter(value);
  };
  const onExpiryAddressSelect = (value: string) => {
    setExpiryAddress(value);
  };

  const types: IDropdownOption[] = addressTypes_v2.map(type => {
    return {
      key: type.spec.displayName,
      value: type.spec.displayName,
      label: type.spec.displayName,
      description: type.spec.shortDescription
    };
  });

  if (typeOptions && typeOptions.length === 0) {
    setTypeOptions(types);
  }

  return (
    <AddressConfiguration
      addressName={addressName}
      isNameValid={isNameValid}
      handleAddressChange={handleAddressChange}
      type={type}
      plan={plan}
      topic={topic}
      onTypeSelect={onTypeSelect}
      onPlanSelect={onPlanSelect}
      onTopicSelect={onTopicSelect}
      onDeadletterSelect={onDeadletterSelect}
      onExpiryAddressSelect={onExpiryAddressSelect}
      typeOptions={typeOptions}
      planOptions={planOptions}
      deadletter={deadletter}
      expiryAddress={expiryAddress}
      deadletterOptions={deadletterOptions}
      topicsForSubscription={topicsForSubscription}
    />
  );
};
