/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  Page,
  PageSection,
  Grid,
  GridItem,
  Card,
  CardBody,
  Title,
  DropdownPosition,
  Popover,
  PopoverPosition,
  Button,
  ButtonVariant
} from "@patternfly/react-core";
import { FilterIcon, ExclamationCircleIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "aphrodite";
import { DropdownWithToggle, IDropdownOption } from "components";
import {
  CredentialsView,
  ICredentialsViewProps
} from "modules/iot-device-detail/components";
import {
  credentialsTypeOptions,
  getDefaultCredentialsFiterOption
} from "modules/iot-device-detail/utils";
import { CredentialsType } from "constant";
import { AdapterListContainer } from "containers";

const style = StyleSheet.create({
  filter_dropdown: {
    paddingLeft: 20
  }
});

export interface IConfigurationInfoProps
  extends Pick<
    ICredentialsViewProps,
    "credentials" | "onConfirmCredentialsStatus"
  > {
  id: string;
  setFilterType: (value: string) => void;
  setFilterValue: (value: string) => void;
  filterType: string;
  filterValue: string;
}

export const ConfigurationInfo: React.FC<IConfigurationInfoProps> = ({
  id,
  credentials,
  setFilterType,
  setFilterValue,
  filterType,
  filterValue,
  onConfirmCredentialsStatus
}) => {
  const [filterOptions, setFilterOptions] = useState<IDropdownOption[]>([]);

  useEffect(() => {
    /**
     * dropdown options should prepare if selected value is all
     * so that dropdown can have all previous options
     */
    if (filterValue === "all") {
      getFilterOptions();
    }
  }, [filterType, credentials]);

  const onSelectCredentialType = (value: string) => {
    setFilterType(value);
    /**
     * reset default selected value of filter dropdown
     */
    setFilterValue("all");
  };

  const shouldDisplayChildDropdown = () => {
    if (
      filterType === CredentialsType.PASSWORD ||
      filterType === CredentialsType.PSK ||
      filterType === CredentialsType.X509_CERTIFICATE
    ) {
      return true;
    }
    return false;
  };

  const getFilterOptions = () => {
    const filterOptions = getDefaultCredentialsFiterOption(filterType);
    const defaultSelectedOption = filterValue
      ? filterValue
      : filterOptions[0]?.value;
    if (shouldDisplayChildDropdown() && credentials) {
      const newCredentials = [...credentials];
      newCredentials
        ?.filter((item: any) => item?.type === filterType)
        .forEach((item: any) => {
          const { "auth-id": authId } = item;
          filterOptions.push({
            key: authId,
            value: authId,
            label: `Auth ID: ${authId}`
          });
        });
    }
    shouldDisplayChildDropdown() && setFilterValue(defaultSelectedOption);
    setFilterOptions(shouldDisplayChildDropdown() ? filterOptions : []);
  };

  return (
    <>
      <Popover
        id="configuration-info-popover"
        position={PopoverPosition.bottom}
        bodyContent={
          <div>
            Use this information to set up a device for connection with an IoT
            service.
          </div>
        }
        aria-label="configuration info popover"
        closeBtnAriaLabel="close configuration info popover"
      >
        <Button
          variant={ButtonVariant.link}
          id="configuration-info-popover-button"
          icon={<ExclamationCircleIcon />}
        >
          What is the device configuration info for?
        </Button>
      </Popover>
      <Page id={id}>
        <PageSection>
          <Grid hasGutter>
            <GridItem span={6}>
              <Card>
                <CardBody>
                  <Title size="2xl" headingLevel="h1">
                    Adapters
                  </Title>
                  <br />
                  <AdapterListContainer id="device-detail-config-info-adapterlist" />
                </CardBody>
              </Card>
            </GridItem>
            <GridItem span={6}>
              <Grid hasGutter>
                <GridItem>
                  <Card>
                    <CardBody>
                      <DropdownWithToggle
                        id="device-detail-config-info-credential-type-dropdown"
                        toggleId={
                          "device-detail-config-info-credential-type-dropdowntoggle"
                        }
                        aria-label="Select credential type"
                        position={DropdownPosition.left}
                        onSelectItem={onSelectCredentialType}
                        dropdownItems={credentialsTypeOptions}
                        value={filterType && filterType.trim()}
                        isLabelAndValueNotSame={true}
                        toggleIcon={
                          <>
                            <FilterIcon />
                            &nbsp;
                          </>
                        }
                      />
                      {filterOptions?.length > 0 && (
                        <DropdownWithToggle
                          id="device-detail-config-info-credential-filter-dropdown"
                          toggleId={
                            "device-detail-config-info-credential-filter-dropdowntoggle"
                          }
                          aria-label="set filter value"
                          position={DropdownPosition.left}
                          onSelectItem={setFilterValue}
                          dropdownItems={filterOptions}
                          value={filterValue}
                          isLabelAndValueNotSame={true}
                          className={css(style.filter_dropdown)}
                        />
                      )}
                    </CardBody>
                  </Card>
                </GridItem>
              </Grid>
              <CredentialsView
                id="device-detail-config-info-credentials-view"
                credentials={credentials}
                onConfirmCredentialsStatus={onConfirmCredentialsStatus}
              />
            </GridItem>
          </Grid>
        </PageSection>
      </Page>
    </>
  );
};
