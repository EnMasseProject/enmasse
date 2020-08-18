/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React from "react";
import { Title, ChipGroup, Chip } from "@patternfly/react-core";

export interface IChipGroupsProps {
  id: string;
  titleId?: string;
  items: string[];
  removeItem: (item: string) => void;
  title?: string;
}
export const ChipGroupsWithTitle: React.FC<IChipGroupsProps> = ({
  id,
  titleId,
  items,
  removeItem,
  title
}) => {
  return (
    <div id={id}>
      {title && items?.length > 0 && (
        <Title id={titleId} headingLevel="h6" size="md">
          {title}
        </Title>
      )}
      <ChipGroup id={id}>
        {items.map((item: string) => (
          <Chip
            key={item}
            id={`${id}-${item}`}
            value={item}
            onClick={() => removeItem(item)}
          >
            {item}
          </Chip>
        ))}
      </ChipGroup>
    </div>
  );
};
