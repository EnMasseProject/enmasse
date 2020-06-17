/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Grid,
  GridItem,
  Button,
  ButtonVariant,
  Flex,
  FlexItem,
  Title
} from "@patternfly/react-core";
import { StyleSheet } from "@patternfly/react-styles";
import { CreateMetadata } from "modules/iot-device/components";
import { useStoreContext, types } from "context-state-reducer";

const styles = StyleSheet.create({
  title: {
    paddingLeft: 20
  },
  header: {
    paddingLeft: 40
  }
});

export interface IEditMetadataContainerProps {
  title?: string;
}

export const EditMetadataContainer: React.FC<IEditMetadataContainerProps> = ({
  title
}) => {
  const { dispatch } = useStoreContext();

  const resetActionType = () => {
    dispatch({ type: types.RESET_DEVICE_ACTION_TYPE });
  };

  const onCancel = () => {
    resetActionType();
  };

  const onSave = () => {
    /**
     * TODO: implement save metadata query
     */
    resetActionType();
  };

  return (
    <>
      <div className={styles.header}>
        <Title size={"2xl"}>Edit device metadata</Title>
        <br />
        <Grid gutter="sm">
          <GridItem span={6}>
            <CreateMetadata />
          </GridItem>
        </Grid>
        <br />
        <Flex>
          <FlexItem>
            <Button
              id="em-save-metadata-button"
              variant={ButtonVariant.primary}
              onClick={onSave}
            >
              Save
            </Button>
          </FlexItem>
          <FlexItem>
            <Button
              id="em-cancel-metadata-button"
              variant={ButtonVariant.link}
              onClick={onCancel}
            >
              Cancel
            </Button>
          </FlexItem>
        </Flex>
      </div>
    </>
  );
};
