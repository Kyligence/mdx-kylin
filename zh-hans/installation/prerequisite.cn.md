## 安装前置条件

为了保障系统的性能与稳定性，我们建议您将 MDX for Kylin 单独运行在一个独立的 Linux 服务器上。

下面我们将为您介绍 MDX for Kylin 安装的前置条件。

- [Kylin](#Kylin)
- [推荐的硬件配置](#推荐的硬件配置)
- [推荐的 Linux 版本](#推荐的-linux-版本)
- [需要的依赖](#需要的依赖)
- [推荐的元数据库版本](#推荐的元数据库版本)
- [推荐的元数据库驱动版本](#推荐的元数据库驱动版本)
- [推荐的客户端配置](#推荐的客户端配置)

### Kylin

MDX for Kylin 需要对接一个 Kylin 实例或集群，现在 MDX for Kylin 能对接 Kylin 版本为 4.0.2 及以上

如果有需要低于 Kylin 4.x 版本，请参考[链接](https://github.com/Kyligence/mdx-kylin/issues/1)

### 推荐的硬件配置

我们推荐您使用下述硬件配置安装 MDX for Kylin：

- 双路 Intel 至强处理器，6核（或8核）CPU，主频 2.3GHz 或以上
- 32GB ECC DDR3 以上
- 至少1个 1TB 的 SAS 硬盘（3.5寸），7200RPM，RAID1
- 至少两个 1GbE 的以太网电口

### 推荐的 Linux 版本

我们推荐您使用下述版本的 Linux 操作系统：

- Red Hat Enterprise 7.x
- CentOS 6.4+ 或 7.x
- Suse Linux 11
- Ubuntu 16

### 需要的依赖

- JAVA 环境：JDK8 或以上

### 推荐的元数据库版本

- MySQL 5.7.x 及以上

### 推荐的元数据库驱动 jar 版本

- mysql-connector-java-8.0.16, 请下载到 `<MDX for Kylin 安装目录>/semantic-mdx/lib/` 下或者替换所需的 mysql connector 版本到该路径下

### 推荐的客户端配置

- CPU：2.5 GHz Intel Core i7
- 操作系统：macOS / windows 7 或 10
- 内存：8G 或以上
- 浏览器及版本：
  - 谷歌Chrome 67.0.3396 及以上
