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
cd $2
cd ../

#set directories
dest_file_name="$1"
old_directory="$2"
single_directory="$3"
new_directory=$4
compress_directory=${dest_file_name%.*}
compress_directory=${compress_directory%.*}
echo "dest_file_name: $dest_file_name, old_directory: $old_directory, single_directory: $single_directory"
mv $single_directory $compress_directory
#make archive file in current directory
tar -zcvf $dest_file_name $compress_directory >> /dev/null 2>&1
#remove old MDX directory
rm -r $old_directory
rm -r $compress_directory
mv $new_directory $old_directory

