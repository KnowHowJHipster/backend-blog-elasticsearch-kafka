package org.iqkv.blog.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import java.util.stream.Stream;
import org.iqkv.blog.domain.Blog;
import org.iqkv.blog.repository.BlogRepository;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link Blog} entity.
 */
public interface BlogSearchRepository extends ElasticsearchRepository<Blog, Long>, BlogSearchRepositoryInternal {}

interface BlogSearchRepositoryInternal {
    Stream<Blog> search(String query);

    Stream<Blog> search(Query query);

    @Async
    void index(Blog entity);

    @Async
    void deleteFromIndexById(Long id);
}

class BlogSearchRepositoryInternalImpl implements BlogSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final BlogRepository repository;

    BlogSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, BlogRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<Blog> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<Blog> search(Query query) {
        return elasticsearchTemplate.search(query, Blog.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(Blog entity) {
        repository.findOneWithEagerRelationships(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), Blog.class);
    }
}
