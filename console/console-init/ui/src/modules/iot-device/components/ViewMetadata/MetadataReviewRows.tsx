/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MetadataReviewRow } from "./MetadataReviewRow";
import { DataType } from "constant";
interface IMetadataReviewRowProps {
  values: any[];
  prevkey?: string;
}

const MetadataReviewRows: React.FC<IMetadataReviewRowProps> = ({
  values,
  prevkey
}) => {
  return (
    <>
      {values.map((val, index) => (
        <>
          <MetadataReviewRow value={val} prevKey={prevkey} />
          {val.type === DataType.ARRAY ||
            (val.type === DataType.OBJECT && <br />)}
        </>
      ))}
    </>
  );
};

export { MetadataReviewRows };
