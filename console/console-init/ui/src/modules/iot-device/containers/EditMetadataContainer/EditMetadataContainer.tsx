/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Grid,
  GridItem,
  Button,
  ButtonVariant,
  Flex,
  FlexItem,
  Title
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { CreateMetadata, IMetadataProps } from "modules/iot-device/components";

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
  onCancel: () => void;
}

export const EditMetadataContainer: React.FC<IEditMetadataContainerProps> = ({
  onCancel
}) => {
  const [metadata, setMetadata] = useState<IMetadataProps[]>([]);
  /**
   * Todo: add get metadata list query
   */
  const shouldDisabledSaveButton = () => {
    if (metadata?.length > 0) {
      return false;
    }
    return true;
  };

  const onSave = () => {
    /**
     * TODO: implement save metadata query
     */
    onCancel();
  };

  return (
    <>
      <div className={css(styles.header)}>
        <Title headingLevel="h2" size="xl">
          Edit device metadata
        </Title>
        <br />
        <Grid hasGutter>
          <GridItem span={6}>
            <CreateMetadata
              metadataList={[]}
              returnMetadataList={setMetadata}
            />
          </GridItem>
        </Grid>
        <br />
        <Flex>
          <FlexItem>
            <Button
              id="edit-metadata-save-button"
              variant={ButtonVariant.primary}
              onClick={onSave}
              isDisabled={shouldDisabledSaveButton()}
            >
              Save
            </Button>
          </FlexItem>
          <FlexItem>
            <Button
              id="edit-metadata-cancel-button"
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
