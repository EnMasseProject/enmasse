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
import { css } from "@patternfly/react-styles";
import { DropdownWithToggle } from "components";
import { ISelectOption } from "utils";
import { dropdown_item_styles } from "modules/device";

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
    handleInputChange(credentialId, event, value, id, "extensions");
  };

  return (
    <>
      {extensions &&
        extensions.map(ext => {
          const { id } = ext;
          return (
            <Grid key={id}>
              <GridItem span={12}>Ext</GridItem>
              <GridItem span={4}>
                <FormGroup fieldId={"parameter" + id} label="Parameter">
                  <TextInput
                    id={"parameter" + id}
                    name="parameter"
                    onChange={(value, event) =>
                      handleInputChangeExtension(credentialId, event, value, id)
                    }
                  />
                </FormGroup>
              </GridItem>
              <GridItem span={3}>
                <FormGroup
                  fieldId={"type" + id}
                  label="Type"
                  style={{ marginLeft: "10px" }}
                >
                  <DropdownWithToggle
                    id={"type" + id}
                    name="type"
                    className={css(dropdown_item_styles.dropdown_align)}
                    toggleClass={css(
                      dropdown_item_styles.dropdown_toggle_align
                    )}
                    position={DropdownPosition.left}
                    dropdownItems={typeOptions}
                    value={ext["type"] || ""}
                    onSelectItem={(value, event) =>
                      handleInputChangeExtension(credentialId, event, value, id)
                    }
                  />
                </FormGroup>
              </GridItem>
              <GridItem span={4}>
                <FormGroup fieldId={"value" + id} label="Value">
                  <TextInput
                    id={"value" + id}
                    name="value"
                    onChange={(value, event) =>
                      handleInputChangeExtension(credentialId, event, value, id)
                    }
                  />
                </FormGroup>
              </GridItem>
              <GridItem span={1}>
                <Button
                  style={{ marginTop: "32px" }}
                  variant="link"
                  type="button"
                  icon={<MinusCircleIcon />}
                  onClick={() =>
                    onDeleteExtension(credentialId, "extensions", id)
                  }
                />
              </GridItem>
              <br />
            </Grid>
          );
        })}
    </>
  );
};
