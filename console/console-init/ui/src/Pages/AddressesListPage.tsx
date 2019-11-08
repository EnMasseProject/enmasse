import * as React from 'react';
import { Link } from 'react-router-dom';
import { PageSection, PageSectionVariants } from '@patternfly/react-core';

export default function AddressesListPage() {
    return(
        <PageSection variant={PageSectionVariants.light}>
        <h1>AddressSpaceListPage</h1>
        <Link to="/address-space/456/address/123">Address List Page</Link>
        </PageSection>
    )
}