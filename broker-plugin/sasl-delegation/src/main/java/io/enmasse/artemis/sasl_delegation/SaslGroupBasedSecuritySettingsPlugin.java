/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.artemis.sasl_delegation;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.SecuritySettingPlugin;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.jboss.logging.Logger;

@SuppressWarnings("serial")
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
    private final Set<String> knownAddresses = new HashSet<>();
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

    private static final char DEFAULT_SINGLE_WORD = '+';

    private static final char DEFAULT_ANY_WORDS = '#';

    private static final char DEFAULT_DELIMITER = '/';

    private void addGroup(String group) {
            String[] parts = group.split("_", 2);
            if (parts.length == 2) {
                char singleWord = DEFAULT_SINGLE_WORD;
                char anyWords = DEFAULT_ANY_WORDS;
                char delimeter = DEFAULT_DELIMITER;
                try {
                    String address = parts[1].replace('*', '#');
                    if(knownAddresses.add(address)) {

                        String singleWordString = String.valueOf(singleWord);
                        String delimeterString = String.valueOf(delimeter);
                        String anyWordsString = String.valueOf(anyWords);

                        Set<Role> roles = new HashSet<>();
                        LOG.debugv("Adding permissions for address {0} due to group {1}", address, group);
                        roles.add(new Role("send_" + address, true, false, false, false, true, false, false, false, true, false));
                        roles.add(new Role("recv_" + address, false, true, true, true, true, true, false, true, true, false));
                        roles.add(new Role("browse_" + address, false, false, false, false, false, false, false, true, false, false));
                        roles.add(new Role("create_" + address, false, false, true, false, true, false, false, false, true, false));
                        roles.add(new Role("delete_" + address, false, false, false, true, false, true, false, false, false, true));
                        roles.add(new Role("manage_" + address, false, false, false, false, false, false, true, false, false, false));

                        Set<Role> allRoles = new HashSet<>(securityRepository.getMatch(address));
                        allRoles.addAll(roles);
                        securityRepository.addMatch(address, allRoles);

                        if (address.equals(anyWordsString)) {
                            for (String existingAddress : knownAddresses) {
                                if (!existingAddress.equals(address)) {
                                    Set<Role> updatedRoles = new HashSet<>(securityRepository.getMatch(existingAddress));
                                    updatedRoles.addAll(roles);
                                    securityRepository.addMatch(existingAddress, updatedRoles);
                                }
                            }
                        } else if (address.equals(singleWordString)) {
                            for (String existingAddress : knownAddresses) {
                                if (!existingAddress.equals(address) && !existingAddress.contains(delimeterString)) {
                                    Set<Role> updatedRoles = new HashSet<>(securityRepository.getMatch(existingAddress));
                                    updatedRoles.addAll(roles);
                                    securityRepository.addMatch(existingAddress, updatedRoles);
                                }
                            }
                        } else if (address.endsWith(delimeterString + singleWordString)) {
                            String stem = address.substring(0, address.length() - 2);
                            for (String existingAddress : knownAddresses) {
                                if (!existingAddress.equals(address)
                                        && existingAddress.startsWith(stem)
                                        && !existingAddress.substring(stem.length() + 1, existingAddress.length()).contains(delimeterString)
                                        && !existingAddress.substring(stem.length() + 1, existingAddress.length()).equals(anyWordsString)) {
                                    Set<Role> updatedRoles = new HashSet<>(securityRepository.getMatch(existingAddress));
                                    updatedRoles.addAll(roles);
                                    securityRepository.addMatch(existingAddress, updatedRoles);
                                }
                            }
                        } else if (address.endsWith(delimeterString + anyWordsString)) {
                            String stem = address.substring(0, address.length() - 2);
                            for (String existingAddress : knownAddresses) {
                                if (!existingAddress.equals(address)
                                        && existingAddress.startsWith(stem)) {
                                    Set<Role> updatedRoles = new HashSet<>(securityRepository.getMatch(existingAddress));
                                    updatedRoles.addAll(roles);
                                    securityRepository.addMatch(existingAddress, updatedRoles);
                                }
                            }
                        }
                    }

                } catch (IllegalArgumentException e) {
                    LOG.infov("Unable to parse implied address from group {0}: {1}", group, e.getMessage(), e);
                    e.printStackTrace();
                }
            }

    }
}
