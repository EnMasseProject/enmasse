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

package io.enmasse.artemis.sasl_delegation;

import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.SecuritySettingPlugin;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.jboss.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SaslGroupBasedSecuritySettingsPlugin implements SecuritySettingPlugin {

    private static final Logger LOG = Logger.getLogger(SaslGroupBasedSecuritySettingsPlugin.class);

    private static final Map<String, SaslGroupBasedSecuritySettingsPlugin> INSTANCES = new ConcurrentHashMap<>();

    private static final String NAME = "name";
    private static final String USE_GROUPS_FROM_SASL_DELEGATION = "useGroupsFromSaslDelegation";
    private static final String ADMIN_GROUP = "admin";
    private static final String MANAGE_GROUP = "manage";
    private static final String ALL_GROUP = "all";
    private String name;
    private HierarchicalRepository<Set<Role>> securityRepository;
    private final Map<String, Set<String>> knownAddresses = new HashMap<>();
    private Set<Role> standardRoles;
    private boolean useGroupsFromSaslDelegation;

    @Override
    public SecuritySettingPlugin init(Map<String, String> map) {
        this.name = map.get(NAME);
        if(this.name != null) {
            INSTANCES.put(this.name, this);
        }
        this.useGroupsFromSaslDelegation = "true".equalsIgnoreCase(map.get(USE_GROUPS_FROM_SASL_DELEGATION));
        Set<Role> roles = new HashSet<>();

        // "admin" (console or other internal process) can do anything
        roles.add(new Role(ADMIN_GROUP, true, true, true, true, true, true, true, true, true, true));

        if(!useGroupsFromSaslDelegation) {
            // "all" users can create/delete queues (but not addresses)
            roles.add(new Role(ALL_GROUP, true, true, true, true, true, true, false, true, false, false));
            roles.add(new Role(MANAGE_GROUP, true, true, true, true, true, true, true, true, false, false));
        }

        this.standardRoles = Collections.unmodifiableSet(roles);

        return this;
    }

    @Override
    public SecuritySettingPlugin stop() {
        if(this.name != null) {
            INSTANCES.remove(this.name);
        }
        return this;
    }

    @Override
    public Map<String, Set<Role>> getSecurityRoles() {

        Map<String, Set<Role>> securityRoles = Collections.singletonMap("#", this.standardRoles);
        return securityRoles;
    }

    @Override
    public void setSecurityRepository(HierarchicalRepository<Set<Role>> hierarchicalRepository) {
        this.securityRepository = hierarchicalRepository;
    }

    static SaslGroupBasedSecuritySettingsPlugin getInstance(String name) {
        return INSTANCES.get(name);
    }

    synchronized void addGroups(List<String> groups) {
        if(useGroupsFromSaslDelegation) {
            LOG.debugv("Adding groups: {0}", groups);
            for (String group : groups) {
                addGroup(group);
            }
        }
    }

    private void addGroup(String group) {
            String[] parts = group.split("_", 2);
            if (parts.length == 2) {
                try {
                    String encodedAddress = parts[1];
                    String address = URLDecoder.decode(encodedAddress, StandardCharsets.UTF_8.name());
                    knownAddresses.putIfAbsent(address, new HashSet<>());
                    if(knownAddresses.get(address).add(encodedAddress)) {

                        Set<Role> roles = new HashSet<>();

                        if(address.equals("#")) {
                            roles.addAll(standardRoles);
                        }

                        LOG.debugv("Adding permissions for address {0} due to group {1}", address, group);
                        for(String encoded : knownAddresses.get(address)) {
                            roles.add(new Role("send_" + encoded, true, false, false, false, false, false, false, false, false, false));
                            roles.add(new Role("consume_" + encoded, false, true, false, false, true, true, false, true, false, false));
                            roles.add(new Role("browse_" + encoded, false, false, false, false, false, false, false, true, false, false));
                        }
                        securityRepository.addMatch(address, roles);

                    }
                } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                    LOG.infov("Unable to parse implied address from group {0}: {1}", group, e.getMessage());
                }
            }

    }
}
