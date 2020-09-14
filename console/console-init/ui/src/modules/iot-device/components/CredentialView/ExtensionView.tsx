/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React from "react";
import { StyleSheet, css } from "aphrodite";
import {
  Card,
  Divider,
  GridItem,
  ExpandableSection,
  TextInput,
  Grid,
  Title
} from "@patternfly/react-core";
import { DropdownWithToggle } from "components";

const styles = StyleSheet.create({
  type_margin: {
    marginLeft: 10
  },
  card_margin: {
    margin: 40
  },
  dropdown_align: {
    display: "flex"
  },
  dropdown_toggle_align: {
    flex: "1"
  },
  grid_padding: {
    paddingLeft: 70,
    paddingRight: 70
  },
  grid_padding_right: {
    paddingRight: 60,
    marginBottom: 20
  },
  grid_item_margin: {
    marginBottom: 40
  }
});

export const ExtensionView = () => {
  return (
    <Card className={css(styles.card_margin)}>
      <ExpandableSection toggleText="Extension">
        <Divider component="div" />
        <Grid className={css(styles.grid_padding)}>
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
        </Grid>
        <Grid className={css(styles.grid_padding)}>
          <GridItem span={4} className={css(styles.grid_padding_right)}>
            <TextInput id="extension-view-parameter-input" type="text" />
          </GridItem>
          <GridItem span={3} className={css(styles.grid_padding_right)}>
            <DropdownWithToggle
              id="extension-view-parameter-dropdown"
              toggleId="extension-view-parameter-dropdowntoggle"
              className={css(styles.dropdown_align)}
              toggleClass={css(styles.dropdown_toggle_align)}
              dropdownItems={["a", "b"]}
            />
          </GridItem>
          <GridItem span={5} className={css(styles.grid_padding_right)}>
            <TextInput id="extension-view-value-input" type="text" />
          </GridItem>
          <br />
        </Grid>
      </ExpandableSection>
    </Card>
  );
};
