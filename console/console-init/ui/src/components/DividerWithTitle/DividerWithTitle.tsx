/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Divider } from "@patternfly/react-core";
// import { StyleSheet } from "@patternfly/react-styles";

// const styles = StyleSheet.create({
//   container: {
//     display: "flex",
//     marginTop: "var(--pf-global--spacer--md)"
//   },
//   divider_align: {
//     marginTop: 12,
//     marginLeft: 15
//   }
// });

export interface IDividerWithTitle {
  title: string;
}

export const DividerWithTitle: React.FC<IDividerWithTitle> = ({ title }) => {
  return (
    <div
    // className={styles.container}
    >
      {title}
      <Divider
      // className={styles.divider_align}
      />
    </div>
  );
};
