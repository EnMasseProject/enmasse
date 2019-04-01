/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import java.util.Objects;

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

    public void subUsed(double sub) {
        this.used -= sub;
        needed = (int) Math.ceil(used);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsageInfo usageInfo = (UsageInfo) o;
        return Double.compare(usageInfo.used, used) == 0 &&
                needed == usageInfo.needed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(used, needed);
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
