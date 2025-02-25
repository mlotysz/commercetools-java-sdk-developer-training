package com.training.handson.services;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.order.Order;
import com.training.handson.dto.CustomFieldRequest;
import com.training.handson.dto.OrderRequest;
import io.vrap.rmf.base.client.ApiHttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {

    private final ProjectApiRoot apiRoot;
    private final String storeKey;

    public OrderService(ProjectApiRoot apiRoot, String storeKey) {
        this.apiRoot = apiRoot;
        this.storeKey = storeKey;
    }

    public CompletableFuture<ApiHttpResponse<Order>> createOrder(
            final OrderRequest orderRequest) {
        return apiRoot.inStore(storeKey)
                .orders()
                .post(orderFromCartDraftBuilder -> orderFromCartDraftBuilder
                        .cart(builder -> builder.id(orderRequest.getCartId()))
                        .version(orderRequest.getCartVersion())
                        .orderNumber("ML-" + System.currentTimeMillis())
                )

                .execute();
    }

    public CompletableFuture<ApiHttpResponse<Order>> setCustomFields(
            final CustomFieldRequest customFieldRequest) {

        final String orderNumber = customFieldRequest.getOrderNumber();
        return getOrderByOrderNumber(orderNumber)
                .thenApply(ApiHttpResponse::getBody)
                .thenComposeAsync(order -> {
                    return apiRoot
                            .inStore(storeKey)
                            .orders()
                            .withId(order.getId())
                            .post(ou -> ou.version(order.getVersion())
                                    .plusActions(oua -> oua.setCustomTypeBuilder()
                                            .type(tri -> tri.key("ct-delivery-instructions"))
                                    )
                                    .plusActions(oua -> oua.setCustomFieldBuilder()
                                            .name("instructions")
                                            .value(customFieldRequest.getInstructions())
                                    )
                                    .plusActions(oua -> oua.setCustomFieldBuilder()
                                            .name("time")
                                            .value(customFieldRequest.getTime())
                                    )
                            )
                            .execute();
                });
    }

    public CompletableFuture<ApiHttpResponse<Order>> getOrderByOrderNumber(final String orderNumber) {

        return apiRoot
                .inStore(storeKey)
                .orders()
                .withOrderNumber(orderNumber)
                .get()
                .execute();
    }

    public CompletableFuture<ApiHttpResponse<Order>> getOrderById(final String orderId) {

        return apiRoot
                .inStore(storeKey)
                .orders()
                .withId(orderId)
                .get()
                .execute();
    }

}
