/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.niord.core.cache;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

/**
 * Base class for Infinispan caches
 */
public abstract class BaseCache {

    @Inject
    private Logger log;

    protected EmbeddedCacheManager cacheContainer;

    /**
     * Starts the cache container
     */
    @PostConstruct
    public void initCacheContainer() {
        if (cacheContainer == null) {
            GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder()
                    .nonClusteredDefault() //Helper method that gets you a default constructed GlobalConfiguration, preconfigured for use in LOCAL mode
                    .globalJmxStatistics().allowDuplicateDomains(true)
                    .build(); //Builds  the GlobalConfiguration object

            Configuration localConfiguration = createCacheConfiguration();

            cacheContainer = new DefaultCacheManager(globalConfiguration, localConfiguration, true);
            log.info("Init cache container");
        }
    }

    /**
     * Clears the cache
     */
    @SuppressWarnings("unused")
    public abstract void clearCache();

    /**
     * Must be implemented by sub-classes to define the local cache configuration
     * @return the local cache configuration
     */
    protected abstract Configuration createCacheConfiguration();

    /**
     * Stops the cache container
     */
    @PreDestroy
    public void destroyCacheContainer() {
        if (cacheContainer != null) {
            cacheContainer.stop();
            cacheContainer = null;
            log.info("Stopped cache container");
        }
    }
}
