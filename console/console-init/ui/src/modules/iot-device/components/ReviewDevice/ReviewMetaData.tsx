import React, { useState } from "react";
import { Grid, GridItem, TextInput, Button } from "@patternfly/react-core";
import { convertJsonToMetadataOptions } from "utils";
import { StyleSheet, css } from "aphrodite";
import { AngleRightIcon, AngleDownIcon } from "@patternfly/react-icons";

const styles = StyleSheet.create({
  grid_align: { marginTop: 10, marginRight: 5, marginLeft: 5 },
  grid_button_align: {
    textAlign: "right",
    marginTop: 10,
    marginRight: 5,
    marginLeft: 5
  },
  text_center_align: { textAlign: "center" }
});
interface IMetaDataReviewRowProps {
  values: any[];
  prevkey?: string;
}

const MetaDataReviewRow: React.FC<IMetaDataReviewRowProps> = ({
  values,
  prevkey
}) => {
  return (
    <>
      {values.map((val, index) => (
        <GetDevice value={val} prevKey={prevkey} index={index} />
      ))}
    </>
  );
};

interface IRowProps {
  value: any;
  prevKey?: string;
  index?: number;
}

const GetDevice: React.FunctionComponent<IRowProps> = ({
  value,
  prevKey,
  index
}) => {
  const [isVisible, setIsVisible] = useState<boolean>(false);

  const onToggle = () => {
    setIsVisible(!isVisible);
  };
  const renderGridItem = (value: string) => {
    return (
      <GridItem span={3} className={css(styles.grid_align)}>
        <TextInput
          key={value + index}
          id={value + index}
          type="text"
          value={value}
          isReadOnly={true}
        />
      </GridItem>
    );
  };

  const hasObjectValue: boolean =
    value.type === "array" || value.type === "object";
  const key: string = prevKey
    ? value.key !== ""
      ? prevKey + "/" + value.key
      : prevKey
    : value.key;
  return (
    <>
      <Grid>
        <GridItem span={1} className={css(styles.grid_button_align)}>
          {hasObjectValue && (
            <Button
              variant="plain"
              id={key}
              style={{ textAlign: "right" }}
              onClick={onToggle}
            >
              {isVisible ? <AngleDownIcon /> : <AngleRightIcon />}
            </Button>
          )}
        </GridItem>
        {renderGridItem(key)}
        {renderGridItem(value.typeLabel)}
        {renderGridItem(hasObjectValue ? "" : value.value)}
        {hasObjectValue && isVisible && (
          <>
            <MetaDataReviewRow values={value.value} prevkey={key} />
          </>
        )}
      </Grid>
    </>
  );
};

interface IReviewMetaDataProrps {
  defaults?: any[];
  ext?: any[];
  label: string;
}
const ReviewMetaData: React.FunctionComponent<IReviewMetaDataProrps> = ({
  defaults,
  ext,
  label
}) => {
  if (ext === undefined && defaults === undefined) {
    return <>--</>;
  }
  const convertedDefaultsData = convertJsonToMetadataOptions(defaults);
  const convertedExtData = convertJsonToMetadataOptions(ext);
  if (convertedDefaultsData.length === 0 && convertedExtData.length === 0) {
    return <>--</>;
  }
  const renderGrid = (values: any[]) => {
    return (
      <>
        <Grid>
          <GridItem span={1}></GridItem>
          <GridItem span={3} className={css(styles.text_center_align)}>
            <b>{label}</b>
          </GridItem>
          <GridItem span={3} className={css(styles.text_center_align)}>
            <b>Type</b>
          </GridItem>
          <GridItem span={3} className={css(styles.text_center_align)}>
            <b>Value</b>
          </GridItem>
        </Grid>
        <MetaDataReviewRow values={values} />
      </>
    );
  };
  return (
    <>
      {convertedDefaultsData.length > 0 && renderGrid(convertedDefaultsData)}
      {convertedExtData.length > 0 && renderGrid(convertedExtData)}
    </>
  );
};

export { ReviewMetaData };
