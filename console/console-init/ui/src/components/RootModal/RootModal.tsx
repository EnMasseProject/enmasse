/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useStoreContext, MODAL_TYPES } from "context-state-reducer";
import { DialogPrompt } from "components/DialogPrompt";
import {
  EditAddressSpaceContainer,
  CreateMessagingProject
} from "modules/address-space/dialogs";
import { EditAddress, CreateAddress } from "modules/address/dialogs";
import { UpdatePassword } from "components/UpdatePassword";
import { EditMsgProjectContainer } from "modules/project/dailogs/EditMsgProject";

const MODAL_COMPONENTS: any = {
  [MODAL_TYPES.CREATE_ADDRESS_SPACE]: CreateMessagingProject,
  [MODAL_TYPES.EDIT_ADDRESS_SPACE]: EditAddressSpaceContainer,
  [MODAL_TYPES.CREATE_ADDRESS]: CreateAddress,
  [MODAL_TYPES.EDIT_ADDRESS]: EditAddress,
  [MODAL_TYPES.DELETE_ADDRESS_SPACE]: DialogPrompt,
  [MODAL_TYPES.DELETE_ADDRESS]: DialogPrompt,
  [MODAL_TYPES.PURGE_ADDRESS]: DialogPrompt,
  [MODAL_TYPES.CLOSE_CONNECTIONS]: DialogPrompt,
  [MODAL_TYPES.LEAVE_CREATE_DEVICE]: DialogPrompt,
  [MODAL_TYPES.UPDATE_PASSWORD]: UpdatePassword,
  [MODAL_TYPES.DELETE_PROJECT]: DialogPrompt,
  [MODAL_TYPES.DELETE_IOT_DEVICE]: DialogPrompt,
  [MODAL_TYPES.UPDATE_DEVICE_STATUS]: DialogPrompt,
  [MODAL_TYPES.EDIT_PROJECT]: EditMsgProjectContainer,
  [MODAL_TYPES.UPDATE_DEVICE_CREDENTIAL_STATUS]: DialogPrompt
};

export const RootModal: React.FC<{}> = () => {
  const { state } = useStoreContext();
  const { modalType, modalProps } = (state && state.modal) || {};
  const ModalComponent = MODAL_COMPONENTS[modalType];
  if (!modalType || !ModalComponent) {
    return null;
  }

  return <ModalComponent id="root-modal-component" {...modalProps} />;
};
