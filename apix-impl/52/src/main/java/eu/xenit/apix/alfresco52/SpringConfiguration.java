package eu.xenit.apix.alfresco52;

import eu.xenit.apix.alfresco.search.SearchFacetsService;
import eu.xenit.apix.translation.ITranslationService;
import org.alfresco.repo.search.impl.solr.facet.SolrFacetService;
import org.alfresco.service.ServiceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SpringConfiguration {

    @Autowired
    ServiceRegistry serviceRegistry;

    @Autowired
    SolrFacetService solrFacetService;

    @Autowired
    private ITranslationService translationService;

    public SearchFacetsService eu_xenit_apix_search_searchFacetsService52Apix() {
        return new SearchFacetsServiceImpl52(serviceRegistry, solrFacetService, translationService);
    }

    @Bean(name = "eu.xenit.apix.search.SearchFacetsService52")
    public SearchFacetsService searchFacetsServiceApix() {
        return new SearchFacetsServiceImpl52(serviceRegistry, solrFacetService, translationService);
    }
}
