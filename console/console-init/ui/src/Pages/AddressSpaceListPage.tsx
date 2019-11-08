import * as React from 'react';
import { Link } from 'react-router-dom';
import { PageSection, PageSectionVariants } from '@patternfly/react-core';

export default function AddressSpaceListPage() {
    return(
        <PageSection variant={PageSectionVariants.light}>
        <h1>AddressSpaceListPage</h1>
        <Link to="address-space/123/addresses">Address Space Detail Page</Link>
        </PageSection>
    )
}