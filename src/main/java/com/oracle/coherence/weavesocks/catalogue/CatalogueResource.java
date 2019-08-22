package com.oracle.coherence.weavesocks.catalogue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.tangosol.net.NamedCache;
import com.tangosol.util.Aggregators;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.comparator.ExtractorComparator;
import com.tangosol.util.extractor.UniversalExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.LimitFilter;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @author Aleksandar Seovic  2019.08.20
 */
@ApplicationScoped
@Path("/catalogue")
public class CatalogueResource {
    private static Comparator<Sock> PRICE_COMPARATOR = new ExtractorComparator<>(new UniversalExtractor<Sock, Float>("price"));
    private static Comparator<Sock> NAME_COMPARATOR  = new ExtractorComparator<>(new UniversalExtractor<Sock, String>("name"));

    @Inject
    private NamedCache<String, Sock> catalogue;

    @GET
    @Produces(APPLICATION_JSON)
    public Collection<Sock> getSocks(@QueryParam("tags") String tags,
                                     @QueryParam("order") @DefaultValue("price") String order,
                                     @QueryParam("page") @DefaultValue("1") int pageNum,
                                     @QueryParam("size") @DefaultValue("10") int pageSize) {

        LimitFilter<Sock> filter = new LimitFilter<>(createTagsFilter(tags), pageSize);
        Comparator<Sock> comparator = "price".equals(order)
                ? PRICE_COMPARATOR
                : "name".equals(order)
                        ? NAME_COMPARATOR
                        : null;
        filter.setComparator(comparator);
        filter.setPage(pageNum - 1);

        return catalogue.values(filter);
    }

    @GET
    @Path("size")
    @Produces(APPLICATION_JSON)
    public int getSockCount(@QueryParam("tags") String tags) {
        return catalogue.aggregate(createTagsFilter(tags), Aggregators.count());
    }

    @GET
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    public Sock getSock(@PathParam("id") String sockId) {
        return catalogue.get(sockId);
    }

    private Filter<Sock> createTagsFilter(String tags) {
        Filter<Sock> filter = AlwaysFilter.INSTANCE();
        if (tags != null) {
            String[] aTags = tags.split(",");
            if (aTags.length > 0) {
                filter = Filters.containsAny(Sock::getTag, aTags);
            }
        }
        return filter;
    }

    @PostConstruct
    public void loadData() {
        if (catalogue.isEmpty()) {
            Jsonb jsonb = JsonbBuilder.create();
            InputStream in = getClass().getClassLoader().getResourceAsStream("data.json");

            List<Sock> socks = jsonb.fromJson(
              in,
              new ArrayList<Sock>(){}.getClass().getGenericSuperclass()
            );

            Map<String, Sock> sockMap = socks.stream()
                    .collect(Collectors.toMap(Sock::getId, sock -> sock));
            catalogue.putAll(sockMap);
        }
    }
}
