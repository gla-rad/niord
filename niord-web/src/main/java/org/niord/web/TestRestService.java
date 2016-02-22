package org.niord.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.security.annotation.SecurityDomain;
import org.niord.core.model.Chart;
import org.niord.core.model.Extent;
import org.niord.core.model.FeatureCollection;
import org.niord.core.service.ChartService;
import org.niord.core.service.FeatureService;
import org.niord.model.IJsonSerializable;
import org.niord.model.ILocalizable;
import org.niord.model.ILocalizedDesc;
import org.niord.model.vo.ChartVo;
import org.niord.model.vo.geojson.FeatureCollectionVo;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test.
 */
@Path("/test")
@Singleton
@Startup
@SecurityDomain("keycloak")
@PermitAll
public class TestRestService {

    @Context
    HttpServletRequest request;

    @Context
    HttpServletResponse response;

    @Inject
    FeatureService featureService;

    @Inject
    ChartService chartService;

    List<Area> areas;
    List<Category> categories;

    @GET
    @Path("/shapes")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public List<FeatureCollectionVo> getAllFeatureCollections() throws Exception {
        return featureService.loadAllFeatureCollections().stream()
                .map(FeatureCollection::toGeoJson)
                .collect(Collectors.toList());
    }

    @POST
    @Path("/shapes")
    @Consumes("application/json;charset=UTF-8")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public FeatureCollectionVo createFeatureCollection(FeatureCollectionVo fc) throws Exception {
        return featureService.createFeatureCollection(FeatureCollection.fromGeoJson(fc))
                .toGeoJson();
    }

    @PUT
    @Path("/shapes")
    @Consumes("application/json;charset=UTF-8")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public FeatureCollectionVo updateFeatureCollection(FeatureCollectionVo fc) throws Exception {
        return featureService.updateFeatureCollection(FeatureCollection.fromGeoJson(fc))
                .toGeoJson();
    }

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Load areas
            List<Area> areasDa = mapper.readValue(getClass().getResource("/areas_da.json"), new TypeReference<List<Area>>(){});
            List<Area> areasEn = mapper.readValue(getClass().getResource("/areas_en.json"), new TypeReference<List<Area>>(){});
            Map<Integer, Area> areasLookupDa = new HashMap<>();
            Map<Integer, Area> areasLookupEn = new HashMap<>();
            updateAreaLookup(areasLookupDa, areasDa);
            updateAreaLookup(areasLookupEn, areasEn);
            areasLookupDa.values().forEach(a -> {
                if (areasLookupEn.containsKey(a.getId())) {
                    a.getDescs().add(areasLookupEn.get(a.getId()).getDescs().get(0));
                }
            });
            areas = new ArrayList<>(areasLookupDa.values());
            System.out.println("**** LOADED " + areas.size() + " areas");

