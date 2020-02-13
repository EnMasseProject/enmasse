import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';

import * as service from './EnmasseAddressSpaces';

describe('load brokered and standard plans', () => {

  const testStandardPlans = {
    "apiVersion": "enmasse.io/v1beta1",
    "kind": "AddressSpaceSchema",
    "metadata": {"annotations": {}, "creationTimestamp": "2019-04-06T00:55:56.667Z", "labels": {}, "name": "standard"},
    "spec": {
      "description": "A standard address space consists of an AMQP router network in combination with attachable 'storage units'. The implementation of a storage unit is hidden from the client and the routers with a well defined API.",
      "addressTypes": [{
        "name": "anycast",
        "description": "A direct messaging address type. Messages sent to an anycast address are not stored but forwarded directly to a consumer.",
        "plans": [{
          "name": "standard-small-anycast",
          "description": "Creates a small anycast address.",
          "resources": {"router": 0.001}
        }, {
          "name": "standard-large-anycast",
          "description": "Creates a large anycast address.",
          "resources": {"router": 0.1}
        }, {
          "name": "standard-medium-anycast",
          "description": "Creates a medium anycast address.",
          "resources": {"router": 0.01}
        }]
      }, {
        "name": "multicast",
        "description": "A direct messaging address type. Messages sent to a multicast address are not stored but forwarded directly to multiple consumers.",
        "plans": [{
          "name": "standard-large-multicast",
          "description": "Creates a large multicast address.",
          "resources": {"router": 0.1}
        }, {
          "name": "standard-medium-multicast",
          "description": "Creates a medium multicast address.",
          "resources": {"router": 0.01}
        }, {
          "name": "standard-small-multicast",
          "description": "Creates a small multicast address.",
          "resources": {"router": 0.001}
        }]
      }, {
        "name": "queue",
        "description": "A store-and-forward queue. A queue may be sharded across multiple storage units, in which case message ordering is no longer guaranteed.",
        "plans": [{
          "name": "standard-medium-queue",
          "description": "Creates a medium sized queue sharing underlying broker with other queues.",
          "resources": {"broker": 0.1, "router": 0.01}
        }, {
          "name": "standard-xlarge-queue",
          "description": "Creates an extra large queue backed by 2 brokers.",
          "resources": {"broker": 2.0, "router": 0.2}
        }, {
          "name": "standard-large-queue",
          "description": "Creates a large queue backed by a dedicated broker.",
          "resources": {"broker": 1.0, "router": 0.1}
        }, {
          "name": "standard-small-queue",
          "description": "Creates a small queue sharing underlying broker with other queues.",
          "resources": {"broker": 0.01, "router": 0.001}
        }]
      }, {
        "name": "topic",
        "description": "A topic address for store-and-forward publish-subscribe messaging. Each message published to a topic address is forwarded to all subscribes on that address.",
        "plans": [{
          "name": "standard-small-topic",
          "description": "Creates a small topic sharing underlying broker with other topics.",
          "resources": {"broker": 0.01, "router": 0.001}
        }, {
          "name": "standard-large-topic",
          "description": "Creates a large topic backed by a dedicated broker.",
          "resources": {"broker": 1.0, "router": 0.1}
        }, {
          "name": "standard-xlarge-topic",
          "description": "Creates an extra large topic backed by 2 brokers.",
          "resources": {"broker": 2.0, "router": 0.2}
        }, {
          "name": "standard-medium-topic",
          "description": "Creates a medium sized topic sharing underlying broker with other topics.",
          "resources": {"broker": 0.1, "router": 0.01}
        }]
      }, {
        "name": "subscription",
        "description": "A subscription on a topic",
        "plans": [{
          "name": "standard-small-subscription",
          "description": "Creates a small durable subscription on a topic.",
          "resources": {"broker": 0.01, "router": 0.001}
        }, {
          "name": "standard-large-subscription",
          "description": "Creates a large durable subscription on a topic.",
          "resources": {"broker": 1.0, "router": 0.1}
        }, {
          "name": "standard-medium-subscription",
          "description": "Creates a medium durable subscription on a topic.",
          "resources": {"broker": 0.1, "router": 0.01}
        }]
      }],
      "plans": [{
        "name": "standard-small",
        "description": "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis",
        "resourceLimits": {"router": 1.0, "broker": 1.0, "aggregate": 2.0}
      }, {
        "name": "standard-medium",
        "description": "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis.",
        "resourceLimits": {"router": 3.0, "broker": 3.0, "aggregate": 6.0}
      }, {
        "name": "standard-unlimited",
        "description": "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis.",
        "resourceLimits": {"router": 10000.0, "broker": 10000.0, "aggregate": 10000.0}
      }, {
        "name": "standard-unlimited-with-mqtt",
        "description": "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis and MQTT support.",
        "resourceLimits": {"router": 10000.0, "broker": 10000.0, "aggregate": 10000.0}
      }],
      "authenticationServices": ["none-authservice"]
    }
  };
  const testBrokeredPlans = {
    "apiVersion": "enmasse.io/v1beta1",
    "kind": "AddressSpaceSchema",
    "metadata": {"annotations": {}, "creationTimestamp": "2019-04-05T23:40:54.142Z", "labels": {}, "name": "brokered"},
    "spec": {
      "description": "A brokered address space consists of a broker combined with a console for managing addresses.",
      "addressTypes": [{
        "name": "queue",
        "description": "A queue that supports selectors, message grouping and transactions",
        "plans": [{
          "name": "brokered-queue",
          "description": "Creates a queue on a broker.",
          "resources": {"broker": 0.0}
        }]
      }, {
        "name": "topic",
        "description": "A topic supports pub-sub semantics. Messages sent to a topic address is forwarded to all subscribes on that address.",
        "plans": [{
          "name": "brokered-topic",
          "description": "Creates a topic on a broker.",
          "resources": {"broker": 0.0}
        }]
      }],
      "plans": [{
        "name": "brokered-single-broker",
        "description": "Single Broker instance",
        "resourceLimits": {"broker": 1.9}
      }],
      "authenticationServices": ["none-authservice"]
    }
  };

  const expectedBrokeredPlanNames = [ "brokered-single-broker" ];

  const expectedStandardPlanNames = [ 'standard-small', 'standard-medium', 'standard-unlimited', 'standard-unlimited-with-mqtt' ];

  it('loadStandardAddressPlans should return plan names', (done) => {
    var mock = new MockAdapter(axios);
    const data = {response: testStandardPlans};
    mock.onGet('apis/enmasse.io/v1beta1/addressspaceschemas/standard').reply(200, testStandardPlans);

    service.loadStandardAddressPlans().then(response => {

      expect(response).toHaveLength(4);
      expect(response).toEqual(expectedStandardPlanNames);
      done();
    });
  });

  it('loadBrokeredAddressPlans should return plan names', (done) => {
    var mock = new MockAdapter(axios);
    const data = {response: testBrokeredPlans};
    mock.onGet('apis/enmasse.io/v1beta1/addressspaceschemas/brokered').reply(200, testBrokeredPlans);

    service.loadBrokeredAddressPlans().then(response => {

      expect(response).toHaveLength(1);
      expect(response).toEqual(expectedBrokeredPlanNames);
      done();
    });
  });
});
