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

KYLIN_MDX_PATH="/usr/local/kylin_mdx"
MYSQL_HOST="10.1.2.32"
HOST_PASSWORD=hadoop
MYSQL_USER=test
MYSQL_PASSWORD=root

cd ${KYLIN_MDX_PATH}
echo "tar -zxvf mdx-kylin-*.tar.gz"
tar -zxvf mdx-kylin-*.tar.gz
cd Kylin-MDX*
sed -i 's/insight.kylin.host=localhost/insight.kylin.host=10.1.2.32/g;
s/insight.kylin.port=7070/insight.kylin.port=7090/g;
s/insight.database.ip=localhost/insight.database.ip=10.1.2.32/g
s/insight.database.name=insight/insight.database.name=kylin_mdx_docker/g;
s/insight.database.username=root/insight.database.username=test/g;
s/insight.semantic.port=7080/insight.semantic.port=7989/g;' conf/insight.properties

echo "begin install Kylin MDX"

sshpass -p ${HOST_PASSWORD} ssh -tt root@${MYSQL_HOST} << EOF
mysql -u${MYSQL_USER} -p${MYSQL_PASSWORD}
DROP DATABASE IF EXISTS kylin_mdx_docker;
CREATE DATABASE IF NOT EXISTS kylin_mdx_docker DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
exit;
exit 0
EOF

case "$1" in
  suse11)
    echo ">>>>>>>>>>>>>>>>>>>> start to install  Kylin MDX in suse11 <<<<<<<<<<<<<<<<<<<<<<<"
    sh bin/mdx.sh pure
    echo ">>>>>>>>>>>>>>>>>>> finished installing  Kylin MDX in suse11 <<<<<<<<<<<<<<<<<"
    ;;
  centos6)
    echo ">>>>>>>>>>>>>>>>>>> start to install  Kylin MDX in centos6 <<<<<<<<<<<<<<<<<<<<<<<"
    sh bin/mdx.sh pure
    echo ">>>>>>>>>>>>>>>>>>> finished installing  Kylin MDX in centos6 <<<<<<<<<<<<<<<<"
    ;;
  centos7)
    echo ">>>>>>>>>>>>>>>>>>> start to install  Kylin MDX in centos7 <<<<<<<<<<<<<<<<<<<<<<<"
    sh bin/mdx.sh pure
    echo ">>>>>>>>>>>>>>>>>> finished installing  Kylin MDX in centos7 <<<<<<<<<<<<<<<<<"
    ;;
  ubuntu16)
    echo ">>>>>>>>>>>>>>>>> start to install  Kylin MDX in ubuntu16 <<<<<<<<<<<<<<<<<<<<<<<<"
    sh bin/mdx.sh pure
    echo ">>>>>>>>>>>>>>>>> finished installing  Kylin MDX in ubuntu16 <<<<<<<<<<<<<<<<<"
esac
