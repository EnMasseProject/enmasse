/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */


const assert = require('assert');
const {makeExecutableSchema} = require('apollo-server');
const {graphql} = require('graphql');
const {resolvers} = require('../mock-console-server');
const typeDefs = require('../schema');

// based on work from: https://hackernoon.com/extensive-graphql-testing-57e8760f1c25


const allAddressesForAddressspace = {
    id: 'all_addresses_for_addressspace',
    query: `
        query all_addresses_for_addressspace {
          addresses(
            filter: "\`$.spec.addressSpace\` = 'mars_as2' AND \`$.metadata.namespace\` = 'app2_ns'",
            orderBy: "\`$.metadata.name\` DESC"
          ) {
            total
            addresses {
              metadata {
                namespace
                name
              }
            }
          }
        }
   `,
    variables: {},

    expected: {
        data: {
            addresses: {
                "addresses": [
                    {
                        "metadata": {
                            "name": "mars_as2.phobos",
                            "namespace": "app2_ns"
                        }
                    },
                    {
                        "metadata": {
                            "name": "mars_as2.deimous",
                            "namespace": "app2_ns"
                        }
                    }
                ],
                "total": 2
            }
        }
    }
};

const mutateAddressSpacePlan = {
    id: 'mutateAddressSpacePlan',
    query: `
        mutation patch_as(
          $as: ObjectMeta_v1_Input!
          $jsonPatch: String!
          $patchType: String!
        ) {
          patchAddressSpace(input: $as, jsonPatch: $jsonPatch, patchType: $patchType)
        }
   `,
    variables: {
        "as": {"name": "jupiter_as1", "namespace": "app1_ns" },
        "jsonPatch": "[{\"op\":\"replace\",\"path\":\"/spec/plan\",\"value\":\"standard-medium\"}]",
        "patchType": "application/json-patch+json"
    },

    expected: {
        data: {
            patchAddressSpace: true
        }
    },
};

describe('GraphQL Test Cases', () => {
    const cases = [
        allAddressesForAddressspace,
        mutateAddressSpacePlan];
    const schema = makeExecutableSchema({typeDefs, resolvers});

    cases.forEach(obj => {
        const {id, query, variables, context, expected} = obj;

        it(`${id}`, async () => {
            const result = await graphql(schema, query, null, context, variables);
            assert.deepEqual(expected, result);
        });
    });
})