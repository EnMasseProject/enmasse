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
  setExpiryQueue: (value: string) => void;
  setTypeOptions: (values: IDropdownOption[]) => void;
  setPlanOptions: (values: IDropdownOption[]) => void;
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
  expiryQueue,
  setExpiryQueue,
  setDeadletter,
  planOptions,
  setPlanOptions
}) => {
  const client = useApolloClient();
  const [topicsForSubscription, setTopicForSubscription] = useState<
    IDropdownOption[]
  >([]);
  const [deadletterOptions, setDeadletterOptions] = useState<IDropdownOption[]>(
    []
  );
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
        const topics_addresses = await client.query<IAddressResponse>({
          query: RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION(
            addressspaceName,
            namespace,
            type
          ),
          fetchPolicy: FetchPolicy.NETWORK_ONLY
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
          setTopicForSubscription(topics);
        }
      }
      if (
        type?.toLowerCase() === "subscription" ||
        type?.toLowerCase() === "queue"
      ) {
        const deadletter_addresses = await client.query<IAddressResponse>({
          query: RETURN_DLQ_ADDRESSES_FOR_SUBSCRIPTION_AND_QUEUE(
            addressspaceName,
            namespace,
            type
          ),
          fetchPolicy: FetchPolicy.NETWORK_ONLY
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
          setDeadletterOptions(deadletters);
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
  const onDeadletterSelect = (value: string) => {
    setDeadletter(value);
  };
  const onExpiryQueueSelect = (value: string) => {
    setExpiryQueue(value);
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
      onExpiryQueueSelect={onExpiryQueueSelect}
      typeOptions={typeOptions}
      planOptions={planOptions}
      deadletter={deadletter}
      expiryQueue={expiryQueue}
      deadletterOptions={deadletterOptions}
      topicsForSubscription={topicsForSubscription}
    />
  );
};
