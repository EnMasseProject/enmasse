import React from "react";
import { useStoreContext, MODAL_TYPES } from "context-state-reducer";
import { DialogPrompt } from "components/common/DialogPrompt";
import {
  EditAddressSpaceContainer,
  CreateAddressSpace
} from "modules/address-space/dialogs";
import { EditAddress } from "modules/address/dialogs/EditAddress";

const MODAL_COMPONENTS: any = {
  [MODAL_TYPES.CREATE_ADDRESS_SPACE]: CreateAddressSpace,
  [MODAL_TYPES.EDIT_ADDRESS_SPACE]: EditAddressSpaceContainer,
  [MODAL_TYPES.EDIT_ADDRESS]: EditAddress,
  [MODAL_TYPES.DELETE_ADDRESS_SPACE]: DialogPrompt,
  [MODAL_TYPES.DELETE_ADDRESS]: DialogPrompt,
  [MODAL_TYPES.PURGE_ADDRESS]: DialogPrompt
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
