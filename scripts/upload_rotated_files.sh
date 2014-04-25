#!/bin/bash

function log {
    MSG=$1; shift
    echo "$MSG"
}

function exit_with_error {
    log "Exiting now"
    exit 1
}

GLOBAL_MATCHES=""
function identify_logrotated_files {
    local TARGET_DIR=$1; shift
    local TARGET_FILE=$1; shift
    local TARGET_ABS_PATH="$TARGET_DIR/$TARGET_FILE"
    if [ ! -f "$TARGET_ABS_PATH" ]; then
        log "TARGET_ABS_PATH ($TARGET_ABS_PATH) doesn't exist; nothing to rotate"
        exit_with_error
    fi
    #DO NOT REMOVE the . or else the current active logfile will be matched, too!
    local TARGET_GLOB="$TARGET_ABS_PATH.*"
    local MATCHES_CMD="ls $TARGET_GLOB"
    local MATCHES=`ls $TARGET_GLOB`
    #ls /a
    if [ $? -ne 0 ]; then
        log "Problem running MATCHES_CMD=$MATCHES_CMD"
        exit_with_error
    fi
    GLOBAL_MATCHES="$MATCHES"
}

function compress {
    TARGET_FILE=$1; shift
    gzip "$TARGET_FILE"

}

function upload_to_s3 {
    COMPRESSED_FILE=$1; shift

}

function delete_compressed_file  {
    TARGET_FILE=$1; shift
    # TODO only delete it if it ends in gz AND it has been uploaded to Amazon S3
    #Only uncomment me when the safety is in place
    #rm TARGET_FILE
}


TARGET_DIR='./logs'
#TARGET_DIR='/a'
log "TARGET_DIR is $TARGET_DIR"
TARGET_FILE='tracking_api_data.log'
log "TARGET_FILE is $TARGET_FILE"

identify_logrotated_files "$TARGET_DIR" "$TARGET_FILE"

log "GLOBAL_MATCHES is $GLOBAL_MATCHES"
for MATCH in $GLOBAL_MATCHES
do
    log "==============="
    log "Begin processing MATCH=$MATCH"
    log "Compressing $MATCH"
    #compress "$MATCH"
    log "Uploading $MATCH to s3"
    #upload_to_s3 "$MATCH"

    #log "Deleting $MATCH
done
log "==============="
