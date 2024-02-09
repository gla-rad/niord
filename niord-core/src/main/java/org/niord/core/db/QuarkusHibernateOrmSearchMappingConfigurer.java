/*
 * Copyright (c) 2023 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.niord.core.db;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import jakarta.enterprise.context.ApplicationScoped;


/**
 * The Quarkus Hibernate ORM Search Mapping Configurer Class.
 *
 * This class ensures that the Jandex Indexes don't get used at runtime,
 * pretty much in the same way the elasticsearch plugin does it as well.
 * <p/>
 * For more info check:
 * <a href="https://github.com/quarkusio/quarkus/issues/37772">here</a>
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@ApplicationScoped
public class QuarkusHibernateOrmSearchMappingConfigurer implements HibernateOrmSearchMappingConfigurer {

    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {
        // Jandex is not available at runtime in Quarkus,
        // so Hibernate Search cannot perform classpath scanning on startup.
        context.annotationMapping()
                .discoverJandexIndexesFromAddedTypes(false)
                .buildMissingDiscoveredJandexIndexes(false);
    }

}