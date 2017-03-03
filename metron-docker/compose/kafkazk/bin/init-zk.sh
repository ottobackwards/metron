#!/bin/bash
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
echo "create /metron metron" | ./bin/zookeeper-shell.sh localhost:2181
echo "create /metron/topology topology" | ./bin/zookeeper-shell.sh localhost:2181
echo "create /metron/topology/parsers parsers" | ./bin/zookeeper-shell.sh localhost:2181
echo "create /metron/topology/enrichments enrichments" | ./bin/zookeeper-shell.sh localhost:2181

$METRON_HOME/bin/zk_load_configs.sh -z localhost:2181 -m PUSH -i $METRON_HOME/config/zookeeper

for p in asa base bro cef fireeye ise lancope logstash paloalto snort squid websphere yaf sourcefire
do
    $METRON_HOME/bin/zk_load_configs.sh -z localhost:2181 -m PUSH -i $METRON_HOME/telemetry/$p/config/zookeeper
done
