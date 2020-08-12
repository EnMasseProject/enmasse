/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { StyleSheet } from "@patternfly/react-styles";
import { IDropdownOption, DropdownWithToggle } from "components";
import { FormGroup, DropdownPosition } from "@patternfly/react-core";

const styles = StyleSheet.create({
  dropdownItem: {
    "text-transform": "capitalize",
    fontWeight: "bold"
  },
  dropdown_align: {
    display: "flex"
  },
  dropdown_toggle: {
    flex: "1"
  }
});
export interface IExpiryQueueProps {
  onExpiryQueueSelect?: (value: string) => void;
  expiryQueue: string;
  deadletterOptions?: IDropdownOption[];
}
export const ExpiryQueue: React.FunctionComponent<IExpiryQueueProps> = ({
  expiryQueue,
  onExpiryQueueSelect,
  deadletterOptions
}) => {
  return (
    <FormGroup
      label="Expiry Queue"
      isRequired={false}
      fieldId="addr-configuration-expiryQueue-dropdown"
    >
      <br />
      <DropdownWithToggle
        id="addr-configuration-expiryQueue-dropdown"
        className={styles.dropdown_align}
        toggleClass={styles.dropdown_toggle}
        dropdownItemClass={styles.dropdownItem}
        position={DropdownPosition.left}
        onSelectItem={onExpiryQueueSelect}
        value={expiryQueue}
        dropdownItems={deadletterOptions}
        dropdownItemId="address-definition-topic-dropdown-item"
        isDisplayLabelAndValue={true}
        isRequiredField={false}
      />
    </FormGroup>
  );
};
