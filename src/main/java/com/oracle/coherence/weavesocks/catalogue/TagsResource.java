package com.oracle.coherence.weavesocks.catalogue;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.tangosol.net.NamedCache;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.stream.RemoteCollectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@ApplicationScoped
@Path("/tags")
public class TagsResource {

    @Inject
    private NamedCache<String, Sock> catalogue;

    @GET
    public Tags getTags() {
        ValueExtractor<Sock, Set<String>> extractor = Sock::getTag;

        Set<String> tags = catalogue.stream()
                .map(entry -> entry.extract(extractor))
                .flatMap(Collection::stream)
                .collect(RemoteCollectors.toSet());

        return new Tags(tags);
    }

    public static class Tags {
        public Set<String> tags;
        public Object err;

        Tags(Set<String> tags) {
            this.tags = tags;
        }
    }
}
