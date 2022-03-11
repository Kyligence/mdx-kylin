/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2011-2012 Pentaho and others
// All Rights Reserved.
*/
package mondrian.rolap.cache;

import com.google.common.cache.Cache;
import mondrian.olap.MondrianCacheControl;
import mondrian.olap.MondrianProperties;
import mondrian.spi.*;
import mondrian.xmla.XmlaRequestContext;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link mondrian.spi.SegmentCache} that stores segments
 * in memory.
 *
 * <p>Segments are held via soft references, so the garbage collector can remove
 * them if it sees fit.</p>
 *
 * <p>Not thread safe.</p>
 *
 * @author Julian Hyde
 */
public class MemorySegmentCache implements SegmentCache {

    private final Cache<SegmentHeader, SegmentBody> cache = MondrianCacheControl.buildCache(
            MondrianProperties.instance().CacheExpireMinute.get(), TimeUnit.MINUTES, 100);

    private final List<SegmentCacheListener> listeners =
        new CopyOnWriteArrayList<SegmentCacheListener>();

    public MemorySegmentCache(){
        MondrianCacheControl.putCacheMap(XmlaRequestContext.getContext().currentProject, MondrianCacheControl.CacheType.MEMORY_SEGMENT_CACHE, cache, null);
    }

    @Override
    public SegmentBody get(SegmentHeader header) {
        return cache.getIfPresent(header);
    }

    @Override
    public boolean contains(SegmentHeader header) {
        return get(header) != null;
    }

    @Override
    public List<SegmentHeader> getSegmentHeaders() {
        return new ArrayList<>(cache.asMap().keySet());
    }

    @Override
    public boolean put(final SegmentHeader header, SegmentBody body) {
        // REVIEW: What's the difference between returning false
        // and throwing an exception?
        if (body == null) {
            return true;
        }
        cache.put(header, body);
        fireSegmentCacheEvent(
            new SegmentCache.SegmentCacheListener.SegmentCacheEvent() {
                @Override
                public boolean isLocal() {
                    return true;
                }

                @Override
                public SegmentHeader getSource() {
                    return header;
                }

                @Override
                public EventType getEventType() {
                    return SegmentCacheListener.SegmentCacheEvent
                        .EventType.ENTRY_CREATED;
                }
            });
        return true; // success
    }

    @Override
    public boolean remove(final SegmentHeader header) {
        final boolean result = cache.getIfPresent(header) != null;
        cache.invalidate(header);
        if (result) {
            fireSegmentCacheEvent(
                new SegmentCache.SegmentCacheListener.SegmentCacheEvent() {
                    @Override
                    public boolean isLocal() {
                        return true;
                    }

                    @Override
                    public SegmentHeader getSource() {
                        return header;
                    }

                    @Override
                    public EventType getEventType() {
                        return
                            SegmentCacheListener.SegmentCacheEvent
                                .EventType.ENTRY_DELETED;
                    }
                });
        }
        return result;
    }

    @Override
    public void tearDown() {
        cache.cleanUp();
        listeners.clear();
    }

    @Override
    public void addListener(SegmentCacheListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(SegmentCacheListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean supportsRichIndex() {
        return true;
    }

    public void fireSegmentCacheEvent(
        SegmentCache.SegmentCacheListener.SegmentCacheEvent evt)
    {
        for (SegmentCacheListener listener : listeners) {
            listener.handle(evt);
        }
    }
}

// End MemorySegmentCache.java
