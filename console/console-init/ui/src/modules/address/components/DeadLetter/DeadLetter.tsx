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
  onDeadLetterSelect?: (value: string) => void;
  deadLetter: string;
  deadLetterOptions?: IDropdownOption[];
}
export const DeadLetter: React.FunctionComponent<IExpiryQueueProps> = ({
  deadLetter,
  onDeadLetterSelect,
  deadLetterOptions
}) => {
  return (
    <FormGroup
      label="Deadletter Queue"
      isRequired={false}
      fieldId="addr-configuration-deadLetter-dropdown"
    >
      <br />
      <DropdownWithToggle
        id="addr-configuration-deadLetter-dropdown"
        className={styles.dropdown_align}
        toggleClass={styles.dropdown_toggle}
        dropdownItemClass={styles.dropdownItem}
        position={DropdownPosition.left}
        onSelectItem={onDeadLetterSelect}
        value={deadLetter}
        dropdownItems={deadLetterOptions}
        dropdownItemId="address-definition-topic-dropdown-item"
        isDisplayLabelAndValue={true}
      />
      <br />
    </FormGroup>
  );
};
