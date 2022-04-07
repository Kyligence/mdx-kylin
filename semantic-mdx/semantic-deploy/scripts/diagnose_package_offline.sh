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

#Step 0. Set parameters and check target
#Go to semantic-mdx directory to make sure that relative directory can work normally
cd -P -- "$(dirname -- "$0")" && pwd -P
cd ../../

# Set time period
begin_time="$1"
end_time="$2"

#Set directories
query_time=$(date "+%Y%m%d_%H%M%S")

#The ip of current MDX node
if [ "$3" == "" ]; then
  ip="localhost"
else
  ip="$3"
fi
#The port of current MDX node
if [ "$4" == "" ]; then
  port="7080"
else
  port="$4"
fi
#Choose to dump mysql or not, input as 'y' or 'n'
if [ "$5" == "" ]; then
  dump="y"
else
  dump="$5"
fi

# The destination directory
if [ "$6" == "" ]; then
  dest_dir="../semantic-mdx/diag_dump"
else
  if [[ $6 =~ ^/.* ]]; then
    dest_dir="$6"
  else
    dest_dir="../semantic-mdx/scripts/$6"
  fi
  if [ ! -d "${dest_dir}" ]; then
    mkdir -p ${dest_dir}
  fi
fi


empty_string=""

temp_dir="diagnosis_tmp"
intermediate_dir="${ip}_${port}_${query_time}_diagnose_package"

#make directories if necessary
mkdir -p "${temp_dir}/${intermediate_dir}"

package_log="${temp_dir}/${intermediate_dir}/package.log"
package_error="${temp_dir}/${intermediate_dir}/package.error"
package_out="${temp_dir}/${intermediate_dir}/package.out"

#Step 1. Prepare directories and package log file
echo "============================================================" >> ${package_log}
echo "Step 1. Start preparing directories." >> ${package_log}
echo "Current path is : `pwd -P`" >> ${package_log}
if [ "$query_time" == "$empty_string" ]
then
  query_time=$(date "+%Y%m%d_%H%M%S")
fi

source_dirs=("conf" "logs" "semantic-mdx/public/WEB-INF/schema")

envFile="${temp_dir}/${intermediate_dir}/mdx_env"

dest_file_name="${ip}_${port}_${query_time}_diagnose_package.tar.gz"

echo "package log file is created." >> ${package_log}

#Step 2. Extract environmental information
echo "============================================================" >> ${package_log}
echo "Step 2. Start extracting environmental information." >> ${package_log}
echo "Current path is : `pwd -P`" >> ${package_log}
echo "Start extracting environmental information." >> ${package_log}

echo "mdx.home: ${SPARK_HOME}" >> ${envFile}
read version < "VERSION"
echo "mdx.version: ${version}" >> ${envFile}
##
read commitId < "commit_SHA1"
echo "commit: ${commitId}" >> ${envFile}

javaVersion=`java -version 2>&1 | sed '1!d' | sed -e 's/"//g' -e 's/version//'`
echo "java.version: ${javaVersion}" >> ${envFile}

echo "------------ get top info ---------------" >> ${envFile}
top -b -n 1 >> ${envFile}

#Step 3. Generate json files for meta data of datasets
echo "============================================================" >> ${package_log}
echo "Step 3. Start retrieving meta data of datasets." >> ${package_log}
echo "Current path is : `pwd -P`" >> ${package_log}
echo "Start retrieving meta data of datasets." >> ${package_log}

database_host=`grep 'insight.database.ip=' "conf/insight.properties" | awk -F= '{print $2}'`
database_port=`grep 'insight.database.port=' "conf/insight.properties" | awk -F= '{print $2}'`

database_name=`grep 'insight.database.name=' "conf/insight.properties" | awk -F= '{print $2}'`
database_usr=`grep 'insight.database.username=' "conf/insight.properties" | awk -F= '{print $2}'`
database_pwd=`grep 'insight.database.password=' "conf/insight.properties" | awk -F= '{print $2}'`

#echo "${database_pwd}" >> ${package_log}
#echo "Current path is : `pwd -P`" >> ${package_log}

