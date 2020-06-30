/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { format } from "date-fns";
import { ArgumentsType } from "./types";

export interface IFormatDateProps {
  date: string | Date | number;
  format?: string;
  options?: ArgumentsType<typeof format>[2];
}
export const FormatDate: React.FunctionComponent<IFormatDateProps> = ({
  date,
  format: formatTpl = "Pp",
  options
}) => {
  date = typeof date === "string" ? new Date(date) : date;
  return <>{format(date, formatTpl, options)}</>;
};
