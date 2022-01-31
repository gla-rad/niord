/*
 * Copyright 2016 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.niord.core.db;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.niord.core.aton.AtonNode;
import org.niord.core.service.BaseService;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

/**
 * Launches the Hibernate Search index
 */
@ApplicationScoped
@Startup
@SuppressWarnings("unused")
public class HibernateSearchIndexService extends BaseService {

    @Inject
    private Logger log;

    @Resource
    TimerService timerService;

    @Inject
    EntityManager entityManager;

    /** Called upon application startup */
    @PostConstruct
    public void init() {
        // In order not to stall webapp deployment, wait 2 seconds starting the search index
        timerService.createSingleActionTimer(2000, new TimerConfig());
    }

    /**
     * Creates the full text indexes
     */
    @Timeout
    private void generateFullTextIndexes() {
        log.info("Start Hibernate Search indexer");

        long t0 = System.currentTimeMillis();
        SearchSession searchSession = Search.session(entityManager);
        // Create a mass indexer
        MassIndexer indexer = searchSession.massIndexer( AtonNode.class )
                .threadsToLoadObjects( 7 );

        // And perform the indexing
        try {
            indexer.startAndWait();
        } catch (InterruptedException | SearchException e) {
            log.error("Error indexing AtoNs", e);
        }
    }
}
