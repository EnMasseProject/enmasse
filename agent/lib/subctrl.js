/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

var log = require('./log.js').logger();

function SubscriptionControl(pods) {
    this.pods = pods;
}

/**
 * Find a queue with the given name if it exists on the list of pods
 * provided. Returns a promise whose value is an object with fields
 * 'found' and 'pod'. The 'found' field indicates whether the queue
 * was found or not. If the queue was found, the 'pod' field indicates
 * the pod on which the broker that has the queue lives. If the queue
 * was not found, the 'pod' field is the pod whose broker has the
 * fewest queues.
 */
function find_queue (name, pods) {
    log.info('looking for ' + name + ' in ' + pods.length + ' pods');
    return Promise.all(pods.map(function (pod) { return pod.broker.getQueueNames(); } )).then(
        function (results) {
            var found = false;
            var pod = undefined;
            var min = undefined;
            for (var i = 0; !found && i < results.length; i++) {
                if (results[i].indexOf(name) >= 0) {
                    pod = pods[i];
                    found = true;
                } else if (min === undefined || min > results[i].length) {
                    pod = pods[i];
                }
            }
            return {'found':found, 'pod':pod};
        }
    );
};

/**
 * Create queue if one does not already exist. Returns a promise whose
 * value on resolution is the pod containing the broker on which the
 * queue exists.
 */
function ensure_queue (name, pods) {
    log.info('ensuring queue ' + name + ' in ' + pods.length + " pods");
    pods.map(function (pod) {
        log.info('podmap ' + pod.name + " with broker undefined: " + (pod.broker === undefined));
    });

    return find_queue(name, pods).then(
        function (result) {
            if (result.found) {
                return result.pod;
            } else {
                return result.pod.broker.deployQueue(name).then(
                    function () {
                        return result.pod;
                    }
                );
            }
        }
    );
};

/**
 * Returns a map of topic name to optional tag (as passed to subscribe
 * request). If no tag was specified, the value of the map is always
 * true.
 */
SubscriptionControl.prototype.list = function (subscription_id) {
    return find_queue(subscription_id, this.pods.pod_list()).then(function (result) {
        if (result.found) {
            return result.pod.broker.getDivertNames().then(
                function (names) {
                    return names.filter(
                        function (n) { return n.indexOf(subscription_id) === 0; }
                    ).reduce(
                        function (a, b) {
                            var parts = b.split('|');
                            a[parts[1]] = parts[2] || true;
                            return a;
                        },
                        {}
                    );
                }
            );
        } else {
            return {};
        }
    });
};

function get_divert_name(subscription_id, topic, tag) {
    return subscription_id + '|' + topic + '|' + (tag || '');
}

SubscriptionControl.prototype.subscribe = function (subscription_id, topics) {
    log.debug('subscribing to ' + JSON.stringify(topics));
    var pods = this.pods.pod_list();
    if (pods.length == 0) {
        return Promise.reject('No brokers available for subscribing to topics ' + JSON.stringify(Object.keys(topics)));
    }
    return ensure_queue(subscription_id, pods).then(
        function (pod) {
            log.info('after ensure looking at pod ' + JSON.stringify(pod.name) + " with broker undefined " + (pod.broker === undefined));
            return pod.broker.ensureConnectorService(subscription_id, subscription_id, subscription_id).then(
                function () {
                    return Promise.all(Object.keys(topics).map(
                        function (topic) {
                            var name = get_divert_name(subscription_id, topic, topics[topic]);
                            return pod.broker.ensureDivert(name, topic, subscription_id);
                        }
                    ));
                }
            );
        }
    );
};

function delete_diverts(broker, prefix) {
    function matches(n) { return n.indexOf(prefix) === 0; };
    function destroy(n) { return broker.destroyDivert(n); }
    return broker.getDivertNames().then(
        function (names) {
            return Promise.all(names.filter(matches).map(destroy));
        }
    );
};

SubscriptionControl.prototype.unsubscribe = function (subscription_id, topics) {
    return find_queue(subscription_id, this.pods.pod_list()).then(
        function(result) {
            if (result.found) {
                return Promise.all(Object.keys(topics).map(
                    function (topic) {
                        var name = get_divert_name(subscription_id, topic);
                        return delete_diverts(result.pod.broker, name);
                        //TODO: could delete queue and connector *if* there are no longer any diverts
                    }
                ));
            }
        }
    );
};

SubscriptionControl.prototype.close = function (subscription_id) {
    return find_queue(subscription_id, this.pods.pod_list()).then(
        function(result) {
            if (result.found) {
                return delete_diverts(result.pod.broker, subscription_id).then(
                    function () {
                        return result.pod.broker.destroyConnectorService(subscription_id).then(
                            function () {
                                //delete queue last as that is how we
                                //find the right broker to cleanup
                                return result.pod.broker.destroyQueue(subscription_id);
                            }
                        );
                    }
                );
            }
        }
    );
};


/**
 * Returns a new SubscriptionControl object with the following public methods:
 *
 *   pod_added
 *   pod_removed
 *   subscribe
 *   unsubscribe
 *   close
 *
 */
module.exports = function (pods) {
    return new SubscriptionControl(pods);
};
