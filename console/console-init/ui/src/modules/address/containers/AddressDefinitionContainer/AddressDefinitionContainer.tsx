/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { Loading } from "use-patternfly";
import { IDropdownOption } from "components";
import {
  RETURN_ADDRESS_PLANS,
  RETURN_ADDRESS_TYPES,
  RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION
} from "graphql-module/queries";
import { IAddressResponse } from "schema/ResponseTypes";
import {
  AddressConfiguration,
  IAddressConfigurationProps
} from "modules/address/components";

export interface IAddressDefinition extends IAddressConfigurationProps {
  addressspaceName: string;
  namespace: string;
  addressSpacePlan: string | null;
  setType: (value: any) => void;
  setPlan: (value: any) => void;
  addressSpaceType?: string;
  setTopic: (value: string) => void;
  setDeadletters: (value: string) => void;
  setExpiryqueue: (value: string) => void;
  setTypeOptions: (values: IDropdownOption[]) => void;
  setPlanOptions: (values: IDropdownOption[]) => void;
  setTopicForSubscripitons: (values: IDropdownOption[]) => void;
  setDeadlettersOptions: (values: IDropdownOption[]) => void;
  setExpiryqueueOptions: (values: IDropdownOption[]) => void;
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
  topicsForSubscription,
  Deadletters,
  Expiryqueue,
  setExpiryqueue,
  setDeadletters,
  DeadlettersOptions,
  ExpiryqueueOptions,
  setExpiryqueueOptions,
  setDeadlettersOptions,
  setTopicForSubscripitons,
  planOptions,
  setPlanOptions
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
        query: RETURN_ADDRESS_PLANS(addressSpacePlan, type)
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
                key: address.spec.address,
                value: address.spec.address,
                label: address.metadata.name
              };
            }
          );
          setTopicForSubscripitons(topics);
        }
      }
      if (
        type.toLowerCase() === "subscription" ||
        type.toLowerCase() === "queue"
      ) {
        const deadletter_addresses = await client.query<IAddressResponse>({
          query: RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION(
            addressspaceName,
            namespace,
            type
          )
        });
        if (
          deadletter_addresses.data &&
          deadletter_addresses.data.addresses &&
          deadletter_addresses.data.addresses.addresses.length > 0
        ) {
          const deadletters = deadletter_addresses.data.addresses.addresses.map(
            address => {
              return {
                key: address.spec.address,
                value: address.spec.address,
                label: address.metadata.name
              };
            }
          );
          setDeadlettersOptions(deadletters);
        }
      }
      if (
        type.toLowerCase() === "subscription" ||
        type.toLowerCase() === "queue"
      ) {
        const expiryqueue_addresses = await client.query<IAddressResponse>({
          query: RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION(
            addressspaceName,
            namespace,
            type
          )
        });
        if (
          expiryqueue_addresses.data &&
          expiryqueue_addresses.data.addresses &&
          expiryqueue_addresses.data.addresses.addresses.length > 0
        ) {
          const expiryqueues = expiryqueue_addresses.data.addresses.addresses.map(
            address => {
              return {
                key: address.spec.address,
                value: address.spec.address,
                label: address.metadata.name
              };
            }
          );
          setExpiryqueueOptions(expiryqueues);
        }
      }
    }
  };

  const onPlanSelect = (value: string) => {
    setPlan(value);
  };

  const onTopicSelect = (value: string) => {
    setTopic(value);
  };
  const onDeadlettersSelect = (value: string) => {
    setDeadletters(value);
  };
  const onExpiryqueueSelect = (value: string) => {
    setExpiryqueue(value);
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
      onDeadlettersSelect={onDeadlettersSelect}
      onExpiryqueueSelect={onExpiryqueueSelect}
      typeOptions={typeOptions}
      planOptions={planOptions}
      Deadletters={Deadletters}
      Expiryqueue={Expiryqueue}
      DeadlettersOptions={DeadlettersOptions}
      ExpiryqueueOptions={ExpiryqueueOptions}
      topicsForSubscription={topicsForSubscription}
    />
  );
};
