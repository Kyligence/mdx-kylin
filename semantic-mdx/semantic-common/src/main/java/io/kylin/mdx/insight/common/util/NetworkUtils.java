package io.kylin.mdx.insight.common.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtils {

    private static final String LOCAL_IP = getLANIPAddress(getLocalIPList());

    /**
     * 获取本机 IP，优先取局域网 IP
     *
     * @return 本机 IP
     */
    public static String getLocalIP() {
        return LOCAL_IP;
    }

    /**
     * 获取局域网ip
     *
     * @return 局域网ip
     */
    public static String getLANIPAddress(List<String> ipList) {
        for (String anIp : ipList) {
            if (anIp.startsWith("10.")) {
                return anIp;
            }
        }
        for (String anIp : ipList) {
            if (anIp.startsWith("172.")) {
                IpAddress ip = new IpAddress(anIp);
                if (ip.getB() >= 16 && ip.getB() <= 31) {
                    return anIp;
                }
            }
        }
        for (String anIp : ipList) {
            if (anIp.startsWith("192.168.")) {
                return anIp;
            }
        }
        if (ipList.size() >= 1) {
            return ipList.get(0);
        } else {
            return "127.0.0.1";
        }
    }

    public static List<String> getLocalIPList() {
        ArrayList<String> ipList = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    ip = addresses.nextElement();
                    if (ip != null && ip.getAddress().length == 4) {
                        ipList.add(ip.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return ipList;
    }

    public static class IpAddress {
        private final int a;
        private final int b;
        private final int c;
        private final int d;

        IpAddress(String ip) {
            String[] li = ip.split("\\.");
            if (li.length == 4) {
                a = Integer.parseInt(li[0]);
                b = Integer.parseInt(li[1]);
                c = Integer.parseInt(li[2]);
                d = Integer.parseInt(li[3]);
            } else {
                throw new IllegalArgumentException();
            }
        }

        int getA() {
            return a;
        }

        int getB() {
            return b;
        }

        int getC() {
            return c;
        }

        int getD() {
            return d;
        }

    }

}

