/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  Form,
  FormGroup,
  TextInput,
  FormSelect,
  FormSelectOption,
  Modal,
  Button
} from "@patternfly/react-core";
import { useQuery } from "@apollo/react-hooks";
import { Loading } from "use-patternfly";
import { useStoreContext, types } from "context-state-reducer";
import { useMutationQuery } from "hooks";
import {
  RETURN_ADDRESS_PLANS,
  EDIT_ADDRESS,
  RETURN_DLQ_ADDRESSES_FOR_SUBSCRIPTION_AND_QUEUE
} from "graphql-module/queries";
import { IAddressResponse } from "schema/ResponseTypes";
import { FetchPolicy } from "constant";
import { IDropdownOption } from "components";
interface IAddressPlans {
  addressPlans: Array<{
    metadata: {
      name: String;
    };
    spec: {
      addressType: string;
      displayName: string;
    };
  }>;
}

export const EditAddress: React.FunctionComponent = () => {
  const { state, dispatch } = useStoreContext();
  const { modalProps } = (state && state.modal) || {};
  const { onConfirm, onClose, address } = modalProps || {};
  const [plan, setPlan] = useState(
    { planLabel: address.planLabel, value: address.planValue } || {}
  );

  const [deadletterAddress, setDeadletterAddress] = useState<any>();

  const [expiryAddress, setExpiryAddress] = useState<any>();

  useEffect(() => {
    if (address.deadLetterAddress && address.deadLetterAddress !== null) {
      setDeadletterAddress({ value: address.deadLetterAddress });
    }
    if (address.expiryAddress && address.expiryAddress !== null) {
      setExpiryAddress({ value: address.expiryAddress });
    }
  }, [address]);

  const refetchQueries: string[] = [
    "all_addresses_for_addressspace_view",
    "single_addresses"
  ];

  const [setEditAddressQueryVariables] = useMutationQuery(
    EDIT_ADDRESS,
    refetchQueries
  );

  let { loading, data } = useQuery<IAddressPlans>(
    RETURN_ADDRESS_PLANS(modalProps.addressSpacePlan, address.type || "")
  );

  const dlq_addresses = useQuery<IAddressResponse>(
    RETURN_DLQ_ADDRESSES_FOR_SUBSCRIPTION_AND_QUEUE(
      address.addressSpaceName,
      address.namespace,
      address.type
    ),
    { fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  if (loading || dlq_addresses.loading) return <Loading />;

  const { addressPlans } = data || {
    addressPlans: []
  };

  const { addresses } = dlq_addresses.data || {
    addresses: { total: 0, addresses: [] }
  };

  let optionsPlan: any[] = addressPlans
    .map(plan => {
      return {
        value: plan.metadata.name,
        label: plan.spec.displayName || plan.metadata.name,
        disabled: false
      };
    })
    .filter(plan => plan !== undefined);

  let deadLetters: IDropdownOption[] = [{ key: " ", value: " ", label: " " }];
  addresses.addresses.forEach(address => {
    deadLetters.push({
      key: address.spec.address,
      value: address.spec.address,
      label: address.metadata.name
    });
  });

  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
    if (onClose) {
      onClose();
    }
  };

  const onPlanChanged = (plan: string) => {
    if (address) {
      const newPlan: any = optionsPlan.filter(
        addressPlan => addressPlan.value === plan
      );
      if (newPlan && newPlan[0]) {
        setPlan(newPlan[0]);
      }
    }
  };

  const onChangeDeadletter = (value: string) => {
    if (value) {
      const newDlq = deadLetters.filter(address => address.value === value);
      if (newDlq && newDlq[0]) {
        setDeadletterAddress(newDlq[0]);
      }
    }
  };

  const onChangeExpiryAddress = (value: string) => {
    if (value) {
      const newDlq = deadLetters.filter(address => address.value === value);
      if (newDlq && newDlq[0]) {
        setExpiryAddress(newDlq[0]);
      }
    }
  };

  const onConfirmDialog = async () => {
    if (address) {
      const variables = {
        a: {
          name: address.name,
          namespace: address.namespace
        },
        jsonPatch:
          '[{"op":"replace","path":"/spec/plan","value":"' +
          plan.value +
          '"},{"op":"replace","path":"/spec/deadLetterAddress","value":"' +
          deadletterAddress.value +
          '"},{"op":"replace","path":"/spec/expiryAddress","value":"' +
          expiryAddress.value +
          '"}]',
        patchType: "application/json-patch+json"
      };
      await setEditAddressQueryVariables(variables);
      onCloseDialog();
      if (onConfirm) {
        onConfirm();
      }
    }
  };

  return (
    <Modal
      id="al-modal-edit-address"
      title="Edit"
      isSmall
      isOpen={true}
      onClose={onCloseDialog}
      actions={[
        <Button
          key="confirm"
          id="al-edit-confirm"
          variant="primary"
          onClick={onConfirmDialog}
        >
          Confirm
        </Button>,
        <Button
          key="cancel"
          id="al-edit-cancel"
          variant="link"
          onClick={onCloseDialog}
        >
          Cancel
        </Button>
      ]}
      isFooterLeftAligned
    >
      <Form>
        <FormGroup label="Name" fieldId="simple-form-name">
          <TextInput
            type="text"
            id="edit-addr-name"
            name="simple-form-name"
            isDisabled
            aria-describedby="simple-form-name-helper"
            value={address.name}
          />
        </FormGroup>
        <FormGroup label="Type" fieldId="simple-form-name">
          <FormSelect
            isDisabled
            aria-label="FormSelect Input"
            id="edit-addr-type"
          >
            <FormSelectOption value={address.type} label={address.type} />
          </FormSelect>
        </FormGroup>
        {address.type?.trim() === "subscription" && (
          <FormGroup label="Topic" fieldId="edit-address-topic-form-select">
            <FormSelect
              isDisabled
              aria-label="form select topic"
              id="edit-addr-topic"
            >
              <FormSelectOption value={address.topic} label={address.topic} />
            </FormSelect>
          </FormGroup>
        )}
        <FormGroup label="Plan" fieldId="simple-form-name">
          <FormSelect
            id="edit-addr-plan"
            value={plan.value}
            onChange={value => onPlanChanged(value)}
            aria-label="FormSelect Input"
          >
            {optionsPlan.map((option, index) => (
              <FormSelectOption
                isDisabled={option.disabled}
                key={index}
                value={option.value}
                label={option.label}
              />
            ))}
          </FormSelect>
        </FormGroup>
        {(address.type?.trim() === "subscription" ||
          address.type?.trim() === "queue") && (
          <>
            <FormGroup
              label="Deadletter Address"
              fieldId="edit-address-dead-letter-form-select"
            >
              <FormSelect
                id="edit-address-dead-letter-form-select"
                value={
                  deadletterAddress && deadletterAddress.value !== null
                    ? deadletterAddress.value
                    : ""
                }
                onChange={value => onChangeDeadletter(value)}
                aria-label="dead letter address"
              >
                {deadLetters.map((option, index) => (
                  <FormSelectOption
                    key={index}
                    value={option.value}
                    label={option.label}
                  />
                ))}
              </FormSelect>
            </FormGroup>
            <FormGroup
              label="Expiry Address"
              fieldId="edit-address-expiry-addr-form-select"
            >
              <FormSelect
                id="edit-address-expiry-addr-form-select"
                value={
                  expiryAddress && expiryAddress.value !== null
                    ? expiryAddress.value
                    : ""
                }
                onChange={value => onChangeExpiryAddress(value)}
                aria-label="expiry address"
              >
                {deadLetters.map((option, index) => (
                  <FormSelectOption
                    key={index}
                    value={option.value}
                    label={option.label}
                  />
                ))}
              </FormSelect>
            </FormGroup>
          </>
        )}
      </Form>
    </Modal>
  );
};
