/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

public class CliOutputData {

    private List<CliDataRow> cliData;

    public CliOutputData(String cmdOutput, CliOutputDataType type) {
        parseCliData(cmdOutput, type);
    }

    public List<CliDataRow> getData() {
        return cliData;
    }

    public CliDataRow getData(String name) {
        return cliData.stream().filter(row -> row.getName().equals(name)).collect(Collectors.toList()).get(0);
    }

    private void parseCliData(String cmdOutput, CliOutputDataType type) {
        cliData = new LinkedList<>();

        List<String[]> lines = splitLines(cmdOutput);

        switch (type) {
            case ADDRESS_SPACE:
                for (String[] line : lines) {
                    cliData.add(new AddressSpaceRow(line));
                }
                break;
            case USER:
                for (String[] line : lines) {
                    cliData.add(new UserRow(line));
                }
                break;
            case ADDRESS:
                for (String[] line : lines) {
                    cliData.add(new AddressRow(line));
                }
                break;
            default:
                break;
        }
    }

    private static List<String[]> splitLines(final String data) {
        final LinkedList<String> lines = data.lines().collect(Collectors.toCollection(LinkedList::new));
        final String header = lines.pop();

        final List<Integer> headerPositions = new ArrayList<>();
        boolean inName = false;
        for (int i = 0; i < header.length(); i++ ) {
            final char c = header.charAt(i);
            switch ( c ) {
                case ' ':
                    inName = false;
                    break;
                default:
                    if ( !inName) {
                        inName = true;
                        headerPositions.add(i);
                    }
                    break;
            }
        }
        // add a terminator
        headerPositions.add(Integer.MAX_VALUE);

        return lines.stream().map(line -> splitLine(headerPositions, line) ).collect(Collectors.toList());
    }

    private static String[] splitLine(final List<Integer> positions, final String line) {
        // size minus the terminator
        final int len = positions.size() - 1;
        final String [] result = new String[len];

        for ( int i = 0; i < len; i++ )  {
            final int start = positions.get(i);
            final int end = Math.min(positions.get(i+1), line.length());
            result[i] = line.substring(start, end).trim();
        }

        return result;
    }

    //=====================================================
    // Data helpers
    //=====================================================

    public enum CliOutputDataType {
        ADDRESS_SPACE,
        USER,
        ADDRESS
    }

    public static abstract class CliDataRow {
        protected String name;
        protected String type;
        protected String age;

        protected void expectColumns(final int expectedColumns, final String[] parsedData) {
            if (parsedData.length != expectedColumns) {
                throw new IllegalArgumentException(String.format(
                        "Unable to parse row for type %s. Requires %s tokens (was: %s: %s)",
                        getClass().getSimpleName(), expectedColumns, parsedData.length, Arrays.toString(parsedData)));
            }
        }

        String[] splitData(String data) {
            return data.trim().split("\\s{3,}");
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getAge() {
            return age;
        }

        @Override
        public int hashCode() {
            return Objects.hash(age, name, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CliDataRow)) {
                return false;
            }
            CliDataRow other = (CliDataRow) obj;
            return Objects.equals(age, other.age) && Objects.equals(name, other.name) && Objects.equals(type, other.type);
        }

        protected ToStringHelper toStringHelper() {
            return MoreObjects.toStringHelper(this)
                    .add("age", this.age)
                    .add("name", this.name)
                    .add("type", this.type);
        }

        @Override
        public String toString() {
            return toStringHelper().toString();
        }

    }

    public static class AddressSpaceRow extends CliDataRow {
        private String plan;
        private boolean ready;
        private String phase;
        private String status;

        AddressSpaceRow(String[] parsedData) {
            expectColumns(7, parsedData);
            this.name = parsedData[0];
            this.type = parsedData[1];
            this.plan = parsedData[2];
            this.ready = Boolean.parseBoolean(parsedData[3]);
            this.phase = parsedData[4];
            this.status = parsedData[5];
            this.age = parsedData[6];
        }

        public String getPlan() {
            return plan;
        }

        public String getPhase() {
            return phase;
        }

        public boolean isReady() {
            return ready;
        }

        public String getStatus() {
            return status;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Objects.hash(phase, plan, ready, status);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (!(obj instanceof AddressSpaceRow)) {
                return false;
            }
            AddressSpaceRow other = (AddressSpaceRow) obj;
            return Objects.equals(phase, other.phase)
                    && Objects.equals(plan, other.plan)
                    && ready == other.ready
                    && Objects.equals(status, other.status);
        }

        @Override
        protected ToStringHelper toStringHelper() {
            return super.toStringHelper()
                    .add("phase", this.phase)
                    .add("plan", this.plan)
                    .add("ready", this.ready)
                    .add("status", this.status);
        }
    }

    public static class AddressRow extends CliDataRow {
        private String address;
        private String plan;
        private boolean ready;
        private String phase;
        private String status;

        AddressRow(String[] parsedData) {
            expectColumns(8, parsedData);
            this.name = parsedData[0];
            this.address = parsedData[1];
            this.type = parsedData[2];
            this.plan = parsedData[3];
            this.ready = Boolean.parseBoolean(parsedData[4]);
            this.phase = parsedData[5];
            this.status = parsedData[6];
            this.age = parsedData[7];
        }

        public String getAddress() {
            return address;
        }

        public String getPlan() {
            return plan;
        }

        public boolean isReady() {
            return ready;
        }

        public String getPhase() {
            return phase;
        }

        public String getStatus() {
            return status;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Objects.hash(address, phase, plan, ready, status);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (!(obj instanceof AddressRow)) {
                return false;
            }
            AddressRow other = (AddressRow) obj;
            return Objects.equals(address, other.address)
                    && Objects.equals(phase, other.phase)
                    && Objects.equals(plan, other.plan)
                    && ready == other.ready
                    && Objects.equals(status, other.status);
        }

        @Override
        protected ToStringHelper toStringHelper() {
            return super.toStringHelper()
                    .add("address", this.address)
                    .add("phase", this.phase)
                    .add("plan", this.plan)
                    .add("ready", this.ready)
                    .add("status", this.status);
        }

    }

    public static class UserRow extends CliDataRow {
        private String username;
        private String phase;

        UserRow(String[] parsedData) {
            expectColumns(6, parsedData);
            this.name = parsedData[0];
            this.username = parsedData[1];
            this.type = parsedData[2];
            this.phase = parsedData[3];
            this.age = parsedData[4];
        }

        public String getUsername() {
            return username;
        }

        public String getPhase() {
            return phase;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Objects.hash(phase, username);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (!(obj instanceof UserRow)) {
                return false;
            }
            UserRow other = (UserRow) obj;
            return Objects.equals(phase, other.phase)
                    && Objects.equals(username, other.username);
        }

        @Override
        protected ToStringHelper toStringHelper() {
            return super.toStringHelper()
                    .add("phase", this.phase)
                    .add("username", this.username);
        }

    }
}
