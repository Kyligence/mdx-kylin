package io.kylin.mdx.rolap.cache;

public interface MondrianCache {

    void setCacheEnabled(boolean cacheEnabled);

    void expireAll();

}
