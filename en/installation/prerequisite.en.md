## Installation Prerequisites

To ensure the performance and robustness of the system, we recommend you to run MDX for Kylin in a single Linux server.

The installation prerequisites of MDX for Kylin are listed below.

- [Kylin](#kyln)
- [Recommended Hardware Configuration](#recommended-hardware-configuration)
- [Recommended Linux Version](#recommended-linux-version)
- [Required dependencies](#required-dependencies)
- [Recommended Database Version](#recommended-database-version)
- [Recommended MySQL JDBC Version](#recommended-mysql-jdbc-version)
- [Recommended browser and driver version](#recommended-browser-and-driver-version)
- [Recommended Client Configuration](#recommended-client-configuration)

### Kylin

MDX for Kylin requires a Kylin instance or cluster. MDX for Kylin can only connect with Kylin 4.0.2 and upon.

If user want to use the MDX for Kylin before Kylin 4.x, please check the [reference](https://github.com/Kyligence/mdx-kylin/issues/1)

### Recommended Hardware Configuration

We recommend you to install MDX for Kylin in the following hardware configuration:

- Dual Intel Xeon Processor, 6 core (or 8 core) CPU - 2.3GHz or above
- 32GB ECC DDR3 or above
- At least one 1TB SAS HDD (3.5 inches), 7200RPM, RAID1
- At least two 1GbE Ethernet ports

### Recommended Linux Version

We recommend you to install MDX for Kylin in the following Linux operation systems:

- Red Hat Enterprise 7.x
- CentOS 6.4+ / CentOS 7.x
- Suse Linux 11
- Ubuntu 16

### Required dependencies

- Java Environment: JDK8 or above

### Recommended Database Version

- MySQL 5.7.x

### Recommended MySQL JDBC Version

- mysql-connector-java-8.0.16, please download to and if necessary, you can change version in `<MDX installation directory>/semantic-mdx/lib/`

### Recommended browser and driver version

The chart screenshot export feature needs you to install a browser and its compatible driver, the browser should run correctly under the headless mode. We recommend you to use one of the following browsers versions, **download its compatible driver and put the driver executable file in your PATH**. 

- Firefox version 57 or above
  > - CentOS 6, CentOS 7: `sudo yum install firefox`
  > - Ubuntu 16: `sudo apt install firefox`
  > - Firefox uses geckodriver, you can download it from [GitHub](https://github.com/mozilla/geckodriver/releases), the version compatibility between geckodriver and Firefox can be found at [Mozilla](https://firefox-source-docs.mozilla.org/testing/geckodriver/Support.html)
  > - You can try executing `firefox --headless` to check whether Firefox runs correctly, if you get a dbus connection error, you should start a dbus-daemon first:
  >   1. CentOS: `sudo yum install dbus-x11`, Ubuntu: `sudo apt install dbus-x11` (install dbus-x11)
  >   2. `sudo dbus-uuidgen --ensure` (generate machine-id)
  >   3. `export $(dbus-launch)` (run a dbus-daemon and set its environment variables)

- Chrome or Chromium version 67 or above
  > - CentOS 6: unsupported
  > - CentOS 7: `sudo yum install chromium` (need epel repository)
  > - Ubuntu 16: `sudo apt install chromium-browser`
  > - Chrome/Chromium uses chromedriver, you can download it from [Google](https://sites.google.com/a/chromium.org/chromedriver/downloads) or [Mirror](https://npm.taobao.org/mirrors/chromedriver), the version compatibility between chromedriver and Chrome/Chromium can be found at [Google](https://sites.google.com/a/chromium.org/chromedriver/downloads)

###  Recommended Client Configuration

- CPU: 2.5 GHz Intel Core i7
- Operating System: macOS / windows 7 / windows 10
- RAM: 8G or above
- Browser version:
  - Chrome 67.0.3396 or above
