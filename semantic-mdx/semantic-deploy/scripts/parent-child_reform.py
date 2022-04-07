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


import sys

# width-first search, judge if there are nodes that are not in current children
# If yes, it indicates there are more levels
def findDepth(rootNodes, nodesWithChild):
    currentChildren = ""
    currentRoots = rootNodes.copy()
    restNodes = nodesWithChild.copy()
    depth = 1

    # initiate roots, current children and rest nodes
    for currentRoot in currentRoots:
        if nodesWithChild.__contains__(currentRoot):
            if currentChildren != "":
                currentChildren = currentChildren + "," + nodesWithChild[currentRoot]
            else:
                currentChildren = nodesWithChild[currentRoot]
        if restNodes.__contains__(currentRoot):
            del restNodes[currentRoot]

    # non-leaf nodes only have more than one level, level number plus
    if not currentChildren == "":
        depth = depth + 1
    while len(restNodes) > 0:
        newLevel = 0
        for restNode in restNodes:
            if not currentChildren.__contains__(restNode):
                # if there is node not in current childeren, there is new level
                depth = depth + 1
                # update roots and children to next level, remove new roots from rest nodes if they are in rest nodes
                currentRoots = currentChildren.split(",")
                currentChildren = ""
                for currentRoot in currentRoots:
                    if nodesWithChild.__contains__(currentRoot):
                        if currentChildren != "":
                            currentChildren = currentChildren + "," + nodesWithChild[currentRoot]
                        else:
                            currentChildren = nodesWithChild[currentRoot]
                newLevel = 1
                break;
        if newLevel == 1:
            for currentRoot in currentRoots:
                if restNodes.__contains__(currentRoot):
                    del restNodes[currentRoot]
        else:
            # if all rest nodes in current children, no more new levels
            # all level number = non-leaf nodes level number + 1
            restNodes.clear()
            depth = depth + 1
    return depth




def transform(input_file, output_file, encode, id_col_str, pid_col_str, name_col_str, max_level_limit_str):
    id_col_num = int(id_col_str)
    if not pid_col_str.isnumeric():
        print("Invalid value for parent id column: " + pid_col_str + ". Default value is used. This may not work properly")
        pid_col_str = "-1"
    pid_col_num = int(pid_col_str)
    name_col_num = int(name_col_str)
    input_file = open(input_file, 'r', encoding=encode)
    output_file = open(output_file, 'w', encoding=encode)
    lines = input_file.readlines()
    input_file.close()
    # all nodes and their child(ren)
    allNodes = {}
    # all nodes and their parent
    origin = {}
    # all nodes and their name
    name = {}
    # all nodes and their data part
    data = {}
    # the set of all root nodes
    rootNodes = []

    ## get the child(ren) of all nodes and the set of all nodes
    for line in lines:
        # remove possible extra characters
        line = line.replace("\n", "")
        line = line.replace("\ufeff", "")
        columns = line.split(",")
        columnNum = len(columns)
        if id_col_num >= columnNum:
            print("Invalid value for id column: " + str(id_col_num) + ". Default value is used. This may not work properly")
            id_col_num = 0
        if name_col_num >= columnNum:
            print("Invalid value for name column: " + str(name_col_num) + ". Default value is used. This may not work properly")
            name_col_num = id_col_num
        if pid_col_num == -1:
            pid_col_num = columnNum - 1
        if pid_col_num >= columnNum:
            print("Invalid value for parent id column: " + str(pid_col_num) + ". Default value is used. This may not work properly")
            pid_col_num = columnNum - 1
        # store the parent information for later use
        origin[columns[id_col_num]] = columns[pid_col_num]
        # store the name information for later use
        name[columns[id_col_num]] = columns[name_col_num]
        # store data information
        dataString = ""
        for i in range(0, columnNum):
            if i != id_col_num and i != pid_col_num and i != name_col_num:
                if dataString == "":
                    dataString = columns[i]
                else:
                    dataString = dataString + "," + columns[i]
        data[columns[id_col_num]] = dataString
        # file may be not in order, allNodes[columns[id_col_num]] may have been set already
        if not allNodes.__contains__(columns[id_col_num]):
            allNodes[columns[id_col_num]] = "N/A"

        # for non-root nodes
        if columns[pid_col_num] != "":
            # file may be not in order, allNodes[columns[pid_col_num]] may not have been set yet
            if not allNodes.__contains__(columns[pid_col_num]):
                allNodes[columns[pid_col_num]] = "N/A"
            if allNodes[columns[pid_col_num]] == "N/A":
                allNodes[columns[pid_col_num]] = columns[id_col_num]
            else:
                allNodes[columns[pid_col_num]] = allNodes[columns[pid_col_num]] + "," + columns[id_col_num]
        # for root nodes
        else:
            rootNodes += [columns[id_col_num]]


    #for node in allNodes:
    #    print(node + ":" + allNodes[node])

    # node with child(ren) and their child(ren)
    nodesWithChild = {}
    # leaf nodes
    leafNodes = {}
    for node in allNodes:
        if allNodes[node] == "N/A":
            leafNodes[node] = "N/A"
        else:
            nodesWithChild[node] = allNodes[node]

    # analyse the number of levels to be generated
    depth = findDepth(rootNodes, nodesWithChild)
    print("depth is : " + str(depth))

    # construct data to write
    reformedResults = allNodes.copy()
    # max level number limit
    max_level_limit = int(max_level_limit_str)
    print("max_level_limit is set as : " + max_level_limit_str)
    for node in allNodes:
        father = origin[node]
        content = node + "," + name[node]
        # find the path from root node to current node
        while father != "" and father not in rootNodes:
            content = father + "," + name[father] + "," + content
            father = origin[father]
        if node not in rootNodes:
            content = father + "," + name[father] + "," + content
        length = int(len(content.split(","))/2)
        # non-leaf nodes need to add NULL for generated levels
        actual_level = min(max_level_limit, depth)
        if length < actual_level:
            for i in range(0, actual_level - length):
                content = content + ",NULL,NULL"
        # for levels deeper than max limit, merge to the max limit level
        if length > max_level_limit:
            content_pieces = content.split(",")
            content = ""
            leaf_col_no = 2 * (max_level_limit - 1)
            for i in range(0, leaf_col_no):
                if content == "":
                    content = content_pieces[i]
                else:
                    content = content + "," + content_pieces[i]
            leaf_no = content_pieces[leaf_col_no]
            leaf_name = content_pieces[leaf_col_no + 1]
            for i in range(leaf_col_no + 2, len(content_pieces), 2):
                leaf_no = leaf_no + "-" + content_pieces[i]
                leaf_name = leaf_name + "-" + content_pieces[i+1]
            if content == "":
                content = leaf_no + "," + leaf_name
            else:
                content = content + "," + leaf_no + "," + leaf_name
        # add the unique id at the first column
        content = node + "," + name[node] + "," + content + "," + data[node] + "\n"
        reformedResults[node] = content

    for node in reformedResults:
        output_file.write(reformedResults[node])
    output_file.close()

