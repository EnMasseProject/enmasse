import React, { useState } from "react";
import {
  Split,
  SplitItem,
  Title,
  Flex,
  FlexItem,
  DropdownItem,
  DropdownPosition,
  PageSection,
  PageSectionVariants,
  Badge,
  Switch
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";
import { DropdownWithKebabToggle } from "components";

interface IIoTProjectDetailHeaderProps {
  projectName: string;
  type: string;
  status: string;
  isEnabled: boolean;
  changeEnable: (value: boolean) => void;
  onEdit: (name: string) => void;
  onDelete: (name: string) => void;
}

export const project_header_styles = StyleSheet.create({
  flex_left_border: {
    paddingLeft: "1em",
    borderLeft: "0.05em solid",
    borderRightColor: "lightgrey"
  },
  address_space_icon_margin: {
    backgroundColor: "#EC7A08",
    fontSize: 25
  },
  address_icon_align: {
    paddingTop: 5,
    paddingRight: 16
  },
  kebab_toggle_margin: {
    marginTop: 20,
    marginLeft: 10,
    fontSize: 15
  },
  namespace_info_margin: {
    marginTop: 16,
    marginBottom: 24
  },
  no_bottom_padding: {
    paddingBottom: 0
  },
  badge_style: {
    backgroundColor: "#EC7A08",
    fontSize: 25,
    paddingLeft: 15,
    paddingRight: 15,
    paddingTop: 5,
    paddingBottom: 5
  }
});

const IoTProjectDetailHeader: React.FunctionComponent<IIoTProjectDetailHeaderProps> = ({
  projectName,
  type,
  status,
  isEnabled,
  changeEnable,
  onEdit,
  onDelete
}) => {
  const AddressTitle = () => (
    <Split gutter="md">
      <SplitItem>
        <Title headingLevel="h1" size="4xl" id="iot-project-name">
          {projectName}
        </Title>
      </SplitItem>
    </Split>
  );

  const AddressDetailInFlex = () => (
    <Flex className={css(project_header_styles.namespace_info_margin)}>
      <FlexItem id="iot-project-type">
        Type : <b>{type}</b>
      </FlexItem>
      <FlexItem
        id="iot-project-status"
        className={css(project_header_styles.flex_left_border)}
      >
        Status : <b>{status}</b>
      </FlexItem>
    </Flex>
  );

  const AddressDetailLayout = () => (
    <>
      <SplitItem className={css(project_header_styles.address_icon_align)}>
        <Badge className={css(project_header_styles.badge_style)}>IoT</Badge>
      </SplitItem>
      <SplitItem>
        <AddressTitle />
        <AddressDetailInFlex />
      </SplitItem>
    </>
  );
  const onChange = () => {
    changeEnable(!isEnabled);
  };
  const EnabledIcon = () => (
    <Switch
      id="iot-project-header-switch"
      isChecked={isEnabled}
      onChange={onChange}
    />
  );

  const KebabOptionsLayout = () => {
    const dropdownItems = [
      <DropdownItem
        id="iot-project-header-kebab-option-edit"
        key="edit"
        aria-label="edit"
        onClick={() => onEdit(projectName)}
      >
        Edit
      </DropdownItem>,
      <DropdownItem
        id="iot-project-header-kebab-option-delete"
        key="delete"
        aria-label="delete"
        onClick={() => onDelete(projectName)}
      >
        Delete
      </DropdownItem>
    ];
    return (
      <DropdownWithKebabToggle
        id="iot-project-header-kebab-dropdown"
        isPlain={true}
        position={DropdownPosition.right}
        toggleId="iot-project-header-kebab-toggle"
        dropdownItems={dropdownItems}
      />
    );
  };

  return (
    <PageSection
      variant={PageSectionVariants.light}
      className={css(project_header_styles.no_bottom_padding)}
    >
      <Split>
        <AddressDetailLayout />
        <SplitItem isFilled />
        <SplitItem className={css(project_header_styles.kebab_toggle_margin)}>
          <EnabledIcon />
        </SplitItem>
        <SplitItem className={css(project_header_styles.kebab_toggle_margin)}>
          <KebabOptionsLayout />
        </SplitItem>
      </Split>
    </PageSection>
  );
};

export { IoTProjectDetailHeader };
