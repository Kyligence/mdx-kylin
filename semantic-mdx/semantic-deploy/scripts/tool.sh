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


set -e
DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

cd $DIR/..

INSIGHT_HOME=$(cd .. && pwd)
JAVA_DATA_TOOL_LOADER="-Dloader.path=./lib/ -cp semantic-service.jar -Dloader.main=io.kylin.mdx.insight.data.ToolLauncher org.springframework.boot.loader.PropertiesLauncher"

echo "INSIGHT_HOME= $INSIGHT_HOME"

export MDX_HOME=${INSIGHT_HOME}
echo "MDX_HOME=${MDX_HOME}"

. "set-java.sh"

setJava

function data_upgrade {
    JAVA_OPTS="-Xms1024m -Xmx1024m -XX:+UseG1GC -XX:G1HeapRegionSize=4m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8"
    SPRING_OPTS="-DINSIGHT_HOME=$2 -Dspring.config.name=insight,application -Dspring.profiles.active=$5
        -Dspring.config.location=classpath:/,file:$2/conf/"


    ${JAVA} ${JAVA_OPTS} ${SPRING_OPTS} ${JAVA_DATA_TOOL_LOADER} $*
}

function encrypt {
    ${JAVA} ${JAVA_DATA_TOOL_LOADER} $*
}

function decrypt {
    ${JAVA} ${JAVA_DATA_TOOL_LOADER} $*
}

function log {
    JAVA_OPTS="-Xms1024m -Xmx1024m -XX:+UseG1GC -XX:G1HeapRegionSize=4m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8"
    SPRING_OPTS="-DINSIGHT_HOME=$2 -Dspring.config.name=insight,application -Dspring.profiles.active=$5
        -Dspring.config.location=classpath:/,file:$2/conf/"

    ${JAVA} ${JAVA_OPTS} ${SPRING_OPTS} ${JAVA_DATA_TOOL_LOADER} $*
}

case "$1" in
    upgrade)
        data_upgrade $*
        ;;
    encrypt)
        encrypt $*
        ;;
    decrypt)
        decrypt $*
        ;;
    log)
        log $*
        ;;
    *)
      echo "Error: Usage $0 {upgrade|encrypt}"
    ;;
esac
