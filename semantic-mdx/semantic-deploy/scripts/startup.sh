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
APP_PROPERTY_FILE=insight.properties
PID=${DIR}/pid


if [[ -z "${INSIGHT_HOME}" ]]; then
    export INSIGHT_HOME=$DIR
    echo "The env variable INSIGHT_HOME has not been set, set INSIGHT_HOME=$DIR"
fi

export MDX_HOME=${INSIGHT_HOME}
echo "MDX_HOME=${MDX_HOME}"

export MDX_CONF=${INSIGHT_HOME}/conf
echo "MDX_CONF=${MDX_CONF}"

# Filter uncommented lines from the properties file.
CONF_CONTENT=$(grep -v '^[ \t]*#' "$MDX_CONF/$APP_PROPERTY_FILE")

# Retrieve the value of a property from the properties file.
function retrieve_property() {
  # Try to retrieve all valid values of the property.
  RETRIEVED_PROPERTIED=$(echo "$CONF_CONTENT" | awk -v PROPERTY_NAME="^[ \t]*$1[ \t]*$" -F= '{if($1~PROPERTY_NAME) {gsub(/^[ \t]+/,"",$2); gsub(/[ \t]+$/,"",$2); print $2}}')
  # Check whether there are multiple valid lines of the property.
  if [[ $(echo "$RETRIEVED_PROPERTIED" | wc -l) -gt 1 ]]
  then
    echo "MDX starting failed: Multiple values of property \"$1\" found, please check file $MDX_CONF/$APP_PROPERTY_FILE."
    exit 1
  fi
}

retrieve_property "insight.mdx.jvm.xms"
jvm_xms="$RETRIEVED_PROPERTIED"
retrieve_property "insight.mdx.jvm.xmx"
jvm_xmx="$RETRIEVED_PROPERTIED"

cd ${DIR}

if [[ $jvm_xms == '' ]]; then
    echo "Set JVM XMS by set-jvm.sh"
    . "$DIR/set-jvm.sh" xms
fi

if [[ $jvm_xmx == '' ]]; then
    echo "Set JVM XMX  by set-jvm.sh"
    . "$DIR/set-jvm.sh" xmx
fi

. "$DIR/set-java.sh"

setJava

retrieve_property "insight.semantic.port"
SEMANTIC_PORT="$RETRIEVED_PROPERTIED"
retrieve_property "insight.database.type"
DATABASE_TYPE="$RETRIEVED_PROPERTIED"

if [[ $DATABASE_TYPE == "postgresql" ]]; then
   echo "Use database: postgresql"
   DATABASE_TYPE="pg"
else
   echo "Use database: mysql"
   DATABASE_TYPE="mysql"
fi

if [[ -z "$SEMANTIC_PORT" ]]; then
  echo -e  "The property 'insight.semantic.port' hasn't been set, use default port: 7080."
  SEMANTIC_PORT=7080
fi

if [[ -n "`netstat -tnlp | grep ":$SEMANTIC_PORT" | grep LISTEN`" ]]; then
   if [[ -e "$PID" ]]; then
      echo "Semantic Service is already started."
      exit 1
   else
      echo "The port $SEMANTIC_PORT has already been used, you can use the property [insight.semantic.port] to change it in ${APP_PROPERTY_FILE}."
      exit 1
   fi
fi

if [[ ! -d "$MDX_HOME/logs" ]]; then
  mkdir $MDX_HOME/logs
fi

echo "JVM memory minimum set as : ${jvm_xms}"
echo "JVM memory maximum set as : ${jvm_xmx}"

JAVA_OPTS="$jvm_xms $jvm_xmx -XX:+UseG1GC -XX:G1HeapRegionSize=4m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8"

LOG_FILE="file:$MDX_CONF/log4j2.xml"
if [[ -e "${MDX_CONF}/log4j2.xml" ]];then
    echo "Use ${MDX_CONF}/log4j2.xml "
    JAVA_OPTS="$JAVA_OPTS -Dlog4j.configurationFile=${LOG_FILE}"
else
    JAVA_OPTS="$JAVA_OPTS -Dlog4j.configurationFile=log4j2.xml"
    echo "${MDX_CONF}/log4j2.xml not found, use default log4j2.xml"
fi

TIME_ZONE=`grep 'insight.mdx.mondrian.jdbc.timezone=' "$MDX_HOME/conf/insight.properties"`
if [ "$TIME_ZONE" != "" ]
then
  time=(${TIME_ZONE//=/ })
  zone=${time[1]}
  echo "Time zone is specified as: $zone"
  JAVA_OPTS="$JAVA_OPTS -Duser.timezone=$zone"
fi

JAVA_OPTS="$JAVA_OPTS -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true -Djava.awt.headless=true"
JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDateStamps -XX:+PrintGCDetails -Xloggc:$INSIGHT_HOME/logs/gc-%t.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$INSIGHT_HOME/logs/heapdump.hprof"

if [ "$DEBUG_MODE" == "true" ]
then
  JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
fi

SPRING_OPTS="$SPRING_OPTS -DINSIGHT_HOME=$INSIGHT_HOME -Dspring.config.name=insight,application -Dspring.profiles.active=$DATABASE_TYPE -Dspring.config.location=classpath:/,file:$INSIGHT_HOME/conf/"

if [[ -f "${MDX_HOME}/logs/semantic.out" ]]; then
  tail -n 10000 "${MDX_HOME}"/logs/semantic.out > "${MDX_HOME}"/logs/semantic.out.tmp
  rm -f "${MDX_HOME}"/logs/semantic.out
  mv "${MDX_HOME}"/logs/semantic.out.tmp "${MDX_HOME}"/logs/semantic.out
fi

echo "Semantic service is starting at port $SEMANTIC_PORT, please check the log at logs/semantic.log."
nohup ${JAVA} ${JAVA_OPTS} ${SPRING_OPTS} -Dloader.path=$DIR/lib/ -jar $DIR/semantic*.jar >> $MDX_HOME/logs/semantic.out 2>&1 &
echo "$!" > ${PID}
