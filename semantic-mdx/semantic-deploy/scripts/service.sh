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
DELETER_PID=${DIR}/deleter_pid

cd $DIR/..

INSIGHT_HOME=$(cd .. && pwd)
JAVA_DATA_TOOL_LOADER="-Dloader.path=./lib/ -cp semantic-service.jar -Dloader.main=io.kylin.mdx.insight.data.ToolLauncher org.springframework.boot.loader.PropertiesLauncher"

echo "INSIGHT_HOME= $INSIGHT_HOME"

export MDX_HOME=${INSIGHT_HOME}
echo "MDX_HOME=${MDX_HOME}"

. "set-java.sh"

setJava

function deleter_start() {
    # 参照startup.sh
    JAVA_OPTS="-Xms1024m -Xmx1024m -XX:+UseG1GC -XX:G1HeapRegionSize=4m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8"
    SPRING_OPTS="-DINSIGHT_HOME=$3 -Dspring.config.name=insight,application -Dspring.profiles.active=$4
        -Dspring.config.location=classpath:/,file:$3/conf/"
    if [[ ! -d "$MDX_HOME/logs" ]]; then
      mkdir $MDX_HOME/logs
    fi
    nohup ${JAVA} ${JAVA_OPTS} ${SPRING_OPTS} ${JAVA_DATA_TOOL_LOADER} $* >> $MDX_HOME/logs/deleteQueryLog.log &
    echo "MDX Query Log Deleter Started ~"
    echo "$!" > ${DELETER_PID}
}

function deleter_stop() {
    echo "Stop MDX Deleter ..."
    # 参照shutdown.sh
    if [[ -e "${DELETER_PID}" ]]
    then
        pid=$(cat "${DELETER_PID}")
        if [[ -n $(ps -ef | grep $pid | grep -v grep) ]]; then
            kill -9 "${pid}"
        fi
        rm -f "${DELETER_PID}"
        echo "MDX Deleter is now stopped."
    elif [[ -n `ps -ef | grep $DIR | grep -v grep` ]]
    then
        pid=$(ps -ef | grep "$DIR" | grep semantic | grep -v grep | awk '{print $2}')
        kill -9 "${pid}"
        echo "MDX Deleter is now stopped."
    else
        echo "There is no deleter service running."
    fi
}


case $1 in
deleteQueryLog)
  case $2 in
  start)
    DATABASE_TYPE=$("$INSIGHT_HOME"/bin/get-config-value.sh insight.database.type $INSIGHT_PROPERTIES mysql)
    deleter_start  $* ${INSIGHT_HOME} ${DATABASE_TYPE}
    ;;
  stop)
    deleter_stop
    ;;
  *)
    echo "Error: Usage $2 {start|stop}"
  esac
  ;;
*)
  echo "Error: Usage $1 {deleter|}"
  ;;
esac


