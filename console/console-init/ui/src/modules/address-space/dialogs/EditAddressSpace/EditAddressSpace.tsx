/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useQuery } from "@apollo/react-hooks";
import {
  RETURN_ADDRESS_SPACE_PLANS,
  RETURN_FILTERED_AUTHENTICATION_SERVICES
} from "graphql-module/queries";
import {
  Form,
  TextContent,
  Text,
  TextVariants,
  FormGroup,
  FormSelect,
  FormSelectOption,
  TextInput,
  Radio,
  Button,
  Modal
} from "@patternfly/react-core";
import {
  IAddressSpacePlans,
  IAddressSpaceAuthServiceResponse
} from "modules/address-space/dialogs/CreateAddressSpace/CreateAddressSpaceConfiguration";
import { Loading } from "use-patternfly";
import { useStoreContext, types } from "context-state-reducer";
import { useMutationQuery } from "hooks";
import { EDIT_ADDRESS_SPACE } from "graphql-module/queries";

export const EditAddressSpace: React.FunctionComponent<{}> = () => {
  const { state, dispatch } = useStoreContext();
  const { modalProps } = (state && state.modal) || {};
  const { onConfirm, onClose } = modalProps || {};

  const [addressSpace, setAddressSpace] = useState(modalProps.addressSpace);
  const refetchQueries: string[] = ["all_address_spaces"];
  const [setEditAddressSpaceQueryVariables] = useMutationQuery(
    EDIT_ADDRESS_SPACE,
    refetchQueries
  );

  const { loading, data } = useQuery<IAddressSpacePlans>(
    RETURN_ADDRESS_SPACE_PLANS
  );

  const authServices = useQuery<IAddressSpaceAuthServiceResponse>(
    RETURN_FILTERED_AUTHENTICATION_SERVICES,
    {
      variables: {
        t: addressSpace.type
      }
    }
  ).data || { addressSpaceSchema_v2: [] };

  if (loading) return <Loading />;

  const { addressSpacePlans } = data || {
    addressSpacePlans: []
  };

  let planOptions: any[] = [];

  let authServiceOptions: any[] = [];

  if (addressSpace.type) {
    planOptions =
      addressSpacePlans
        .map(plan => {
          if (plan.spec.addressSpaceType === addressSpace.type) {
            return {
              value: plan.metadata.name,
              label: plan.metadata.name
            };
          }
        })
        .filter(plan => plan !== undefined) || [];
  }

  if (authServices.addressSpaceSchema_v2[0])
    authServiceOptions = authServices.addressSpaceSchema_v2[0].spec.authenticationServices.map(
      authService => {
        return {
          value: authService,
          label: authService
        };
      }
    );

  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
    if (onClose) {
      onClose();
    }
  };

  const onPlanChange = (plan: string) => {
    if (addressSpace) {
      addressSpace.planValue = plan;
      setAddressSpace({ ...addressSpace });
    }
  };

  const onAuthServiceChanged = (authService: string) => {
    if (addressSpace) {
      addressSpace.authenticationService = authService;
      setAddressSpace({ ...addressSpace });
    }
  };

  const onConfirmDialog = async () => {
    if (addressSpace) {
      const variables = {
        a: {
          name: addressSpace.name,
          namespace: addressSpace.nameSpace
        },
        jsonPatch:
          '[{"op":"replace","path":"/spec/plan","value":"' +
          addressSpace.planValue +
          '"},' +
          '{"op":"replace","path":"/spec/authenticationService/name","value":"' +
          addressSpace.authenticationService +
          '"}' +
          "]",
        patchType: "application/json-patch+json"
      };
      await setEditAddressSpaceQueryVariables(variables);

      onCloseDialog();
      if (onConfirm) {
        onConfirm();
      }
    }
  };

  return (
    <Modal
      isLarge
      id="as-list-edit-modal"
      title="Edit"
      isOpen={true}
      onClose={onCloseDialog}
      actions={[
        <Button
          key="confirm"
          id="as-list-edit-confirm"
          variant="primary"
          onClick={onConfirmDialog}
        >
          Confirm
        </Button>,
        <Button
          key="cancel"
          id="as-list-edit-cancel"
          variant="link"
          onClick={onCloseDialog}
        >
          Cancel
        </Button>
      ]}
      isFooterLeftAligned={true}
    >
      <Form>
        <TextContent>
          <Text component={TextVariants.h2}>Choose a new plan.</Text>
        </TextContent>
        <FormGroup label="Namespace" fieldId="name-space" isRequired={true}>
          <FormSelect
            id="edit-namespace"
            isDisabled
            value={addressSpace.nameSpace}
            aria-label="FormSelect Input"
          >
            <FormSelectOption
              value={addressSpace.nameSpace}
              label={addressSpace.nameSpace}
            />
          </FormSelect>
        </FormGroup>
        <FormGroup label="Name" fieldId="address-space" isRequired={true}>
          <TextInput
            type="text"
            id="as-name"
            isDisabled
            value={addressSpace.name}
          />
        </FormGroup>
        <FormGroup
          label="Type"
          fieldId="address-space-type"
          isInline
          isRequired={true}
        >
          <Radio
            name="radio-1"
            isDisabled
            label="Standard"
            id="radio-standard"
            value="standard"
            isChecked={addressSpace.type === "standard"}
          />
          <Radio
            name="radio-2"
            isDisabled
            label="Brokered"
            id="radio-brokered"
            value="brokered"
            isChecked={addressSpace.type === "brokered"}
          />
        </FormGroup>
        <FormGroup
          label="Address space plan"
          fieldId="simple-form-name"
          isRequired={true}
        >
          <FormSelect
            id="edit-addr-plan"
            value={addressSpace.planValue}
            onChange={val => onPlanChange(val)}
            aria-label="FormSelect Input"
          >
            {planOptions.map((option, index) => (
              <FormSelectOption
                isDisabled={option.disabled}
                key={index}
                value={option.value}
                label={option.label}
              />
            ))}
          </FormSelect>
        </FormGroup>
        <FormGroup
          label="Authentication Service"
          fieldId="simple-form-name"
          isRequired={true}
        >
          <FormSelect
            id="edit-addr-auth"
            value={addressSpace.authenticationService}
            onChange={val => onAuthServiceChanged(val)}
            aria-label="FormSelect Input"
          >
            {authServiceOptions.map((option, index) => (
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
