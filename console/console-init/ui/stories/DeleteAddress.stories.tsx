import * as React from "react";
import { MemoryRouter } from "react-router";
import { storiesOf } from "@storybook/react";
import { Button } from "@patternfly/react-core";
import { DeletePrompt } from "src/Components/Common/DeletePrompt";
import { text } from "@storybook/addon-knobs";

const stories = storiesOf("Address", module);

stories.add("Delete Address Prompt",()=>{
    const [isOpen, setIsOpen] = React.useState(false);
    return(
        <MemoryRouter>
            <Button onClick={()=>{setIsOpen(!isOpen)}}>Open Modal On Delete</Button>
                <DeletePrompt
                    header={text("Header","Delete the Address ?")}
                    name={text("Name at top of detials","leo_b")}
                    detail={text("Details","There are some description that telling users what would happenafter deleting this address.")}
                    isOpen={isOpen}
                    setIsOpen={setIsOpen}
                />
        </MemoryRouter>
    )
})
