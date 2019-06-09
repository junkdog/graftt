#!/usr/bin/env bash


path_to_scrub=""

# since dirname is missing from macos
function resolve_path {
    local path="$1"
    if [[ -L "$path" ]]; then
        path=$(readlink "$path")
    fi
    path_to_scrub="${path%/*}"
}



resolve_path "$0"

jar_path=${path_to_scrub}/target/cli-*.jar
# java ${CMPAPI_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,address=5005,suspend=y  -jar ${jar_path} "$@"
java ${CMPAPI_OPTS} -jar ${jar_path} "$@"
