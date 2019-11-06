
var fs = require("fs");
var jison = require("jison");
var jp = require('jsonpath');
var path = require('path');


var bnf = fs.readFileSync(path.resolve(__dirname, 'filter.jison'), "utf8");
var parser = new jison.Parser(bnf);
const escapeStringRegexp = require('escape-string-regexp');

parser.yy = {
    booleanExpression: function(x) {
        return {evaluate: function (target) {
                return x.evaluate(target);
            }};
    },
    binaryExpression: {
        equalsExpression: function (x, y) {
            return {
                evaluate : function(target) {
                    return x.evaluate(target) === y.evaluate(target);
                }
            }
        },
        notEqualsExpression: function (x, y) {
            return {
                evaluate : function(target) {
                    return x.evaluate(target) !== y.evaluate(target);
                }
            }
        },
        greaterThanEqualsExpression: function (x, y) {
            return {
                evaluate : function(target) {
                    return x.evaluate(target) >= y.evaluate(target);
                }
            }
        },
        greaterThanExpression: function (x, y) {
            return {
                evaluate : function(target) {
                    return x.evaluate(target) > y.evaluate(target);
                }
            }
        },
        lessThanEqualsExpression: function (x, y) {
            return {
                evaluate : function(target) {
                    return x.evaluate(target) <= y.evaluate(target);
                }
            }
        },
        lessThanExpression: function (x, y) {
            return {
                evaluate : function(target) {
                    return x.evaluate(target) < y.evaluate(target);
                }
            }
        },
        likeExpression: function (x, y) {
            return {
                evaluate : function(target) {
                    var lhs = x.evaluate(target);
                    var rhs = escapeStringRegexp(y.evaluate(target));
                    // TODO escape quote symbols
                    var re = new RegExp("^" + rhs.replace(/%/g, ".*").replace(/_/g, ".") + "$");
                    var res = re.exec(lhs);
                    return res !== null;
                }
            }
        }

    },
    logicExpression: {
        createAnd: function (x, y) {
            return {
                evaluate : function(target) {
                    return x.evaluate(target) && y.evaluate(target);
                }
            }
        },
        createOr: function (x, y) {
            return {
                evaluate : function(target) {
                    return x.evaluate(target) || y.evaluate(target);
                }
            }
        }
    },
    unaryExpression: {
        createNot: function (x) {
            return {
                evaluate: function (target) {
                    return !x.evaluate(target);
                }
            }
        }
    },
    constantExpression: {
        createString: function(s) {
            return {
                evaluate : function(target) {
                    return s.substring(1, s.length - 1);
                }
            };
        },
        createJsonPath: function(s) {
            s = s.substring(1, s.length - 1);
            jp.parse(s);
            return {
                evaluate : function(target) {
                    var result = jp.query(target, s, 1);
                    return result.length ? result[0] : undefined;
                }
            };
        }
    }

};

module.exports = parser;