def check_before_transform(args):
    prepared_parameters = {
        "input_file": "",
        "output_file": "",
        "encoding": "utf-8",
        "id_column_number": "0",
        "pid_column_number": "-1",
        "name_column_number": "0",
        "limit": "10"
    }
    parameter_dic = {
        "-i": "input_file",
        "--input": "input_file",
        "-o": "output_file",
        "--output": "output_file",
        "-e": "encoding",
        "--encoding": "encoding",
        "-ic": "id_column_number",
        "--id_column": "id_column_number",
        "-p": "pid_column_number",
        "--parent_id_column": "pid_column_number",
        "-n": "name_column_number",
        "--name_column": "name_column_number",
        "-lt": "limit",
        "--limit": "limit"
    }
    allowed_parameters = ["--help", "-i", "--input", "-o", "--output", "-e", "--encoding", "-ic", "--id_column", "-p",
                          "--parent_id_column", "-n", "--name_column", "-lt", "--limit"]
    invalidParameter = False
    # check invalidParameter
    if len(args) % 2 != 1 or len(args) < 3:
        invalidParameter = True
    if invalidParameter:
        print("There is parameter missing, please check. Please input '--help' as parameter if any problem")
    for i in range(1, len(args), 2):
        if args[i] not in allowed_parameters:
            invalidParameter = True
            break;
    if invalidParameter:
        print("There is invalid parameter, please input '--help' as parameter if any problem")
    else:
        # assign parameters
        for i in range(1, len(args), 2):
            prepared_parameters[parameter_dic[args[i]]] = args[i + 1]
        # check if output path still default values
        if prepared_parameters["output_file"] == "":
            prepared_parameters["output_file"] = prepared_parameters["input_file"].replace(".csv", "_transfered.csv")
        if prepared_parameters["name_column_number"] == "0":
            prepared_parameters["name_column_number"] = prepared_parameters["id_column_number"]

        limit = prepared_parameters["limit"]
        ic = prepared_parameters["id_column_number"]
        name = prepared_parameters["name_column_number"]
        if not limit.isnumeric() or int(limit) <= 0:
            print("Invalid value for limit: " + str(limit) + ". Default value is used. This may not work properly")
            prepared_parameters["limit"] = "10"
        if not ic.isnumeric() or int(ic) < 0:
            print("Invalid value for id column: " + str(ic) + ". Default value is used. This may not work properly")
            prepared_parameters["id_column_number"] = "0"
        if not name.isnumeric() or int(name) < 0:
            print("Invalid value for name column: " + str(name) + ". Default value is used. This may not work properly")
            prepared_parameters["name_column_number"] = prepared_parameters["id_column_number"]
        transform(
            prepared_parameters["input_file"],
            prepared_parameters["output_file"],
            prepared_parameters["encoding"],
            prepared_parameters["id_column_number"],
            prepared_parameters["pid_column_number"],
            prepared_parameters["name_column_number"],
            prepared_parameters["limit"],
        )

def main():
    args = sys.argv
    quit = False
    if len(args) >= 2:
        if args[1] == "--help":
            print("Allowed parameters:")
            print(" --help: for help information")
            print(" [Mandatory] -i, --input: the path of input csv file, absolute path recommended")
            print(" [Optional]  -o, --output: the path of output csv file, in the same directory as input file and name with a suffix '_transfered' by default")
            print(" [Optional] -e, --encoding: the encoding for input and output csv file, utf-8 by default")
            print(" [Optional] -ic, --id_column: the position of id column, start from 0, 0 by default")
            print(" [Optional] -p, --parent_id_column: the position of parent id column, start from 0, the last column by default")
            print(" [Optional] -n, --name_column: the position of name column, start from 0, the same as id column by default")
            print(" [Optional] -lt, --limit: the max level number limit, 10 by default")
            print("Default parameters may lead to incorrect result depending on the strcuture of input file. Giving as many arameters as possible is recommended!")
            quit = True
    if not quit:
        check_before_transform(args)

main()
