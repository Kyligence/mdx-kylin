## MDX cluster installation

- [MDX instances installation & configurations](#mdx-instances-installation--configurations)
- [Nginx installation & configurations](#nginx-installation--configurations)
- [How to access MDX cluster](#how-to-access-mdx-cluster)
- [Notes](#notes)

### MDX instances installation & configurations

   After downloading the MDX package from the website, please follow the guidance [Install on Linux](install_linux.en.md) in the user mannual to install and configure MDX instances. 
   
   To ensure that diagnosis package generation can perform normally in cluster mode, IP and Port information of ALL MDX for Kylin instances are needed in the property file `${MDX_HOME}/conf/insight.properties` in the form as `insight.mdx.cluster.nodes=<ip_A>:<port_A>,<ip_B>:<port_B>` for each MDX instance.

### Nginx installation & configurations

   For installation of Nginx, please refer to the documents on the website of Nginx. MDX utilizes customized  `header`:`x-Host` and `x-Port` to route requests to specified nodes in order to generate diagnosis packages on web. Thus, route rules are required in the Nginx property file. For example, based on **Nginx 1.13.7**, for two MDX for Kylin nodes with IP and Port as `<ip_A>:<port_A>` and `<ip_B>:<port_B>`, following configuration is required:

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

### How to access MDX cluster

   After finishing above Nginx configuration, you may access the MDX cluster by the URL `http://<ip_N>:<port_N>/login` which is determined by the configuration  `server_name` and `listen` for Nginx. For BI software such Excel, you may access the project named `<project_name>` on the MDX cluster by the URL `http://<ip_N>:<port_N>/mdx/xmla/<project_name>`.

### Notes

   + Values surrounded by {} or <> in content above should be replaced (including {} or <>) with actual value accordingly.

   + Nginx configuration above is a typical one. The route rules should be adjusted according to actual needs.

   + Nginx may have problem accessing `js` and `css` file when the versions of different MDX instances are not the same version. So please ensure that **versions of all MDX for Kylin instances are the same**.

   + Please do not use localhost for the IP information in the property file as this may cause Nginx forward failure.

   + For enviroments on cloud, make sure that all MDX instances are installed under the same virtual subnet.
