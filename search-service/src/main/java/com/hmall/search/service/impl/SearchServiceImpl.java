package com.hmall.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.mapper.SearchMapper;
import com.hmall.search.service.ISearchService;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class SearchServiceImpl extends ServiceImpl<SearchMapper, Item> implements ISearchService {
    private final RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(HttpHost.create("http://192.168.100.128:9200"))
    );
    @Override
    public Page<ItemDoc> esSearch(ItemPageQuery query) {
        SearchRequest request = new SearchRequest("items");
        BoolQueryBuilder bool = QueryBuilders.boolQuery();

        if (StrUtil.isNotBlank(query.getKey())) {
            bool.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        if (StrUtil.isNotBlank(query.getCategory())) {
            bool.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        if (StrUtil.isNotBlank(query.getBrand())) {
            bool.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        if (query.getMinPrice() != null) {
            bool.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
        }
        if (query.getMaxPrice() != null) {
            bool.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
        }
        if (StrUtil.isNotBlank(query.getSortBy())) {
            request.source().sort(query.getSortBy(), query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC);
        }
        if (query.getPageNo() != null && query.getPageSize() != null) {
            request.source().from((query.getPageNo() - 1) * query.getPageSize()).size(query.getPageSize());
        }

        request.source().query(bool);

        SearchResponse search = null;
        try {
            search = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (search == null)  {
            log.error("查询结果为空！");
            return null;
        }

        Page<ItemDoc> ans = new Page<>();
        ans.setRecords(new java.util.ArrayList<>());
        SearchHits hits = search.getHits();
        if (hits.getTotalHits() != null) {
            ans.setTotal(hits.getTotalHits().value);
        }
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit hit : hits1) {
            ItemDoc bean = JSONUtil.toBean(hit.getSourceAsString(), ItemDoc.class);
            ans.getRecords().add(bean);
        }
        return ans;
    }

    @Override
    public Map<String, List<String>> esAggregate(ItemPageQuery query) {
        Map<String, List<String>> map = new HashMap<>();
        if (StrUtil.isBlank(query.getKey())) {
            map.put("category", new ArrayList<>(Arrays.asList("手机", "拉杆箱", "休闲鞋", "硬盘", "真皮包")));
            map.put("brand", new ArrayList<>(Arrays.asList("希捷", "小米", "华为", "OPPO", "尤妮佳", "OPPO", "Apple", "锤子")));
            return map;
        }
        map.put("category", new ArrayList<>());
        map.put("brand", new ArrayList<>());

        SearchRequest request = new SearchRequest("items");
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        if (StrUtil.isNotBlank(query.getKey())) {
            bool.filter(QueryBuilders.matchQuery("name", query.getKey()));
        }
        if (StrUtil.isNotBlank(query.getCategory())) {
            bool.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        if (StrUtil.isNotBlank(query.getBrand())) {
            bool.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        if (query.getMinPrice() != null) {
            bool.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
        }
        if (query.getMaxPrice() != null) {
            bool.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
        }
        request.source().size(0);
        request.source().query(bool);
        request.source().aggregation(AggregationBuilders.terms("category_agg").field("category").size(3));
        request.source().aggregation(AggregationBuilders.terms("brand_agg").field("brand").size(3));

        SearchResponse search = null;

        try {
            search = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (search == null) {
            log.error("查询结果为空！");
            return null;
        }
        Aggregations aggregations = search.getAggregations();
        Terms terms = aggregations.get("category_agg");
        for (Terms.Bucket bucket : terms.getBuckets()) {
            map.get("category").add(bucket.getKeyAsString());
        }
        terms = aggregations.get("brand_agg");
        for (Terms.Bucket bucket : terms.getBuckets()) {
            map.get("brand").add(bucket.getKeyAsString());
        }
        return map;
    }
}
