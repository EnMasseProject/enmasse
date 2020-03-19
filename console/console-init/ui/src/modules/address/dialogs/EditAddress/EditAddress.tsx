/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
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
import { RETURN_ADDRESS_PLANS, EDIT_ADDRESS } from "graphql-module/queries";
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
  console.log(address);
  const [plan, setPlan] = React.useState(
    { planLabel: address.planLabel, value: address.planValue } || {}
  );
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

  if (loading) return <Loading />;

  const { addressPlans } = data || {
    addressPlans: []
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

  const onConfirmDialog = async () => {
    if (address) {
      const variables = {
        a: {
          name: address.name,
          namespace: address.namespace
        },
        jsonPatch:
          '[{"op":"replace","path":"/spec/plan","value":"' + plan.value + '"}]',
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
      </Form>
    </Modal>
  );
};
