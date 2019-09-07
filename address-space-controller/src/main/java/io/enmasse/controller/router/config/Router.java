/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Router {
    private Mode mode = Mode.interior;
    private String id = "${HOSTNAME}";
    private int workerThreads = 4;
    private Distribution defaultDistribution = Distribution.unavailable;
    private boolean allowResumableLinkRoute = false;
    private boolean timestampsInUTC = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Distribution getDefaultDistribution() {
        return defaultDistribution;
    }

    public void setDefaultDistribution(Distribution defaultDistribution) {
        this.defaultDistribution = defaultDistribution;
    }

    public boolean isAllowResumableLinkRoute() {
        return allowResumableLinkRoute;
    }

    public void setAllowResumableLinkRoute(boolean allowResumableLinkRoute) {
        this.allowResumableLinkRoute = allowResumableLinkRoute;
    }

    public boolean isTimestampsInUTC() {
        return timestampsInUTC;
    }

    public void setTimestampsInUTC(boolean timestampsInUTC) {
        this.timestampsInUTC = timestampsInUTC;
    }

    public enum Mode {
        interior
    }

    @Override
    public String toString() {
        return "Router{" +
                "mode=" + mode +
                ", id='" + id + '\'' +
                ", workerThreads=" + workerThreads +
                ", defaultDistribution=" + defaultDistribution +
                ", allowResumableLinkRoute=" + allowResumableLinkRoute +
                ", timestampsInUTC=" + timestampsInUTC +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Router router = (Router) o;
        return workerThreads == router.workerThreads &&
                allowResumableLinkRoute == router.allowResumableLinkRoute &&
                timestampsInUTC == router.timestampsInUTC &&
                mode == router.mode &&
                Objects.equals(id, router.id) &&
                defaultDistribution == router.defaultDistribution;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, id, workerThreads, defaultDistribution, allowResumableLinkRoute, timestampsInUTC);
    }

    /*
          router {
        mode: interior
        id: ${HOSTNAME}
        workerThreads: ${ROUTER_WORKER_THREADS}
        defaultDistribution: unavailable
        allowResumableLinkRoute: false
        timestampsInUTC: true
      }
      */
}
