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
  Button,
  Title
} from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { DropdownWithToggle, DividerWithTitle } from "components";
import { ISelectOption } from "utils";
import { StyleSheet, css } from "aphrodite";

const styles = StyleSheet.create({
  type_margin: {
    marginLeft: 10
  },
  dropdown_align: { display: "flex", marginRight: 10 },
  dropdown_toggle_align: { flex: "1" }
});

export interface IExtension {
  id: string;
  key?: string;
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
      <Grid id={"ext-list-grid"}>
        <GridItem span={12}>
          <DividerWithTitle title={"Ext"} />
          <br />
        </GridItem>
        <GridItem span={4}>
          <Title headingLevel="h1" size="md">
            Parameter
          </Title>
        </GridItem>
        <GridItem span={3} className={css(styles.type_margin)}>
          <Title headingLevel="h1" size="md">
            Type
          </Title>
        </GridItem>
        <GridItem span={5}>
          <Title headingLevel="h1" size="md">
            Value
          </Title>
        </GridItem>
        {Array.isArray(extensions) &&
          extensions?.map(ext => {
            const { id } = ext;
            return (
              <Grid key={id}>
                <GridItem span={4}>
                  <TextInput
                    id={`ext-list-key-textinput-${id}`}
                    name="key"
                    onChange={(value, event) =>
                      handleInputChangeExtension(credentialId, event, value, id)
                    }
                    value={ext["key"]}
                  />
                </GridItem>
                <GridItem span={3} className={css(styles.type_margin)}>
                  <DropdownWithToggle
                    id={`ext-list-type-dropdwon-${id}`}
                    name="type"
                    className={css(styles.dropdown_align)}
                    toggleClass={css(styles.dropdown_toggle_align)}
                    position={DropdownPosition.left}
                    dropdownItems={typeOptions}
                    value={ext["type"] || ""}
                    onSelectItem={(value, event) =>
                      handleInputChangeExtension(credentialId, event, value, id)
                    }
                  />
                </GridItem>
                <GridItem span={4}>
                  <TextInput
                    id={`ext-list-value-textinput-${id}`}
                    name="value"
                    onChange={(value, event) =>
                      handleInputChangeExtension(credentialId, event, value, id)
                    }
                  />
                </GridItem>
                <GridItem span={1}>
                  <Button
                    id="ext-list-delete-extension-button"
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
