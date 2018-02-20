/*
 * Copyright 2018 Red Hat Inc.
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

function is_pod_ready(pod) {
    return pod.ready === 'True' && pod.phase === 'Running';
}

module.exports = function (topics, create_topic) {
    return function (pods) {
        var by_topic = {};
        pods.filter(is_pod_ready).forEach(function (pod) {
            var key = pod.annotations.address;
            var list = by_topic[key];
            if (list === undefined) {
                list = [];
                by_topic[key] = list;
            }
            list.push(pod);
        });
        for (var name in by_topic) {
            var topic = topics[name];
            if (topic === undefined) {
                topic = create_topic(name);
                topics[name] = topic;
            }
            topic.pods.update(by_topic[name]);
        }
        for (var name in topics) {
            if (by_topic[name] === undefined) {
                topics[name].close();
            }
            if (topics[name].empty()) {
                delete topics[name];
            }
        }
    };
};

