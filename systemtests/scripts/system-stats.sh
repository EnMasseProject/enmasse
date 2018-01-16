#!/usr/bin/env bash
printf "Time\t\t\tMemory\t\tDisk\t\tCPU\n"
export TZ=UTC

while [ true ]; do
    MEMORY=$(free -m | awk 'NR==2{printf "%.2f%%\t\t", $3*100/$2 }')
    DISK=$(df -h | awk '$NF=="/"{printf "%s\t\t", $5}')
    CPU=$(top -bn1 | grep load | awk '{printf "%.2f%%\t\t\n", $(NF-2)}')
    TIME=$(date +"%m-%d-%Y-%T")
    echo "$TIME    $MEMORY$DISK$CPU"
    sleep 5
done
