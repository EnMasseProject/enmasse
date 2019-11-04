import {
  Button,
  Dropdown,
  DropdownToggle,
  GridItem,
  Pagination,
  KebabToggle,
  DropdownPosition,
  ButtonVariant,
  TextInput,
  PageSection,
  InputGroup,
  Grid
} from "@patternfly/react-core";
import * as React from "react";
import { storiesOf } from "@storybook/react";
import { MemoryRouter } from "react-router";
import { action } from "@storybook/addon-actions";
import { SearchIcon } from "@patternfly/react-icons";
const stories = storiesOf("Console", module);

stories.add("Address Space Pagination Header", () => {
  return (
    <MemoryRouter>
      <PageSection variant={"light"}>
        <Grid style={{ height: "100vh" }}>
          <GridItem span={6}>
            <InputGroup>
              <Dropdown
                position="right"
                onSelect={action("Dropdown onSelect")}
                isOpen={false}
                toggle={
                  <DropdownToggle onToggle={action("Dropdown onToggle")}>
                    Name
                  </DropdownToggle>
                }
                dropdownItems={[]}
              />
              <TextInput
                name="search name"
                id="searchName"
                type="search"
                placeholder="Filter by name"
                aria-label="search input name"
              />
              <Button
                variant={ButtonVariant.control}
                aria-label="search button for search input"
              >
                <SearchIcon />
              </Button>
              <Button variant="primary">Create address</Button>
              <Dropdown
                isPlain
                position={DropdownPosition.right}
                isOpen={false}
                onSelect={action("dropdown select")}
                toggle={<KebabToggle onToggle={action("toggle function")} />}
                dropdownItems={[]}
              />
            </InputGroup>
          </GridItem>
          <GridItem span={6}>
            <Pagination
              itemCount={523}
              perPage={10}
              page={1}
              onSetPage={action("Page Changes")}
              widgetId="pagination-options-menu-top"
              onPerPageSelect={action("page count changes")}
            />
          </GridItem>
        </Grid>
      </PageSection>
    </MemoryRouter>
  );
});
