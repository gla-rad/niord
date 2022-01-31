///*
// * Copyright 2016 Danish Maritime Authority.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.niord.core.conf;
//
//import javax.enterprise.context.ApplicationScoped;
//import javax.enterprise.context.control.ActivateRequestContext;
//import javax.enterprise.inject.Produces;
//import javax.enterprise.inject.Typed;
//import javax.persistence.EntityManager;
//import javax.persistence.EntityManagerFactory;
//import javax.persistence.PersistenceContext;
//
///**
// * Produces the niord entity manager, thus available for CDI injection.
// */
//@Typed(EntityManagerFactory.class)
//@SuppressWarnings("unused")
//public class DatabaseConfiguration {
//
//    @PersistenceContext(name = "niord")
//    EntityManager entityManager;
//
//    @ApplicationScoped
//    @Produces
//    public EntityManager getEntityManager() {
//        return entityManager;
//    }
//
//}
