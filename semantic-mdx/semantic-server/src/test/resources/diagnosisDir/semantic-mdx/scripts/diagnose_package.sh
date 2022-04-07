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


#go to semantic-mdx directory to make sure that relative directory can work normally
cd -P -- "$(dirname -- "$0")" && pwd -P
cd ../

#set directories
begin_time="$1"
end_time="$2"
query_time="$3"
ip="$4"
port="$5"
empty_string=""

if [ "$query_time" == "$empty_string" ]
then
  query_time=$(date "+%Y%m%d_%H%M%S")
fi

if [ "$begin_time" == "$empty_string" ]
then
  #no time chosen, all data included
  type="full"
  source_dirs=("conf" "logs" "semantic-mdx/public/WEB-INF/schema")
else
  #time specified, only part data included
  type="part"
  source_dirs=("diagnosis_tmp/conf_${query_time}" "diagnosis_tmp/logs_${query_time}" "diagnosis_tmp/schema_${query_time}")
fi
intermediate_dir="${ip}_${port}_${query_time}_diagnose_package"
dest_dir="diagnose_packages"
dest_file_name="${query_time}_diagnose_package.tar.gz"


#make directories if necessary
mkdir -p "${intermediate_dir}"

if [ ! -d ${dest_dir} ]
then
    echo "destination directory does not exist, new one generated"
    mkdir -p "${dest_dir}"
fi

if [ -f ../diagnosis_tmp/conf_${query_time}/mdx_env ]
then
    mv ../diagnosis_tmp/conf_${query_time}/mdx_env ${intermediate_dir}/
fi

if [ -f ../diagnosis_tmp/conf_${query_time}/sync_info ]
then
    mv ../diagnosis_tmp/conf_${query_time}/sync_info ${intermediate_dir}/
fi

#copy if source directory exist
for source_dir in "${source_dirs[@]}"
do
    if [ ! -d ../${source_dir} ]
    then
        #datasource does not exist, remove intermediate directory and exit with code 1
        cd -P ../ && pwd -P
        echo "${source_dir} does not exist, please check it"
        cd -P semantic-mdx/
    else
        cp -r ../${source_dir} ${intermediate_dir}/
        if [ $type == "part" ]
        then rm -r ../${source_dir}
        fi
    fi
done

#make archive file in current directory
tar -zcvf ./${dest_file_name} ./${intermediate_dir}
#remove intermediate directory
rm -r ./${intermediate_dir}
#move generated package to ../ directory
mv ./${dest_file_name} ./${dest_dir}/${dest_file_name}

#describe the location of generated package
echo "Generated package is under:"
cd -P -- ${dest_dir} && pwd -P
echo "the package name is:"
echo "${dest_file_name}"
exit 0
