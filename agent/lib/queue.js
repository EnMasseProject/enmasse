/*
 * Copyright 2017 Red Hat Inc.
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

var kubernetes = require('./kubernetes.js');
var log = require('./log.js').logger();
var pod_watcher = require('./pod_watcher');
var tls_options = require('./tls_options.js');
var artemis = require('./artemis.js');
var utils = require('./utils');

function Queue (addr) {
    this.addr = addr;
    this.name = addr.address;
};

function get_current_pods () {
    var options = {
        selector: 'role=broker'
    };

    if (process.env.INFRA_UUID) {
        var infraSelector = "infraUuid=" + process.env.INFRA_UUID;
        if (options.selector) {
            options.selector += "," + infraSelector;
        }
    }
    return kubernetes.get('pods', options);
}

function get_pods (pods) {
    var options = {
        selector: 'name in ('+pods.join()+')'
    };

    if (process.env.INFRA_UUID) {
        var infraSelector = "infraUuid=" + process.env.INFRA_UUID;
        if (options.selector) {
            options.selector += "," + infraSelector;
        }
    }
    return kubernetes.get('pods', options);
}

function get_broker_port(pod) {
    return utils.get(pod.ports, ['broker', 'amqp'], 5673);
};

function create_brokers(podDefs, address_space) {
    return podDefs.map(function (podDef){

        var options;
        if (address_space === 'brokered') {
            options = {
                username: 'console',
                id:     podDef.name,
                host:   process.env.BROKER_SERVICE_HOST,
                port:   process.env.BROKER_SERVICE_PORT
            };
        } else {
            options = {
                username:'anonymous',
                host:   podDef.host,
                port:   get_broker_port(podDef),
                id:     podDef.name,
            };
        }
        try {
            options = tls_options.get_client_options(options);
        } catch (error) {
            log.error(error);
        }

        return artemis.connect(options);
    });
};

function purge_brokered_queue(queueName) {
    return get_current_pods().then(
        function (currentPods) {

            var podDefs = currentPods.items.map(function (pod) {
                return pod_watcher.get_pod_definition(pod);
            });

            var broker_cons = create_brokers(podDefs, 'brokered');
            var purgedQueues = broker_cons.map(broker => broker.purgeQueue(queueName));

            //for the brokered address space, there will never be more then 1 broker.
            return Promise.all(purgedQueues)
                .then(sum_total_purged)
                .finally(() => {
                    Promise.all(broker_cons.map(c => c.close()))
                        .catch((e) => log.warn("Failed to close purge connection", e));
                });
        });
};

function sum_total_purged(results) {
    var totalPurged = 0;
    results.forEach((c) => totalPurged += c);
    log.debug("Purged %d message(s) from %d shard(s)", totalPurged, results.length);
    return totalPurged;
}

Queue.prototype.purge = function () {
    var queueName = this.name;
    var addr = this.addr;

    if (process.env.ADDRESS_SPACE_TYPE === 'brokered') {
        return purge_brokered_queue(queueName);
    } else {
        var brokerNames = addr.status.brokerStatuses.map(function (brokerStatus) {
            return brokerStatus.clusterId;
        });
       return get_pods(brokerNames).then(
           function (brokerPods) {
               var podDefs = brokerPods.items.map(function (pod) {
                   return pod_watcher.get_pod_definition(pod);
               });

               var broker_cons = create_brokers(podDefs, 'standard');
               var purgedQueues = broker_cons.map(broker => broker.purgeQueue(queueName));

               return Promise.all(purgedQueues)
                   .then(sum_total_purged)
                   .finally(() => {
                       Promise.all(broker_cons.map(c => c.close()))
                           .catch((e) => log.warn("Failed to close purge connection", e));
                   });

           });
    }
};


module.exports = function (name) {
    return new Queue(name);
};
