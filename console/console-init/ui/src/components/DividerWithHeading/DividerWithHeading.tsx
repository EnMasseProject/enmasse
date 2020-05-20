import React from "react";
import { Divider } from "@patternfly/react-core";
import { StyleSheet } from "@patternfly/react-styles";

const styles = StyleSheet.create({
  container: {
    display: "flex",
    marginTop: "var(--pf-global--spacer--md)"
  },
  divider_align: {
    marginTop: 12,
    marginLeft: 15
  }
});

export interface IDividerWithHeading {
  heading: string;
}

export const DividerWithHeading: React.FC<IDividerWithHeading> = ({
  heading
}) => {
  return (
    <div className={styles.container}>
      {heading}
      <Divider className={styles.divider_align} />
    </div>
  );
};
