## FAQ

The following a list of frequently asked questions about MDX for Kylin.

**Q: An explanation of MDX for Kylin query cache**

A: MDX for Kylin will clear the cache in the following situations:

- MDX for Kylin will actively detect Kylin Segment changes. After discovering changes, MDX for Kylin will actively clear the cache.

- Dataset metadata changes. Any modification to the dataset will invalidate the query cache of the current dataset.

- Execute the clear MDX query cache command to clear the query cache. [Reference manual](../rest/query.en.md)

	The command is as follows:

	```
	curl -X GET \
	'http://host:port/mdx/xmla/clearCache' \
	-H 'Authorization: Basic QURNSU46S1lMSU4='
	```
	> Note: After executing the clear cache command, the query cache of all MDX project will be refreshed;

- The MDX query cache automatically expires after 12 hours. The expiration time can be adjusted by setting parameter `insight.mdx.mondrian.cache.expire-minute` in profile `$MDX_HOME/conf/insight.properties`, with the parameter default value of 12 hours.
