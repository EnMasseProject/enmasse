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
