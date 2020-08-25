/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MetaDataReviewRow } from "./MetaDataReviewRow";
interface IMetaDataReviewRowProps {
  values: any[];
  prevkey?: string;
}

const MetaDataReviewRows: React.FC<IMetaDataReviewRowProps> = ({
  values,
  prevkey
}) => {
  return (
    <>
      {values.map((val, index) => (
        <>
          <MetaDataReviewRow value={val} prevKey={prevkey} index={index} />
          {val.type === "array" || (val.type === "object" && <br />)}
        </>
      ))}
    </>
  );
};

export { MetaDataReviewRows };
