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
dir=$(cd -P -- "$(dirname -- "$0")" && pwd -P)


echo "Start to build binary package for MDX for Kylin."
if [ ! -d $dir/../../dist ];then
  mkdir -p $dir/../../dist
else
  rm -rf $dir/../../dist
  mkdir -p $dir/../../dist
fi

if [ -z "$version" ]; then
    echo "version not set, use UNKNOWN instead."
    version="UNKNOWN"
else
    echo "Version: $version"
fi

if [ -z "$changelog" ]; then
    echo "changelog not set, use UNKNOWN instead."
    changelog="UNKNOWN"
else
    echo "CHANGELOG: $changelog"
fi

package_name=mdx-kylin-${version}
tmp_dir=$dir/../$package_name
rm -rf $tmp_dir
mkdir -p $tmp_dir
echo `git rev-parse HEAD` | tee $tmp_dir/commit_SHA1

# build js for semantic-mdx
cd $dir/../../client
npm run build

# build tarball for semantic-mdx
echo "====================================="
echo "Build tarball for MDX "
cd $dir/../../semantic-mdx
mvn clean install -Pprod -Dmaven.test.skip=true -pl semantic-deploy -am
cp semantic-deploy/target/*.tar.gz $tmp_dir/semantic-mdx.tar.gz

# combine two tarball packages
cd $tmp_dir
tar -zxvf semantic-mdx.tar.gz
rm *.tar.gz
mv semantic* semantic-mdx
cp ../../LICENSE .
cp ../../NOTICE .
cp -r ../bin .
cp -r ../conf .
echo "$changelog" > ./CHANGELOG.md
echo "$version" > ./VERSION

cd ..
tar -zcvf $package_name.tar.gz $package_name
cd ..
mv build/$package_name.tar.gz dist/
# finally remove useless temp dir
rm -rf $tmp_dir

# build tarball for semantic-mdx
echo "====================================="
echo "Build Finished!"
echo "Location: dist/$package_name.tar.gz"
