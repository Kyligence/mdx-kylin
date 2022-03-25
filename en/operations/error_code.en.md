## Error Code

### Error Naming Rules

In order to make the error message more concise, clear and traceable, the error codes are divided according to the service module and business category, and the format is: `MDX-AABBCCCC`

+ AA: Service modules, such as system service, semantic service, MDX service, etc.
+ BB: Business categories under service module.
+ CCCC: The specific reason for the error, such as the string length exceeds the limit, the query result is too long, etc.



The specific service modules and business category list are as follows.

| Service Module      | Business Category        |
| ------------------- | ------------------------ |
| 01 System Service   | 0102 System Config User  |
| 01 System Service   | 0104 Diagnose Package    |
| 01 System Service   | 0105 Sync Task           |
| 01 System Service   | 0106 User / Group / Role |
| 01 System Service   | 0107 DB                  |
| 01 System Service   | 0108 System Tool         |
| 02 Semantic Service | 0201 Kylin API CALL |
| 02 Semantic Service | 0202 Dataset             |
| 03 MDX Service      | 0301 Schema              |
| 03 MDX Service      | 0302 XMLA                |
| 03 MDX Service      | 0303 Query               |
| 03 MDX Service      | 0304 Rewrite / Reject    |
| 04 Authority        | 0401 Authentication      |
| 04 Authority        | 0402 Authorization       |
| 04 Authority        | 0403 User Delegation     |

### Error Code List

