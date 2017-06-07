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

package enmasse.controller.common;

/**
 * Template parameters that are dynamically set by the address controller.
 */
public interface TemplateParameter {
    String NAME = "NAME";
    String ADDRESS = "ADDRESS";
    String INSTANCE = "INSTANCE";
    String GROUP_ID = "GROUP_ID";
    String MESSAGING_HOSTNAME = "MESSAGING_HOSTNAME";
    String MQTT_HOSTNAME = "MQTT_GATEWAY_HOSTNAME";
    String CONSOLE_HOSTNAME = "CONSOLE_HOSTNAME";
    String API_SERVER_HOSTNAME = "API_SERVER_HOSTNAME";

    String KAFKA_BOOTSTRAP_SERVERS = "KAFKA_BOOTSTRAP_SERVERS";
    String ROUTER_SECRET = "ROUTER_SECRET";
    String MQTT_SECRET = "MQTT_SECRET";
    String COLOCATED_ROUTER_SECRET = "COLOCATED_ROUTER_SECRET";
}
