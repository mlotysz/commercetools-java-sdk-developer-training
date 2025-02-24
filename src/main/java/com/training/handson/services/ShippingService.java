package com.training.handson.services;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.shipping_method.ShippingMethod;
import com.commercetools.api.models.shipping_method.ShippingMethodPagedQueryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ShippingService {

    private final ProjectApiRoot apiRoot;

    private final String storeKey;

    public ShippingService(ProjectApiRoot apiRoot, String storeKey) {
        this.apiRoot = apiRoot;
        this.storeKey = storeKey;
    }

    public CompletableFuture<ApiHttpResponse<ShippingMethodPagedQueryResponse>> getShippingMethods() {

        return apiRoot
                .shippingMethods()
                .get()
                .withExpand("zoneRates[*].zone")
                .execute();
    }

    public CompletableFuture<ApiHttpResponse<ShippingMethod>> getShippingMethodByKey(String key) {
        return apiRoot
                .shippingMethods()
                .withKey(key)
                .get()
                .withExpand("zoneRates[*].zone")
                .execute();
    }

    public CompletableFuture<ApiHttpResponse<ShippingMethodPagedQueryResponse>> getShippingMethodsByCountry(String countryCode) {
        return apiRoot
                .shippingMethods()
                .matchingLocation()
                .get()
                .addCountry(countryCode)
                .withExpand("zoneRates[*].zone")
                .execute();
    }

    public CompletableFuture<ApiHttpResponse<JsonNode>> checkShippingMethodExistence(String key) {
        return apiRoot
                .shippingMethods()
                .withKey(key)
                .head()
                .execute();
    }
}