| Error Code   | Error Message                                                |
| ------------ | ------------------------------------------------------------ |
| MDX-00000001 | Internal error                                               |
| MDX-01020001 | Sync user not configured                                     |
| MDX-01020002 | Property parsing error                                       |
| MDX-01040001 | Not generating diagnostic package on current node            |
| MDX-01040002 | Dataset not found in diagnostic package generation           |
| MDX-01040003 | Diagnostic package generation node error                     |
| MDX-01040004 | Dataset serializing error in diagnostic package generation   |
| MDX-01040005 | Diagnostic package file not found                            |
| MDX-01040006 | Diagnostic package downloading error                         |
| MDX-01050001 | Invalid sync user information                                |
| MDX-01050002 | Illegal sync user information updating request               |
| MDX-01060001 | Invalid role name                                            |
| MDX-01060002 | Duplicate role name                                          |
| MDX-01060003 | User error                                                   |
| MDX-01060004 | Role not exists                                              |
| MDX-01060005 | User not exists                                              |
| MDX-01060006 | Deleting role with users assigned                            |
| MDX-01060007 | Invalid role                                                 |
| MDX-01060008 | Cannot delete the ADMIN role                                 |
| MDX-01060009 | Role ID and name not match                                   |
| MDX-01060010 | Adding / Deleting more than 1 user to / from a role          |
| MDX-01070001 | Dataset record insertion error                               |
| MDX-01070002 | MDX_QUERY record insertion error                             |
| MDX-01070003 | SQL_QUERY record insertion error                             |
| MDX-01070004 | Role record insertion error                                  |
| MDX-01070005 | Role record deleting error                                   |
| MDX-01080001 | User password encryption error                               |
| MDX-01080002 | User password decryption error                               |
| MDX-01080005 | set-jvm script reading error                                 |
| MDX-01080006 | set-jvm script writing error                                 |
| MDX-01080007 | MDX properties file reading error                            |
| MDX-01080008 | MDX properties file writing error                            |
| MDX-01080009 | MDX version format error                                     |
| MDX-02010002 | Fetching Kylin tables description error |
| MDX-02010003 | Fetching Kylin models description error |
| MDX-02010004 | Fetching Kylin project list error     |
| MDX-02010005 | Fetching Kylin user list error        |
| MDX-02010006 | Fetching Kylin users by project error |
| MDX-02010007 | Fetching Kylin users by group error   |
| MDX-02010008 | Fetching Kylin user authorities error |
| MDX-02010009 | Fetching Kylin project access information error |
| MDX-02010010 | HTTP request with empty URI                                  |
| MDX-02010011 | Building URI instance error                                  |
| MDX-02010012 | Kylin host / port not configured      |
| MDX-02010013 | Kylin connection error                |
| MDX-02010014 | Kylin HTTP request failed             |
| MDX-02010015 | Reading Kylin HTTP response error     |
| MDX-02020002 | Fetching dataset by project error                            |
| MDX-02020003 | Fetching dataset by ID error                                 |
| MDX-02020004 | Duplicate dataset name in project                            |
| MDX-02020006 | Duplicate dimension table name                               |
| MDX-02020007 | Duplicate dimension table alias                              |
| MDX-02020008 | Duplicate measure alias                                      |
| MDX-02020009 | Duplicate calculated measure name                            |
| MDX-02020010 | Duplicate namedset name                                      |
| MDX-02020011 | Folder name format error                                     |
| MDX-02020012 | Calculate measure validation error                           |
| MDX-02020014 | Illegal default member validation request                    |
| MDX-02020015 | Unsupported dataset type                                     |
| MDX-02020016 | MDX server connection error                                  |
| MDX-02020017 | Dataset connection error                                     |
| MDX-02020018 | Invalid dimension usage                                      |
| MDX-02020019 | Hierarchy contains column from another table                 |
| MDX-02020020 | Fetching dimension table by model error                      |
| MDX-02020021 | Fetching dimension by dimension table and model error        |
| MDX-02020022 | Fetching measure by model error                              |
| MDX-02020023 | Fetching calculated measure error                            |
| MDX-02020024 | Fetching namedset error                                      |
| MDX-02020025 | Invalid dataset found                                        |
| MDX-02020026 | Kylin project error                   |
| MDX-02020027 | Invalid dataset list fetching request                        |
| MDX-02020028 | Invalid or expired dataset export token                      |
| MDX-02020029 | Dataset export compression error                             |
| MDX-02020030 | Invalid dataset import request                               |
| MDX-02020031 | Dataset import decompression error                           |
| MDX-02020032 | Invalid dataset Zip file                                     |
| MDX-02020033 | No valid dataset in uploaded file                            |
| MDX-03010003 | Datasource file not found                                    |
| MDX-03010004 | Creating datasource file error                               |
| MDX-03010005 | Creating schema directory error                              |
| MDX-03010006 | Creating schema file error                                   |
| MDX-03010007 | Serializing schema file error                                |
| MDX-03020002 | Drillthrough only supports tabular data format               |
| MDX-03020003 | Drillthrough error                                           |
| MDX-03020005 | URL format of XML/A request error                            |
| MDX-03030001 | MDX dataset not found                                        |
| MDX-03030002 | MDX object not found                                         |
| MDX-03030003 | MDX member resolving error                                   |
| MDX-03030004 | MDX statement parsing error                                  |
| MDX-03030014 | Hierarchy in calculated member not found                     |
| MDX-03030015 | Wrong return type of axis expression                         |
| MDX-03030016 | Wrong return type of member expression                       |
| MDX-03030017 | Wrong return type of set expression                          |
| MDX-03030018 | Missing function argument                                    |
| MDX-03030019 | Wrong function argument type                                 |
| MDX-03030020 | Unknown MDX argument                                         |
| MDX-03030021 | MDX member not found                                         |
| MDX-03030022 | Wrong return type of default member expression               |
| MDX-03030023 | Invalid member encountered                                   |
| MDX-03030024 | No signature matching function found                         |
| MDX-03030025 | More than one signature matching function found              |
| MDX-03030031 | Hierarchy appears on multiple axes                           |
| MDX-03030032 | Function arguments do not belong to the same hierarchy       |
| MDX-03030033 | Function arguments do not belong to a time dimension table   |
| MDX-03030034 | Invalid axis number                                          |
| MDX-03030035 | Duplicate axis name                                          |
| MDX-03030037 | Tuple contains members of the same hierarchy                 |
| MDX-03030038 | Argument type error of VisualTotals function                 |
| MDX-03030042 | Invalid data type cast target                                |
| MDX-03030043 | Function does not support NULL member argument               |
| MDX-03030045 | Cannot use this function because no time dimension has been defined |
| MDX-03030046 | Did not explicitly specify the target hierarchy              |
| MDX-03030047 | No accessible element in the hierarchy                       |
| MDX-03030050 | Drillthrough disabled                                        |
| MDX-03030051 | Performing drillthrough over an unknown member               |
| MDX-03030054 | Properties missing in connection string                      |
| MDX-03030055 | Hierarchy belong to a time dimension table not defined as time type |
| MDX-03030056 | Hierarchy not belong to a time dimension table defined as time type |
| MDX-03030064 | Calculated member belongs to an invalid dimension            |
| MDX-03030068 | Duplicate calculated member name                             |
| MDX-03030076 | UDF class not found                                          |
| MDX-03030077 | Illegal UDF class definition                                 |
| MDX-03030079 | Duplicate UDF name                                           |
| MDX-03030080 | Duplicate namedset name                                      |
| MDX-03030081 | Invalid calculated measure or namedset expression in dataset |
| MDX-03030087 | Invalid aggregator                                           |
| MDX-03030090 | Descendants function argument type error                     |
| MDX-03030092 | Cannot infer type of set elements                            |
| MDX-03030094 | Hierarchy definition contains no level                       |
| MDX-03030095 | Hierarchy definition contains levels with duplicate name     |
| MDX-03030103 | Crossjoin result size exceeds limit                          |
| MDX-03030104 | Query result size exceeds limit                              |
| MDX-03030105 | Number of members to be read exceeds limit                   |
| MDX-03030106 | Number of measure values to be read exceeds limit            |
| MDX-03030107 | Query canceled                                               |
| MDX-03030108 | Query timeout                                                |
| MDX-03030109 | Number of calculation iterations exceeds limit               |
| MDX-03030110 | Hierarchy contains no member                                 |
| MDX-03030157 | Native optimization cannot be applied to function            |
| MDX-03030158 | Query resources recycling error                              |
| MDX-03030159 | Number of concurrent MDX queries exceeds limit               |
| MDX-03030160 | Number of concurrent SQL queries from Mondrian engine exceeds limit |
| MDX-03030162 | Number of concurrent SQL queries from Sniper engine exceeds limit |
| MDX-03030163 | RolapSchema instance recycling error                         |
| MDX-03030164 | MondrianServer instance recycling error                      |
| MDX-03030165 | RolapConnection instance recycling error                     |
| MDX-03030166 | Aggregation query execution cancelled due to time out        |
| MDX-03030167 | Aggregation query execution cancelled                        |
| MDX-03030168 | SniperResult construction cancelled                          |
| MDX-03040003 | MDX cluster nodes not configured                             |
| MDX-03040004 | Cannot rewrite expression                                    |
| MDX-03040005 | Expression is not a function expression                      |
| MDX-03040006 | Expression is not a specified function expression            |
| MDX-03040007 | Expression is not an identifier                              |
| MDX-03040008 | Invalid namedset Union function expression                   |
| MDX-03040009 | Invalid namedset Except function expression                  |
| MDX-04010001 | User login authentication error                              |
| MDX-04010002 | Invalid basic authentication information                     |
| MDX-04010003 | Invalid cookie authentication information                    |
| MDX-04010004 | Invalid gateway authentication information                   |
| MDX-04010005 | Authentication information missing                           |
| MDX-04010006 | User name / password error                                   |
| MDX-04010007 | User locked                                                  |
| MDX-04010008 | Kylin authentication failed           |
| MDX-04010009 | User disabled                                                |
| MDX-04020001 | Not an ADMIN user                                            |
| MDX-04020002 | Not an ADMIN user or project admin user                      |
| MDX-04020003 | User has no project access                                   |
| MDX-04020004 | No dataset available                                         |
| MDX-04020005 | All datasets in project are broken                           |
| MDX-04020006 | User has no access to any dataset in project                 |
| MDX-04030001 | User delegation feature not enabled                          |
| MDX-04030002 | Empty delegation user parameter                              |
| MDX-04030003 | Delegation user parameter too long                           |
| MDX-04030004 | Invalid delegation user parameter                            |
| MDX-04030005 | Delegation user permissions inadequate                       |
