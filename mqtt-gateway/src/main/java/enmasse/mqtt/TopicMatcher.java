/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Provides matching feature between "wildcarded" topics (for subscription)
 * with a fixed topic (for publishing)
 */
public class TopicMatcher {

    private static final String PLUS_WILDCARD = "\\+";
    private static final String SHARP_WILDCARD = "#";

    private static final String PLUS_WILDCARD_REPLACE = "[^/]\\+";
    private static final String SHARP_WILDCARD_REPLACE = ".*";

    /**
     * Verify if the topic matches the "wildcarded" topic provided
     *
     * @param wildcardedTopic   topic with wildcards (for subscription)
     * @param topic fixed topic (for publishing)
     * @return  if there is a match
     */
    public static boolean isMatch(String wildcardedTopic, String topic) {

        String topicReplaced =
                wildcardedTopic.replaceAll(PLUS_WILDCARD, PLUS_WILDCARD_REPLACE).replaceAll(SHARP_WILDCARD, SHARP_WILDCARD_REPLACE);

        Pattern pattern = Pattern.compile(topicReplaced);

        return pattern.matcher(topic).matches();
    }

    /**
     * Verify if the topic matches one of the "wildcarded" topics provided
     *
     * @param wildcardedTopics  topics with wildcards (for subscription)
     * @param topic fixed topic (for publishing)
     * @return  the "wildcarded" topic which matches
     */
    public static String match(List<String> wildcardedTopics, String topic) {

        for (String wildcardedTopic: wildcardedTopics) {

            if (isMatch(wildcardedTopic, topic)) {
                return wildcardedTopic;
            }
        }

        return null;
    }
}
