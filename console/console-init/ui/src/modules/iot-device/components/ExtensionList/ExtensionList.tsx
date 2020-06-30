/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Grid,
  GridItem,
  FormGroup,
  TextInput,
  DropdownPosition,
  Button
} from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { DropdownWithToggle, DividerWithTitle } from "components";
import { ISelectOption } from "utils";
import { StyleSheet, css } from "aphrodite";

const styles = StyleSheet.create({
  type_margin: {
    marginLeft: 10
  },
  delete_button_margin: {
    marginTop: 32
  },
  dropdown_align: { display: "flex", marginRight: 10 },
  dropdown_toggle_align: { flex: "1" }
});

export interface IExtension {
  id: string;
  parameter?: string;
  type?: string;
  value?: string;
}

export interface IExtensionListProps {
  extensions: Array<IExtension>;
  handleInputChange: (
    id: string,
    evt: any,
    value: string,
    extId?: string,
    property?: string
  ) => void;
  onDeleteExtension: (
    credentialId: string,
    property?: string,
    extId?: string
  ) => void;
  credentialId: string;
}

export const ExtensionList: React.FC<IExtensionListProps> = ({
  extensions,
  handleInputChange,
  onDeleteExtension,
  credentialId
}) => {
  const typeOptions: ISelectOption[] = [
    { key: "string", label: "String", value: "String" },
    { key: "number", label: "Number", value: "Number" },
    { key: "boolean", label: "Boolean", value: "Boolean" }
  ];

  const handleInputChangeExtension = (
    credentialId: string,
    event: any,
    value: string,
    id: string
  ) => {
    handleInputChange(credentialId, event, value, id, "ext");
  };

  return (
    <>
      <Grid id={"el-list-grid"}>
        <GridItem span={12}>
          <DividerWithTitle title={"Ext"} />
          <br />
        </GridItem>
        {extensions &&
          extensions.map(ext => {
            const { id } = ext;
            return (
              <Grid key={id}>
                <GridItem span={4}>
                  <FormGroup
                    fieldId={"el-parameter-textinput-" + id}
                    label="Parameter"
                  >
                    <TextInput
                      id={"el-parameter-textinput" + id}
                      name="parameter"
                      onChange={(value, event) =>
                        handleInputChangeExtension(
                          credentialId,
                          event,
                          value,
                          id
                        )
                      }
                    />
                  </FormGroup>
                </GridItem>
                <GridItem span={3}>
                  <FormGroup
                    fieldId={"el-type-dropdown-" + id}
                    label="Type"
                    className={css(styles.type_margin)}
                  >
                    <DropdownWithToggle
                      id={"el-type-dropdwon-" + id}
                      name="type"
                      className={css(styles.dropdown_align)}
                      toggleClass={css(styles.dropdown_toggle_align)}
                      position={DropdownPosition.left}
                      dropdownItems={typeOptions}
                      value={ext["type"] || ""}
                      onSelectItem={(value, event) =>
                        handleInputChangeExtension(
                          credentialId,
                          event,
                          value,
                          id
                        )
                      }
                    />
                  </FormGroup>
                </GridItem>
                <GridItem span={4}>
                  <FormGroup fieldId={"el-value-textinput-" + id} label="Value">
                    <TextInput
                      id={"el-value-textinput-" + id}
                      name="value"
                      onChange={(value, event) =>
                        handleInputChangeExtension(
                          credentialId,
                          event,
                          value,
                          id
                        )
                      }
                    />
                  </FormGroup>
                </GridItem>
                <GridItem span={1}>
                  <Button
                    className={css(styles.delete_button_margin)}
                    variant="link"
                    type="button"
                    icon={
                      <MinusCircleIcon
                        color={"var(--pf-c-button--m-plain--Color)"}
                      />
                    }
                    onClick={() => onDeleteExtension(credentialId, "ext", id)}
                  />
                </GridItem>
              </Grid>
            );
          })}
      </Grid>
    </>
  );
};
