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

var Promise = require('bluebird');

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

SubscriptionControl.prototype.subscribe = function (subscription_id, topic) {
    return ensure_queue(subscription_id, this.pods.pod_list()).then(
        function (pod) {
            var name = subscription_id + ':' + topic;
            return pod.broker.ensureDivert(name, topic, subscription_id).then(
                function () {
                    return pod.router.ensure_auto_link({'name':subscription_id, 'addr':subscription_id, dir:'out', connection:'broker'});
                }
            );
        }
    );
};

SubscriptionControl.prototype.unsubscribe = function (subscription_id, topic) {
    return find_queue(subscription_id, this.pods.pod_list()).then(
        function(result) {
            if (result.found) {
                var name = subscription_id + ':' + topic;
                return result.pod.broker.destroyDivert(name);
            }
        }
    );
};

function delete_diverts(broker, subscription_id) {
    function matches(n) { return n.indexOf(subscription_id) === 0; };
    function destroy(n) { return broker.destroyDivert(n); }
    return broker.getDivertNames().then(
        function (names) {
            return Promise.all(names.filter(matches).map(destroy));
        }
    );
};

function delete_auto_link(router, subscription_id) {
    return router.get_auto_links().then(
        function (links) {
            if (links.some(function (l) { return l.name === subscription_id; })) {
                return router.delete_auto_link({'name':subscription_id});
            }
        }
    );
};

SubscriptionControl.prototype.close = function (subscription_id) {
    return find_queue(subscription_id, this.pods.pod_list()).then(
        function(result) {
            if (result.found) {
                return delete_auto_link(result.pod.router, {'name':subscription_id}).then(
                    function () {
                        return delete_diverts(result.pod.broker, subscription_id).then(
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
