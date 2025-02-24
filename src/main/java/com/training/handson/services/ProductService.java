package com.training.handson.services;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product_search.ProductPagedSearchResponse;
import com.commercetools.api.models.product_search.ProductSearchFacetDistinctExpressionBuilder;
import com.commercetools.api.models.product_search.ProductSearchFacetDistinctValueBuilder;
import com.commercetools.api.models.product_search.ProductSearchFacetExpression;
import com.commercetools.api.models.product_search.ProductSearchProjectionParams;
import com.commercetools.api.models.product_search.ProductSearchProjectionParamsBuilder;
import com.commercetools.api.models.product_search.ProductSearchRequestBuilder;
import com.commercetools.api.models.search.SearchAndExpressionBuilder;
import com.commercetools.api.models.search.SearchExactExpressionBuilder;
import com.commercetools.api.models.search.SearchExactValueBuilder;
import com.commercetools.api.models.search.SearchFieldType;
import com.commercetools.api.models.search.SearchFullTextExpressionBuilder;
import com.commercetools.api.models.search.SearchFullTextValueBuilder;
import com.commercetools.api.models.search.SearchMatchType;
import com.commercetools.api.models.search.SearchQuery;
import com.commercetools.api.models.search.SearchSortMode;
import com.commercetools.api.models.search.SearchSortOrder;
import io.vrap.rmf.base.client.ApiHttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ProductService {

    private final ProjectApiRoot apiRoot;

    public ProductService(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    public CompletableFuture<ApiHttpResponse<ProductPagedSearchResponse>> getProducts(
            String keyword,
            String storeKey,
            Boolean includeFacets) {

        ProductSearchRequestBuilder builder = ProductSearchRequestBuilder.of()
                .withSort(ssb -> ssb
                        .field("variants.prices.centAmount")
                        .mode(SearchSortMode.MIN)
                        .order(SearchSortOrder.ASC)
                )
                .productProjectionParameters(pppb -> pppb
                        .priceCountry("US")
                        .priceCurrency("USD"))
                .markMatchingVariants(true);

        if (includeFacets != null && includeFacets) {
            builder.facets(createFacets());
        }

        // Check if at least one is provided, store and/or a keyword
        if (StringUtils.isNotEmpty(keyword) || StringUtils.isNotEmpty(storeKey)) {

            // Check if both are provided, store and a keyword
            if (StringUtils.isNotEmpty(keyword) && StringUtils.isNotEmpty(storeKey)) {

                final String storeId = getStoreId(storeKey);
                builder.query(createSearchQuery(keyword, storeId))
                        .productProjectionParameters(createProductProjectionParams(storeKey));

            } else if (StringUtils.isNotEmpty(keyword)) { // Check if only keyword is provided

                builder.query(createFullTextQuery(keyword));

            } else if (StringUtils.isNotEmpty(storeKey)) { // Check if only store is provided
                final String storeId = getStoreId(storeKey);
                builder.query(createStoreQuery(storeId))
                        .productProjectionParameters(createProductProjectionParams(storeKey));
            }
        }

        return apiRoot
                .products()
                .search()
                .post(builder.build())
                .execute();
    }

    private List<ProductSearchFacetExpression> createFacets() {
        return List.of(
                ProductSearchFacetDistinctExpressionBuilder
                        .of()
                        .distinct(
                                ProductSearchFacetDistinctValueBuilder.of()
                                        .name("color")
                                        .language("en-US")
                                        .field("variants.attributes.color")
                                        .fieldType(SearchFieldType.LTEXT)
                                        .build()
                        )
                        .build());
    }

    private String getStoreId(String storeKey) {
        return apiRoot.stores().withKey(storeKey).get().executeBlocking().getBody().getId();
    }

    private SearchQuery createSearchQuery(String keyword, String storeId) {
        return SearchAndExpressionBuilder.of()
                .and(Arrays.asList(
                        createFullTextQuery(keyword),
                        createStoreQuery(storeId)
                ))
                .build();
    }

    private SearchQuery createFullTextQuery(String keyword) {
        return SearchFullTextExpressionBuilder.of()
                .fullText(SearchFullTextValueBuilder.of()
                        .fieldType(SearchFieldType.LTEXT)
                        .field("name")
                        .language("en-US")
                        .mustMatch(SearchMatchType.ALL)
                        .value(keyword)
                        .build())
                .build();
    }

    private SearchQuery createStoreQuery(String storeId) {
        return SearchExactExpressionBuilder.of().exact(
                        SearchExactValueBuilder.of()
                                .field("stores")
                                .value(storeId)
                                .fieldType(SearchFieldType.SET_REFERENCE)
                                .build()
                )
                .build();
    }

    private ProductSearchProjectionParams createProductProjectionParams(String storeKey) {
        return ProductSearchProjectionParamsBuilder.of()
                .priceCountry("US")
                .priceCurrency("USD")
                .storeProjection(storeKey)
                .build();
    }

    public CompletableFuture<ApiHttpResponse<ProductProjection>> getProductByKey(String productKey) {
        return apiRoot
                .productProjections()
                .withKey(productKey)
                .get()
                .execute();
    }

}
