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
dir=$(cd -P -- "$(dirname -- "$0")/.." && pwd -P)

export LC_ALL=en_US.utf-8
export LANG=en_US.utf-8
export INSIGHT_HOME=$dir
export INSIGHT_PROPERTIES=$dir/conf/insight.properties
cd "$INSIGHT_HOME"
echo "Working directory: $(pwd)"

SEMANTIC_PORT=$("$INSIGHT_HOME"/bin/get-config-value.sh insight.semantic.port $INSIGHT_PROPERTIES 7080)
CONTEXT_PATH=$("$INSIGHT_HOME"/bin/get-config-value.sh insight.semantic.context-path $INSIGHT_PROPERTIES "/")
HOST=0.0.0.0
ENABLE_SSL=$("$INSIGHT_HOME"/bin/get-config-value.sh insight.semantic.ssl.enabled $INSIGHT_PROPERTIES false)
if [[ $ENABLE_SSL == true ]]; then
   PROTOCOL_TYPE=https
else
   PROTOCOL_TYPE=http
fi

if [[ "${CONTEXT_PATH}" != */ ]]; then
  CONTEXT_PATH=${CONTEXT_PATH}/
fi
echo "MDX context path: ${CONTEXT_PATH}"

function pure_start () {
    echo "Start MDX for Kylin..."
    export SPRING_OPTS="-Dinsight.semantic.startup.sync.enable=false"
    semantic-mdx/startup.sh
    retry=0
    while ! curl -k -s -f -o /dev/null ${PROTOCOL_TYPE}://127.0.0.1:"${SEMANTIC_PORT}""${CONTEXT_PATH}"api/health
    do
       printf "."; sleep 1; let retry=retry+1
       if [ $retry -gt 60 ]; then
           echo ""
           echo "Semantic service failed to start, please check logs/semantic.log and logs/semantic.out for the details."
           exit 1
       fi
    done
    echo ""
    echo "MDX for Kylin is started. Now you can visit ${PROTOCOL_TYPE}://${HOST}:${SEMANTIC_PORT}${CONTEXT_PATH} to explore."
}

function sync () {
    echo "Starting syncing metadata..."
    result=$(curl -s -X GET --header 'Accept: application/json' ${PROTOCOL_TYPE}://127.0.0.1:"${SEMANTIC_PORT}""${CONTEXT_PATH}"api/system/sync)
    if [[ "${result}" =~ "success" ]]; then
         echo "Successfully starting syncing metadata..."
    else
         echo "Can't synchronize metadata. The connection user information or password maybe empty or has been changed, please contact system admin to update in Configuration page under Management. Please see logs/semantic.log for detail."
    fi
}

case "$2" in
    "-X")
        echo "MDX run with debug mode on port 5005"
        export DEBUG_MODE="true"
        ;;
    *)
esac

case "$1" in
    start)
        pure_start
        sync
        ;;
    pure)
        pure_start
        ;;
    sync)
        sync
        ;;
    stop)
        echo "Stop MDX for Kylin..."
        semantic-mdx/shutdown.sh
        echo "MDX for Kylin is now stopped. You can run 'mdx.sh ps' to confirm no process left."
        ;;
    restart)
        echo "Stop MDX for Kylin..."
        semantic-mdx/shutdown.sh
        echo "MDX for Kylin is now stopped."
        pure_start
        sync
        ;;
    ps)
        pgrep -f "$INSIGHT_HOME" -l
        ;;
    kill)
        pkill -f "$INSIGHT_HOME"
        ;;
    encrypt)
        semantic-mdx/scripts/tool.sh "$@"
        ;;
    upgrade)
      if [[ $2 =~ ^/.* ]]; then
         OLE_DIRECTORY="$2"
       else
         OLE_DIRECTORY="$dir/bin/$2"
      fi
      export OLD_INSIGHT_PROPERTIES=$OLE_DIRECTORY/conf/insight.properties
      OLD_SEMANTIC_PORT=$("$INSIGHT_HOME"/bin/get-config-value.sh insight.semantic.port $OLD_INSIGHT_PROPERTIES 7080)
      DATABASE_TYPE=$("$INSIGHT_HOME"/bin/get-config-value.sh insight.database.type $OLD_INSIGHT_PROPERTIES mysql)
      if [[ -z $DATABASE_TYPE ]]; then
        "$DATABASE_TYPE"="mysql"
      fi
      read -r -p "Before running the upgrade script, please confirm whether the metadata of MDX for Kylin has been backed up. [yes|no] " input

        case $input in
            [yY][eE][sS]|[yY])
            if [[ $(curl -k -s -f -o /dev/null -w %{http_code} ${PROTOCOL_TYPE}://127.0.0.1:"${OLD_SEMANTIC_PORT}""${CONTEXT_PATH}"api/health) -eq 200 ]]; then
              echo "Please stop the old MDX server before upgrade."
              exit 1
            else
	            echo "Start the upgrade, we will upgrade the config and metadata"
	            read -r -p "Copy Logs Directory  [yes|no]" input
	                case $input in
	                     [yY][eE][sS]|[yY])
	                     semantic-mdx/scripts/tool.sh "upgrade" $OLE_DIRECTORY $dir "copyLog" $DATABASE_TYPE
	                     ;;
	                     *)
	                     semantic-mdx/scripts/tool.sh "upgrade" $OLE_DIRECTORY $dir "noCopyLog" $DATABASE_TYPE
	                     ;;
	                esac
            fi
	          ;;

            [nN][oO]|[nN])
		        echo "Stop the upgrade, you can backup the MDX metadata"
            exit 1
       	    ;;

            *)
	          echo "Invalid input, please input \"y,Y,yes,YES,Yes,yEs,yeS,YEs,yES,YeS \" or \" n,N,No,nO,NO \""
          	exit 1
         		;;
        esac
        ;;
    service)
      ALL_PARA=($@)
      semantic-mdx/scripts/service.sh ${ALL_PARA[*]:1:3}
      ;;
    *)
        echo "Invalid option: $1"
        echo "Usage: mdx.sh start|pure|stop|ps|kill|sync|encrypt|service"
        ;;
esac
