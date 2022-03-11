#!/bin/bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
PID_FILE=$DIR/pid

function check_alive() {
    for ((i=1;i<=15;i+=1))
    do
        printf "."
        sleep 1
        killed_pid=$(ps -ef | grep $1 | grep -v grep | awk '{print $2}')
        if [[ -z ${killed_pid} ]]; then
            echo ""
            return 1
        fi
    done
    echo ""
    return 0
}

function check_after() {
    # check kill status
    check_alive "$1"
    status=$?
    if [[ $status -eq 1 ]]; then
        echo "Semantic service has been stopped, pid=$1."
        return
    else
        echo "Semantic service termination failed! The process [pid=$1] is about to be forcibly terminated."
    fi
    # force kill and check
    kill -9 "$1"
    check_alive "$1"
    status=$?
    if [[ $status -eq 1 ]]; then
        echo "Semantic service has been forcibly stopped, pid=$1."
    else
        echo -e "Semantic service didn't stopped completely. Please check the cause of the failure or contact IT support."
    fi
}

if [[ -e "$PID_FILE" ]]
then
    pid=$(cat "$PID_FILE")
    if [[ -n $(ps -ef | grep $pid | grep -v grep) ]]; then
        kill "${pid}"
    fi
    rm -f "$PID_FILE"
    check_after "$pid"
elif [[ -n `ps -ef | grep $DIR | grep -v grep` ]]
then
    pid=$(ps -ef | grep "$DIR" | grep semantic | grep -v grep | awk '{print $2}')
    kill "${pid}"
    check_after "$pid"
else
    echo "There is no semantic service running."
fi
