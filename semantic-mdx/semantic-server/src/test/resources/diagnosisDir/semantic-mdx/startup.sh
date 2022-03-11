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

jvm_xms_name=$(cat $MDX_HOME/conf/${APP_PROPERTY_FILE} | grep insight.mdx.jvm.xms | awk -F= '{print $1}')
jvm_xmx_name=$(cat $MDX_HOME/conf/${APP_PROPERTY_FILE} | grep insight.mdx.jvm.xmx | awk -F= '{print $1}')

if [[ $jvm_xms_name == '' || $jvm_xms_name =~ ^#.* ]];
then
    jvm_xms=''
else
    jvm_xms=$(cat $MDX_HOME/conf/${APP_PROPERTY_FILE} | grep insight.mdx.jvm.xms | awk -F= '{print $2}')
fi

if [[ $jvm_xmx_name == '' && $jvm_xmx_name =~ ^#.* ]];
then
    jvm_xmx=''
else
    jvm_xmx=$(cat $MDX_HOME/conf/${APP_PROPERTY_FILE} | grep insight.mdx.jvm.xmx | awk -F= '{print $2}')
fi

cd ${DIR}

if [[ $jvm_xms == '' || $jvm_xmx == '' ]]; then
    echo "set JVM by set-jvm.sh"
    . "$DIR/set-jvm.sh"
fi

. "$DIR/set-java.sh"

setJava

function trimStr {
    trimmed=$1
    trimmed=${trimmed%% }
    trimmed=${trimmed## }
    echo $trimmed
}

SEMANTIC_PORT=$(cat $MDX_HOME/conf/${APP_PROPERTY_FILE} | grep insight.semantic.port | awk -F= '{print $2}')
DATABASE_TYPE=$(cat $MDX_HOME/conf/${APP_PROPERTY_FILE} | grep insight.database.type | awk -F= '{print $2}')


SEMANTIC_PORT=$(trimStr $SEMANTIC_PORT)
DATABASE_TYPE=$(trimStr $DATABASE_TYPE)

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

timeZone=`grep 'insight.mdx.mondrian.jdbc.timezone=' "$MDX_HOME/conf/insight.properties"`
if [ "$timeZone" == "" ]
then JAVA_OPTS="$jvm_xms $jvm_xmx -XX:+UseG1GC -XX:G1HeapRegionSize=4m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8 -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true -Djava.awt.headless=true -XX:+PrintGCDateStamps -XX:+PrintGCDetails -Xloggc:$INSIGHT_HOME/logs/gc.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$INSIGHT_HOME/logs/heapdump.hprof"
else
  time=(${timeZone//=/ })
  zone=${time[1]}
  echo "Time zone is specified as: $zone"
  JAVA_OPTS="$jvm_xms $jvm_xmx -XX:+UseG1GC -XX:G1HeapRegionSize=4m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8 -Duser.timezone=$zone -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true -Djava.awt.headless=true -XX:+PrintGCDateStamps -XX:+PrintGCDetails -Xloggc:$INSIGHT_HOME/logs/gc.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$INSIGHT_HOME/logs/heapdump.hprof"
fi


SPRING_OPTS="$SPRING_OPTS -DINSIGHT_HOME=$INSIGHT_HOME -Dspring.config.name=insight,application -Dspring.profiles.active=$DATABASE_TYPE -Dspring.config.location=classpath:/,file:$INSIGHT_HOME/conf/"

echo "Semantic service is starting at port $SEMANTIC_PORT, please check the log at logs/semantic.log."
nohup ${JAVA} ${JAVA_OPTS} ${SPRING_OPTS} -Dloader.path=$DIR/lib/ -jar $DIR/semantic*.jar > $MDX_HOME/logs/semantic.out 2>&1 &
echo "$!" > ${PID}