            // Load categories
            List<Category> categoriesDa = mapper.readValue(getClass().getResource("/categories_da.json"), new TypeReference<List<Category>>(){});
            List<Category> categoriesEn = mapper.readValue(getClass().getResource("/categories_en.json"), new TypeReference<List<Category>>(){});
            Map<Integer, Category> categoriesLookupDa = new HashMap<>();
            Map<Integer, Category> categoriesLookupEn = new HashMap<>();
            updateCategoryLookup(categoriesLookupDa, categoriesDa);
            updateCategoryLookup(categoriesLookupEn, categoriesEn);
            categoriesLookupDa.values().forEach(a -> {
                if (categoriesLookupEn.containsKey(a.getId())) {
                    a.getDescs().add(categoriesLookupEn.get(a.getId()).getDescs().get(0));
                }
            });
            categories = new ArrayList<>(categoriesLookupDa.values());
            System.out.println("**** LOADED " + categories.size() + " categories");

        } catch (IOException e) {
            // Prevent start-up
            throw new RuntimeException(e);
        }
    }

    public void updateAreaLookup(Map<Integer, Area> lookup, List<Area> areas) {
        if (areas != null) {
            areas.forEach(a -> {
                lookup.put(a.getId(), a);
                if (a.getChildren() != null) {
                    a.getChildren().forEach(c -> c.setParent(a));
                }
                updateAreaLookup(lookup, a.getChildren());
            });
        }
    }

    public void updateCategoryLookup(Map<Integer, Category> lookup, List<Category> categories) {
        if (categories != null) {
            categories.forEach(a -> {
                lookup.put(a.getId(), a);
                if (a.getChildren() != null) {
                    a.getChildren().forEach(c -> c.setParent(a));
                }
                updateCategoryLookup(lookup, a.getChildren());
            });
        }
    }

    @GET
    @Path("/xxx")
    @Produces("application/json;charset=UTF-8")
    @NoCache
    @RolesAllowed("clientuser")
    public String test() {
        return System.currentTimeMillis() + "";
    }


    @GET
    @Path("/charts")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public List<ChartVo> searchCharts(@QueryParam("name") @DefaultValue("") String name,
                                      @QueryParam("limit") @DefaultValue("1000") int limit) {
        return chartService.searchCharts(name, limit).stream()
                .limit(limit)
                .map(Chart::toVo)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/charts/{chartIds}")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public List<ChartVo> getCharts(@PathParam("chartIds") String chartIds,
                                 @QueryParam("limit") @DefaultValue("1000") int limit) {
        return chartService.findByChartNumbers(chartIds.split(",")).stream()
                .limit(limit)
                .map(Chart::toVo)
                .collect(Collectors.toList());
    }

    public List<Extent> getChartExtents(String[] chartIds) {

        return null;
        /**
        if (chartIds == null) {
            return null;
        }
        List<Extent> result = new ArrayList<>();
        Arrays.stream(chartIds)
                .forEach(c -> {
                    ChartVo chart = searchCharts(c, 1).get(0);
                    if (chart.getLowerLeftLatitude() != null) {
                        result.addAll(new Extent(
                                chart.getLowerLeftLatitude(),
                                chart.getLowerLeftLongitude(),
                                chart.getUpperRightLatitude(),
                                chart.getUpperRightLongitude()).normalize());
                    }
                });
        return result;
         */
    }

    @GET
    @Path("/areas")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public List<Area> searchAreas(@QueryParam("lang") @DefaultValue("en") String lang,
                                  @QueryParam("name") @DefaultValue("") String name,
                                  @QueryParam("limit") @DefaultValue("1000") int limit) {
        return areas.stream()
                .filter(a -> name.isEmpty()
                        || (a.getDesc(lang) != null && a.getDesc(lang).getName().toLowerCase().contains(name.toLowerCase())))
                .sorted((a1, a2) -> a1.getDesc(lang).getName().toLowerCase().compareTo(a2.getDesc(lang).getName().toLowerCase()))
                .limit(limit)
                .map(a -> a.parentCopy(lang))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/areas/{areaIds}")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public List<Area> getAreas(@PathParam("areaIds") String areaIds,
                               @QueryParam("lang") @DefaultValue("en") String lang,
                               @QueryParam("limit") @DefaultValue("1000") int limit) {
        Set<String> ids = new HashSet<>(Arrays.asList(areaIds.split(",")));
        return areas.stream()
                .filter(a -> ids.contains(a.getId().toString()))
                .limit(limit)
                .map(a -> a.parentCopy(lang))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/categories")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public List<Category> searchCategories(@QueryParam("lang") @DefaultValue("en") String lang,
                                  @QueryParam("name") @DefaultValue("") String name,
                                  @QueryParam("limit") @DefaultValue("1000") int limit) {
        return categories.stream()
                .filter(a -> name.isEmpty()
                        || (a.getDesc(lang) != null && a.getDesc(lang).getName().toLowerCase().contains(name.toLowerCase())))
                .sorted((a1, a2) -> a1.getDesc(lang).getName().toLowerCase().compareTo(a2.getDesc(lang).getName().toLowerCase()))
                .limit(limit)
                .map(a -> a.parentCopy(lang))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/categories/{categoryIds}")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public List<Category> getCategories(@PathParam("categoryIds") String categoryIds,
                               @QueryParam("lang") @DefaultValue("en") String lang,
                               @QueryParam("limit") @DefaultValue("1000") int limit) {
        Set<String> ids = new HashSet<>(Arrays.asList(categoryIds.split(",")));
        return categories.stream()
                .filter(a -> ids.contains(a.getId().toString()))
                .limit(limit)
                .map(a -> a.parentCopy(lang))
                .collect(Collectors.toList());
    }



    public abstract static class HierarchicalData<T,TD extends ILocalizedDesc> implements ILocalizable<TD>, IJsonSerializable {
        Integer id;
        T parent;
        List<T> children;
        List<TD> descs;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public T getParent() {
            return parent;
        }

        public void setParent(T parent) {
            this.parent = parent;
        }

        public List<T> getChildren() {
            return children;
        }

        public void setChildren(List<T> children) {
            this.children = children;
        }

        @Override
        public List<TD> getDescs() {
            return descs;
        }

        @Override
        public void setDescs(List<TD> descs) {
            this.descs = descs;
        }
    }

    public static class AreaDesc implements ILocalizedDesc, IJsonSerializable {
        String lang;
        String name;

        @Override
        public String getLang() {
            return lang;
        }

        @Override
        public void setLang(String lang) {
            this.lang = lang;
        }

        @Override
        public boolean descDefined() {
            return ILocalizedDesc.fieldsDefined(name);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Area extends HierarchicalData<Area, AreaDesc> {
        double sortOrder;

        public AreaDesc getDesc(String lang) {
            return descs.stream()
                    .filter(d -> d.getLang().equals(lang))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public AreaDesc createDesc(String lang) {
            AreaDesc desc = new AreaDesc();
            desc.setLang(lang);
            return desc;
        }

        /**
         * Creates a copy of this area including parent areas but excluding child areas
         * @return a parent-lineage copy of this area
         */
        public Area parentCopy(String lang) {
            Area area = new Area();
            area.setId(id);
            area.setSortOrder(sortOrder);
            if (descs != null) {
                area.setDescs(new ArrayList<>(descs));
                area.sortDescs(lang);
            }
            if (parent != null) {
                area.setParent(parent.parentCopy(lang));
            }
            return area;
        }

        public double getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(double sortOrder) {
            this.sortOrder = sortOrder;
        }
    }

    public static class CategoryDesc implements ILocalizedDesc, IJsonSerializable {
        String lang;
        String name;

        @Override
        public String getLang() {
            return lang;
        }

        @Override
        public void setLang(String lang) {
            this.lang = lang;
        }

        @Override
        public boolean descDefined() {
            return ILocalizedDesc.fieldsDefined(name);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Category extends HierarchicalData<Category, CategoryDesc> {
        double sortOrder;

        public CategoryDesc getDesc(String lang) {
            return descs.stream()
                    .filter(d -> d.getLang().equals(lang))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public CategoryDesc createDesc(String lang) {
            CategoryDesc desc = new CategoryDesc();
            desc.setLang(lang);
            return desc;
        }

        /**
         * Creates a copy of this category including parent categories but excluding child categories
         * @return a parent-lineage copy of this category
         */
        public Category parentCopy(String lang) {
            Category category = new Category();
            category.setId(id);
            category.setSortOrder(sortOrder);
            if (descs != null) {
                category.setDescs(new ArrayList<>(descs));
                category.sortDescs(lang);
            }
            if (parent != null) {
                category.setParent(parent.parentCopy(lang));
            }
            return category;
        }

        public double getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(double sortOrder) {
            this.sortOrder = sortOrder;
        }
    }

}
