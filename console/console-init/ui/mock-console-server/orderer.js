/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

const firstBy = require('thenby');
const jp = require('jsonpath');


function orderer(sort_spec) {
    if (sort_spec) {
        return (r1, r2)  => {
            var by = firstBy.firstBy((a, b) => 0);

            sort_spec.split(/\s*,\s*/).forEach(spec => {
                var match = /^`(.+)`\s*(asc|desc)?$/i.exec(spec);
                var compmul = match.length > 2 && match[2] && match[2].toLowerCase() === "desc" ? -1 : 1;

                var path = match[1];
                var result1 = jp.query(r1, path, 1);
                var result2 = jp.query(r2, path, 1);

                var value1 = result1.length ? result1[0] : undefined;
                var value2 = result2.length ? result2[0] : undefined;

                var cmp = function () {
                    // Implements SQL 'nulls first'
                    if (value1 === undefined && value2 === undefined) {
                        return 0;
                    } else if (value1 === undefined) {
                        return  -1 * compmul;
                    } else if (value2 === undefined) {
                        return compmul;
                    }
                    return (value1 < value2) ? -1 * compmul : (value1 > value2 ? compmul : 0);
                };
                by = by.thenBy(cmp);
            });

            return by(r1, r2);
        };
    } else {
        return (r1, r2) => (a, b) => 0;
    }
}

module.exports = orderer;
