/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 TONBELLER AG
// Copyright (C) 2006-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.rolap.cache;

import com.google.common.cache.Cache;
import mondrian.olap.MondrianCacheControl;
import mondrian.olap.MondrianProperties;
import mondrian.xmla.XmlaRequestContext;
import org.apache.commons.collections.map.ReferenceMap;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link SmartCacheImpl} which uses a
 * {@link ReferenceMap} as a backing object. Both the key
 * and the value are soft references, because of their
 * cyclic nature.
 *
 * <p>This class does not enforce any synchronization, because
 * this is handled by {@link SmartCacheImpl}.
 *
 * @author av, lboudreau
 * @since Nov 3, 2005
 */
public class SoftSmartCache<K, V> extends SmartCacheImpl<K, V> {

    private final Cache<K, V> cache = MondrianCacheControl.buildCache(
            MondrianProperties.instance().CacheExpireMinute.get(), TimeUnit.MINUTES, 100);

    public SoftSmartCache() {
        MondrianCacheControl.putCacheMap(
                XmlaRequestContext.getContext().currentProject,
                MondrianCacheControl.CacheType.SOFT_SMART_CACHE,
                cache,
                null);
    }

    public V putImpl(K key, V value) {
        // Null values are the same as a 'remove'
        // Convert the operation because ReferenceMap doesn't
        // like null values.
        if (key == null) {
            return null;
        }

        V oldVal = cache.getIfPresent(key);
        if (value == null) {
            cache.invalidate(key);
        }
        cache.put(key, value);
        return oldVal;
    }

    public V getImpl(K key) {
        return key == null ? null : cache.getIfPresent(key);
    }

    public V removeImpl(K key) {
        if (key == null) {
            return null;
        }
        V oldVal = cache.getIfPresent(key);
        cache.invalidate(key);
        return oldVal;
    }

    public void clearImpl() {
        cache.cleanUp();
    }

    public long sizeImpl() {
        return cache.size();
    }

    public Iterator<Map.Entry<K, V>> iteratorImpl() {
        return cache.asMap().entrySet().iterator();
    }
}

// End SoftSmartCache.java

