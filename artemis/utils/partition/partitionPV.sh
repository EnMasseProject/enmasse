# parameters
# - base directory
# - lock timeout
function partitionPV() {
  LOCK_DIR="$1"
  LOCK_TIMEOUT="${2:-30}"

  mkdir -p "${LOCK_DIR}"

  LOCK_FD=200
  WAITING_FD=201

  COUNT=1

  while : ; do
    INSTANCE_DIR="${LOCK_DIR}/split-$COUNT"
    mkdir -p "${INSTANCE_DIR}"

    echo "Attempting to obtain lock for directory: ($INSTANCE_DIR)"

    TERMINATING_FILE="${INSTANCE_DIR}/terminating"
    (
      flock -n $WAITING_FD
      if [ $? -eq 0 ]; then
        # Nobody waiting, try to grab the lock

        flock -n $LOCK_FD
        LOCK_STATUS=$?
        if [ $LOCK_STATUS -ne 0 ] ; then
          # Second attempt with a potential wait period
          TERMINATING=$(cat "${TERMINATING_FILE}" 2>/dev/null)
          if [ -z "$TERMINATING" ] ; then
            # Not terminating, grab the lock without waiting
            flock -n $LOCK_FD
          else
            # Terminating, grab the lock with timeout
            echo "Existing server instance is terminating, waiting to acquire the lock"
            flock -w $LOCK_TIMEOUT $LOCK_FD
          fi
          LOCK_STATUS=$?
        fi
        if [ $LOCK_STATUS -eq 0 ] ; then
          echo "Successfully locked directory: ($INSTANCE_DIR)"

          > "$TERMINATING_FILE"
          flock -u $WAITING_FD

          SERVER_DATA_DIR="${INSTANCE_DIR}/serverData"
          mkdir -p "${SERVER_DATA_DIR}"

          if [ ! -f "${SERVER_DATA_DIR}/../data_initialized" ]; then
            init_data_dir ${SERVER_DATA_DIR}
            touch "${SERVER_DATA_DIR}/../data_initialized"
          fi

          runServer "${SERVER_DATA_DIR}" "${COUNT}" &

          PID=$!

          trap "echo Received TERM ; echo \"$HOSTNAME\" > \"$TERMINATING_FILE\" ; kill -TERM $PID" TERM

          wait $PID 2>/dev/null
          STATUS=$?
          trap - TERM
          wait $PID 2>/dev/null

          echo "Server terminated with status $STATUS ($(kill -l $STATUS))"

          if [ "$STATUS" -eq 255 ] ; then
            echo "Server returned 255, changing to 254"
            STATUS=254
          fi

          echo "Releasing lock: ($INSTANCE_DIR)"
          exit $STATUS
        fi
      else
        echo "Failed to obtain lock for directory: ($INSTANCE_DIR)"
      fi

      exit 255
    ) 200> "${INSTANCE_DIR}/lock" 201> "${INSTANCE_DIR}/waiting" &

    PID=$!

    trap "kill -TERM $PID" TERM

    wait $PID 2>/dev/null
    STATUS=$?
    trap - TERM
    wait $PID 2>/dev/null

    if [ $STATUS -ne 255 ] ; then
      break;
    fi
    COUNT=$(expr $COUNT + 1)
  done
}


# parameters
# - base directory
# - migration timeout
# - migration pause between cycles
function migratePV() {
  LOCK_DIR="$1"
  MIGRATION_TIMEOUT="${2:-30}"
  MIGRATION_PAUSE="${3:-30}"
  MIGRATED=false

  mkdir -p "${LOCK_DIR}"

  LOCK_FD=200
  MIGRATING_FD=201

  COUNT=1

  while : ; do
    INSTANCE_DIR="${LOCK_DIR}/split-$COUNT"
    if [ -d "$INSTANCE_DIR" ] ; then
      mkdir -p "${INSTANCE_DIR}"

      TERMINATING_FILE="${INSTANCE_DIR}/terminating"
      TERMINATING=$(cat "${TERMINATING_FILE}" 2>/dev/null)
      if [ -n "$TERMINATING" ] ; then
        echo "Attempting to migrate directory: ($INSTANCE_DIR)"

        (
          flock -n $MIGRATING_FD
          if [ $? -eq 0 ]; then
            TERMINATING_TIME=$(stat -c "%Y" "${TERMINATING_FILE}")
            CURRENT_TIME=$(date +"%s")
            TIMEOUT=$(expr $MIGRATION_TIMEOUT + $TERMINATING_TIME - $CURRENT_TIME)
            echo "Waiting for grace period to expire, remaining timeout is ${TIMEOUT} seconds"
            while : ; do
              TERMINATING=$(cat "${TERMINATING_FILE}" 2>/dev/null)
              if [ -z "$TERMINATING" ] ; then
                echo "Migration cancelled, no longer terminating in directory: ($INSTANCE_DIR)"
                break
              else
                TIMEOUT=$(expr $TIMEOUT - 1)
                if [ "$TIMEOUT" -gt 0 ] ; then
                  sleep 1
                else
                  break
                fi
              fi
            done

            if [ "$TIMEOUT" -le 0 ] ; then
              echo "Attempting to obtain lock for directory: ($INSTANCE_DIR)"

              flock -n $LOCK_FD
              LOCK_STATUS=$?

              if [ $LOCK_STATUS -eq 0 ] ; then
                echo "Successfully locked directory: ($INSTANCE_DIR)"
                MIGRATED=true

                flock -u $MIGRATING_FD

                SERVER_DATA_DIR="${INSTANCE_DIR}/serverData"
                MIGRATION_DIR="${SERVER_DATA_DIR}/migration"
                mkdir -p "${MIGRATION_DIR}"
                cd "${MIGRATION_DIR}"

                runMigration "${SERVER_DATA_DIR}" "${COUNT}" &

                PID=$!

                trap "echo Received TERM ; echo \"$HOSTNAME\" > \"$TERMINATING_FILE\" ; kill -TERM $PID" TERM

                wait $PID 2>/dev/null
                STATUS=$?
                trap - TERM
                wait $PID 2>/dev/null

                echo "Migration terminated with status $STATUS ($(kill -l $STATUS))"

                if [ "$STATUS" -eq 0 ] ; then
                  > "$TERMINATING_FILE"
                elif [ "$STATUS" -eq 255 ] ; then
                  echo "Server returned 255, changing to 254"
                  STATUS=254
                fi

                echo "Releasing lock: ($INSTANCE_DIR)"
                exit $STATUS
              fi
            fi
          fi

          exit 255
        ) 200> "${INSTANCE_DIR}/lock" 201> "${INSTANCE_DIR}/migrating" &

        PID=$!

        trap "kill -TERM $PID" TERM

        wait $PID 2>/dev/null
        trap - TERM
        wait $PID 2>/dev/null
      fi
      COUNT=$(expr $COUNT + 1)
    else
      if [ "$MIGRATED" = "false" ] ; then
        echo "Finished Migration Check cycle, pausing for ${MIGRATION_PAUSE} seconds before resuming"
        COUNT=1
        sleep "${MIGRATION_PAUSE}"
      else
        MIGRATED=false
      fi
    fi
  done
}
