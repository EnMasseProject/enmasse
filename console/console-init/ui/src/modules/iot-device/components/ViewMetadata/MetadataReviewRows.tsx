/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { Fragment } from "react";
import { MetadataReviewRow } from "./MetadataReviewRow";
import { DataType } from "constant";
interface IMetadataReviewRowProps {
  metadataRows: any[];
  prevkey?: string;
}

const MetadataReviewRows: React.FC<IMetadataReviewRowProps> = ({
  metadataRows,
  prevkey
}) => {
  return (
    <>
      {metadataRows.map((val, index) => (
        <Fragment key={`fragment-index-${index}`}>
          <MetadataReviewRow metadataRow={val} prevKey={prevkey} />
          {val.type === DataType.ARRAY ||
            (val.type === DataType.OBJECT && <br />)}
        </Fragment>
      ))}
    </>
  );
};

export { MetadataReviewRows };
