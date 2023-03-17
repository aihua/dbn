package com.dci.intellij.dbn.common.ref;

import com.intellij.util.containers.ContainerUtil;

import java.util.Map;

class WeakRefCacheKeyValueImpl<K, V> extends WeakRefCacheBase<K, V>  {

    @Override
    protected Map<K, V> createCache() {
        return ContainerUtil.createConcurrentWeakKeyWeakValueMap();
    }

}
