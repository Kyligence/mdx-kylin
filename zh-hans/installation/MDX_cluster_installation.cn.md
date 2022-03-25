## MDX 集群部署

- [MDX 实例安装配置](#mdx-实例安装配置)
- [Nginx 安装配置](#nginx-安装配置)
- [MDX 集群访问方式](#mdx-集群访问方式)
- [注意事项](#注意事项)

### MDX 实例安装配置

   从 [Kyligence 提供的下载地址](https://s3.cn-north-1.amazonaws.com.cn/public.kyligence.io/kylin/tar/mdx-for-kylin-1.0.0-beta.tar.gz) 下载 MDX for Kylin 安装包后，按照手册说明安装必要的组件、修改必要的配置参数，详细请参考[在 Linux 环境安装](install_linux.cn.md)。
   
   集群部署时，为保证能够正常使用生成诊断包功能，需要在每一个 MDX 节点的 `${MDX_HOME}/conf/insight.properties` 文件中添加配置项 `insight.mdx.cluster.nodes=<ip_A>:<port_A>,<ip_B>:<port_B>`，将集群中所有 MDX for Kylin 节点的 ip 和 port 信息加入配置文件中。

### Nginx 安装配置

   Nginx 安装请参考 Nginx 官网安装文档。由于 MDX for Kylin 根据自定义 `header`：`x-Host` 和 `x-Port` 以将请求转发到指定的节点上，以确保可以在页面上通知多个节点生成诊断包并下载，您需要在 Nginx 配置文件中添加路由规则。例如针对 **Nginx 1.13.7** ，对两台机器上的 A、B 两个 MDX for Kylin 节点，其 ip 和 port 分别为 `<ip_A>:<port_A>` 和 `<ip_B>:<port_B>`，则需要在 Nginx 的配置文件中添加如下配置：
   ```properties
   # <nodes> can be customized name
   upstream <nodes> {
	   server <ip_A>:<port_A>;
	   server <ip_B>:<port_B>;
	   }
	
	server {
		listen <port_N>;
		server_name <ip_N>;
		#charset utf-8;(Recommended Use)
		#Enable customized header
		underscores_in_headers on;
		location / {
			if ($http_x_host = '') {
				#Requests without customized headers can be sent to any node
				#Here, <nodes> needs to be consistent with the name after "upstream"
				proxy_pass http://<nodes>;
				break;
			}
		#For requests containing customized headers, forward them to the specified node
		proxy_pass http://$http_x_host:$http_x_port;break;
		}
	}
   ```

### MDX for Kylin 集群访问方式

   完成上述配置后，可以根据 Nginx 配置的 `server_name` 和 `listen` ， 页面上通过 `http://<ip_N>:<port_N>/login` 来对 MDX for Kylin 集群进行访问，Excel 等BI 工具通过 `http://<ip_N>:<port_N>/mdx/xmla/<project_name>` 来对 MDX for Kylin 集群上的某个名为 `<project_name>` 项目进行访问。

### 注意事项

   + 以上内容中 {} 或 <> 应当根据实际使用情形，连同 {} 或 <> 一同替换为实际值。

   + 多节点部署配置示例中 Nginx 配置为示例配置，具体的路由方式和过滤范围可根据实际需要进行调整。

   + 如果多个 MDX for Kylin 节点的版本不一致，Nginx 转发后对 `js` 和 `css` 文件的读取可能存在问题，所以您**必须保证各个节点的 MDX for Kylin 版本一致**。

   + 由于请求诊断包时使用配置文件中给出的 IP 和 Port 信息，请不要使用 localhost，可能会导致 Nginx 转发失败导致请求无法成功。
   
   + 在云上环境使用时，为了让 Nginx 能正常转发请求，请将所有 MDX 示例安装在同一虚拟子网内。
