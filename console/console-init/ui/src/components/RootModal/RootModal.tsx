/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useStoreContext, MODAL_TYPES } from "context-state-reducer";
import { DialogPrompt } from "components/DialogPrompt";
import {
  EditAddressSpaceContainer,
  CreateAddressSpace
} from "modules/address-space/dialogs";
import { EditAddress, CreateAddress } from "modules/address/dialogs";

const MODAL_COMPONENTS: any = {
  [MODAL_TYPES.CREATE_ADDRESS_SPACE]: CreateAddressSpace,
  [MODAL_TYPES.EDIT_ADDRESS_SPACE]: EditAddressSpaceContainer,
  [MODAL_TYPES.CREATE_ADDRESS]: CreateAddress,
  [MODAL_TYPES.EDIT_ADDRESS]: EditAddress,
  [MODAL_TYPES.DELETE_ADDRESS_SPACE]: DialogPrompt,
  [MODAL_TYPES.DELETE_ADDRESS]: DialogPrompt,
  [MODAL_TYPES.PURGE_ADDRESS]: DialogPrompt,
  [MODAL_TYPES.CLOSE_CONNECTIONS]: DialogPrompt
};

export const RootModal: React.FC<{}> = () => {
  const { state } = useStoreContext();
  const { modalType, modalProps } = (state && state.modal) || {};
  const ModalComponent = MODAL_COMPONENTS[modalType];
  if (!modalType || !ModalComponent) {
    return null;
  }

  return <ModalComponent {...modalProps} />;
};