cd "semantic-mdx/scripts"
database_pwd=`. "tool.sh" decrypt ${database_pwd}`
cd "../.."

#echo "${database_pwd}" >> ${package_log}

database_pwd=${database_pwd#*"The decryption string: "}


ignore_table="${database_name}.sql_query"
ignore_table2="${database_name}.mdx_query"

if [ ${dump} == "y" ]
	then
		datasets_dir="${temp_dir}/${intermediate_dir}/datasets"
		mkdir ${datasets_dir}
		echo "Choose to dump mysql." >> ${package_log}
		mysqldump -h"${database_host}" -P"${database_port}" -u"${database_usr}" -p"${database_pwd}" "${database_name}" --ignore-table="${ignore_table}" --ignore-table="${ignore_table2}" > "${datasets_dir}/mysqldump.sql"
	else
		echo "Choose not to dump mysql." >> ${package_log}
fi

#Step 4. Copy schema and datasource xml files and configuration file
echo "============================================================" >> ${package_log}
echo "Step 4. Start copying schema xml files and configuration file." >> ${package_log}
echo "Current path is : `pwd -P`" >> ${package_log}
echo "Start copying schema xml files." >> ${package_log}
if [ ! -d "${temp_dir}/${intermediate_dir}/schema_${query_time}/" ]
then
	echo "${temp_dir}/${intermediate_dir}/schema_${query_time} created." >> ${package_log}
	mkdir "${temp_dir}/${intermediate_dir}/schema_${query_time}"
fi
cp -r "semantic-mdx/public/WEB-INF/schema/" "${temp_dir}/${intermediate_dir}/schema_${query_time}/" >> ${package_log}
cp -r "conf/" "${temp_dir}/${intermediate_dir}/conf_${query_time}/" >> ${package_log}

cp "semantic-mdx/set-jvm.sh" "${temp_dir}/${intermediate_dir}/conf_${query_time}/" >> ${package_log}
cp "semantic-mdx/startup.sh" "${temp_dir}/${intermediate_dir}/conf_${query_time}/" >> ${package_log}

#Step 5. Extract log information from log files
echo "============================================================" >> ${package_log}
echo "Step 5. Start extracting log information from log files." >> ${package_log}
echo "Current path is : `pwd -P`" >> ${package_log}

source_dir="logs"
log_dir="${temp_dir}/${intermediate_dir}/logs_${query_time}"

echo "Time range is ${begin_time} to ${end_time}" >> ${package_log}

if [ ! -d "${source_dir}" ]
then
	echo "${source_dir} does not exist, please check it" >> ${package_log}
else
	mkdir ${log_dir}
	for log_file in `ls "${source_dir}"`
	do
		echo "${log_file}" >> ${package_log}

		source_file="`pwd -P`/${source_dir}/${log_file}"
		result_log_file="`pwd -P`/${log_dir}/${log_file}"

		cd "semantic-mdx/scripts"
		log_content=`. "tool.sh" log ${source_file} "${begin_time}" "${end_time}"`
		cd "../.."
		echo "${log_content}" >> ${result_log_file}

		if [ ! -s ${result_log_file} ]
		then
			echo "No data in this time range." >> ${result_log_file}
		fi
	done
fi


#Step 6. Package the prepared files
echo "============================================================" >> ${package_log}
echo "Step 6. Start packaging all files prepared." >> ${package_log}
echo "Current path is : `pwd -P`" >> ${package_log}

#describe the location of generated package
echo "Generated package is under: ${dest_dir}" >>${package_log}
echo "the package name is: ${dest_file_name}" >>${package_log}
#make archive file in current directory
cd "./${temp_dir}"
package_log="${intermediate_dir}/package.log"

tar -zcvf "${dest_file_name}" "${intermediate_dir}" >>${package_log}

#move generated package to destination directory
mv "${dest_file_name}" "${dest_dir}/"
#remove intermediate directory
rm -r "${intermediate_dir}"

if [ "$6" != "" ]; then
  dest_dir="$6"
fi
echo " Package finished, you can get Diagnostic package from: $dest_dir"

exit 0
