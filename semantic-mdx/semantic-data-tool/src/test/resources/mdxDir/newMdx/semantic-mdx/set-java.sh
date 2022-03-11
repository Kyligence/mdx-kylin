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


# -----------------------------------------------------------------------------
# Finds a suitable Java
#

# misc functions
function quit {
    echo "$@"
    if [[ -n "${QUIT_MESSASGE_LOG}" ]]; then
        echo `setColor 31 "$@"` >> ${QUIT_MESSASGE_LOG}
    fi
    exit 1
}

function verbose {
    if [[ -n "$verbose" ]]; then
        echo "$@"
    fi
}

setJava() {
    if [[ "${JAVA}" == "" ]]; then
        if [[ -z "$JAVA_HOME" ]]; then
            JAVA_VERSION=`java -version 2>&1 | awk -F\" '/version/ {print $2}'`
            if [[ $JAVA_VERSION ]] && [[ "$JAVA_VERSION" > "1.8" ]]; then
                JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
                echo "set JAVA_HOME to $JAVA_HOME"
            else
                quit "Java 1.8 or above is required."
            fi
            [[ -z "$JAVA_HOME" ]] && quit "Please set JAVA_HOME"
            export JAVA_HOME
        else
            echo "JAVA_HOME has been set,it's $JAVA_HOME"
        fi
        export JAVA=$JAVA_HOME/bin/java
        [[ -e "${JAVA}" ]] || quit "${JAVA} does not exist. Please set JAVA_HOME correctly."
        echo "JAVA=${JAVA}"
    fi
}
