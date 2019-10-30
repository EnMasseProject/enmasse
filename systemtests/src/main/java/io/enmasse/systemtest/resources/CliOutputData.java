/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
        String[] lines = splitLines(cmdOutput);
        switch (type) {
            case ADDRESS_SPACE:
                for (String line : lines) {
                    cliData.add(new AddressSpaceRow(line));
                }
                break;
            case USER:
                for (String line : lines) {
                    cliData.add(new UserRow(line));
                }
                break;
            case ADDRESS:
                for (String line : lines) {
                    cliData.add(new AddressRow(line));
                }
                break;
            default:
                break;
        }
    }

    private String[] splitLines(String data) {
        String[] lines = data.split(System.getProperty("line.separator"));
        return Arrays.copyOfRange(lines, 1, lines.length);
    }


    //=====================================================
    // Data helpers
    //=====================================================

    public enum CliOutputDataType {
        ADDRESS_SPACE,
        USER,
        ADDRESS
    }

    public static class CliDataRow {
        private String name;
        private String type;
        private String age;

        String[] splitData(String data) {
            return data.trim().split("\\s{2,}");
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

        public void setName(String name) {
            this.name = name;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setAge(String age) {
            this.age = age;
        }
    }

    public static class AddressSpaceRow extends CliDataRow {
        private String plan;
        private boolean ready;
        private String phase;
        private String status;

        AddressSpaceRow(String data) {
            String[] parsedData = splitData(data);
            setName(parsedData[0]);
            setType(parsedData[1]);
            this.plan = parsedData[2];
            this.ready = Boolean.parseBoolean(parsedData[3]);
            this.phase = parsedData[4];
            setAge(parsedData[5]);
            try {
                this.status = parsedData[6];
            } catch (Exception ignored) {
                this.status = "";
            }
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
    }

    public static class AddressRow extends CliDataRow {
        private String address;
        private String addressSpace;
        private String plan;
        private boolean ready;
        private String phase;
        private String status;

        AddressRow(String data) {
            String[] parsedData = splitData(data);
            setName(parsedData[0]);
            this.address = parsedData[1];
            this.addressSpace = parsedData[2];
            setType(parsedData[3]);
            this.plan = parsedData[4];
            this.ready = Boolean.parseBoolean(parsedData[5]);
            this.phase = parsedData[6];
            setAge(parsedData[7]);
            try {
                this.status = parsedData[8];
            } catch (Exception ignored) {
                this.status = "";
            }
        }

        public String getAddress() {
            return address;
        }

        public String getAddressSpace() {
            return addressSpace;
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
    }

    public static class UserRow extends CliDataRow {
        private String username;

        UserRow(String data) {
            String[] parsedData = splitData(data);
            setName(parsedData[0]);
            this.username = parsedData[1];
            setType(parsedData[2]);
            setAge(parsedData[3]);
        }

        public String getUsername() {
            return username;
        }
    }
}
