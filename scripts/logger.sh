#!/usr/bin/env bash

function log() {
    local level="${1}"
    shift
    echo "[$(date "+%F %T")] [${level}]: ${@}"
}

function err_and_exit() {
    log "ERROR" "${1}" >&2
    exit ${2:-1}
}

function err() {
    log "ERROR" "${1}" >&2
}

function info() {
    log "INFO" "${1}"
}

function warn() {
    log "WARN" "${1}"
}

