/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

class UsageInfo {
    private double used;
    private int needed;

    public UsageInfo() {
        this.used = 0;
        this.needed = 0;
    }

    public UsageInfo(UsageInfo copy) {
        this.used = copy.used;
        this.needed = copy.needed;
    }

    public void addUsed(double added) {
        this.used += added;
        needed = (int) Math.ceil(used);
    }

    @Override
    public String toString() {
        return "{used=" + used + ", needed=" + needed + "}";
    }

    public double getUsed() {
        return used;
    }

    public int getNeeded() {
        return needed;
    }
}
