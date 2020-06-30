/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { formatRelative } from "date-fns";
import { ArgumentsType } from "./types";

export interface IFormatRelativeProps {
  date: string | Date | number;
  base?: Date;
  options?: ArgumentsType<typeof formatRelative>[2];
}

export const FormatRelative: React.FunctionComponent<IFormatRelativeProps> = ({
  date,
  base = new Date(),
  options
}) => {
  date = typeof date === "string" ? new Date(date) : date;
  return <>{formatRelative(date, base, options)}</>;
};
