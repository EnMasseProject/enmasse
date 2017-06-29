# parameters
# - base directory
# - lock timeout
function partitionPV() {
  LOCK_DIR="$1"
  LOCK_TIMEOUT="${3:-30}"

  mkdir -p "${LOCK_DIR}"

  LOCK_FD=200
  WAITING_FD=201

  COUNT=1

  while : ; do
    INSTANCE_ID="artemis-${COUNT}"
    INSTANCE_DIR="${LOCK_DIR}/${INSTANCE_ID}"
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

          runServer "${SERVER_DATA_DIR}" "${INSTANCE_ID}" &

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
