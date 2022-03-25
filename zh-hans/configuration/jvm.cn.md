## JVM 参数配置

您可以在配置文件 `$MDX_HOME/semantic-mdx/set-jvm.sh` 中，进行 JVM 的配置。默认配置使用的内存较少，您可以根据自己的实际情况调节。该项配置的默认值如下:

```bash
jvm_xms=-Xms3g
jvm_xmx=-Xmx16g
```

您可以更改它，以修改其中的配置。

```bash
jvm_xms=-Xms3g  # MDX for Kylin 启动时的 JVM 初始内存。
jvm_xmx=-Xmx16g  # MDX for Kylin 启动时的 JVM 最大内存。
```

另外您也可以在 `$MDX_HOME/conf/insight.properties` 中，更改 JVM 配置，此设置的优先级将高于上述 `set-jvm.sh`，配置如下：

```
insight.mdx.jvm.xms=-Xms3g
insight.mdx.jvm.xmx=-Xmx16g
```


在启动 MDX for Kylin 时，系统会默认加载配置文件 `$MDX_HOME/semantic-mdx/set-jvm.sh` 中定义的参数。如果您修改了参数，需要**重新启动 MDX for Kylin** 才能使新参数值生效。
