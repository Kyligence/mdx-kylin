## JVM Configurations

You can configure the JVM in the configuration file `$MDX_HOME/semantic-mdx/set-jvm.sh`. The default configuration uses less memory, you can adjust it according to your actual situation. The default value of this configuration is as follows:

```bash
jvm_xms=-Xms3g
jvm_xmx=-Xmx16g
```

You can change it to modify its configuration.

```bash
jvm_xms=-Xms3g # Initial memory of JVM when MDX for Kylin starts.
jvm_xmx=-Xmx16g # The maximum memory of JVM when MDX for Kylin starts.
```
In addition, you can also modify it in `$MDX_HOME/conf/insight.properties`, which has a higher priority than the above `set-jvm.sh` file. An example is as follows

```
insight.mdx.jvm.xms=-Xms3g
insight.mdx.jvm.xmx=-Xmx16g
```

When MDX for Kylin is started, the system will load the parameters defined in the configuration file `$MDX_HOME/semantic-mdx/set-jvm.sh` by default. If you modify the parameters, you need to **restart MDX for Kylin** to make the new parameter values take effect.
