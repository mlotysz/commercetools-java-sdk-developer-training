package com.training.handson.services;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart.Cart;
import com.commercetools.api.models.cart.CartDraftBuilder;
import com.commercetools.api.models.cart.CartSetShippingAddressAction;
import com.commercetools.api.models.cart.CartUpdateActionBuilder;
import com.commercetools.api.models.cart.LineItemDraftBuilder;
import com.commercetools.api.models.common.Address;
import com.commercetools.api.models.common.AddressBuilder;
import com.commercetools.api.models.shipping_method.ShippingMethod;
import com.commercetools.api.models.shipping_method.ShippingMethodResourceIdentifier;
import com.commercetools.api.models.shipping_method.ShippingMethodResourceIdentifierBuilder;
import com.training.handson.dto.AddressRequest;
import io.vrap.rmf.base.client.ApiHttpResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class CartService {

    private final ProjectApiRoot apiRoot;
    private final String storeKey;

    public CartService(ProjectApiRoot apiRoot, String storeKey) {
        this.apiRoot = apiRoot;
        this.storeKey = storeKey;
    }

    public CompletableFuture<ApiHttpResponse<Cart>> getCartById(final String cartId) {

        return apiRoot
                .inStore(storeKey)
                .carts()
                .withId(cartId)
                .get()
                .execute();
    }

    public CompletableFuture<ApiHttpResponse<Cart>> createAnonymousCart(
            final String sku,
            final Long quantity
//            final String supplyChannelKey,
//            final String distChannelKey
    ) {
        return apiRoot
                .inStore(storeKey)
                .carts()
                .post(CartDraftBuilder.of()
                        .currency("USD")
                        .country("US")
                        .lineItems(List.of(
                                LineItemDraftBuilder.of()
                                        .sku(sku)
                                        .quantity(quantity)
                                        .build()))
                        .build())
                .execute();
    }

    private String getCurrencyCodeByCountry(final String countryCode) {
        return switch (countryCode) {
            case "US" -> "USD";
            case "UK" -> "GBP";
            default -> "EUR";
        };
    }

    public CompletableFuture<ApiHttpResponse<Cart>> addProductToCartBySkusAndChannel(
            final String cartId,
            final String sku,
            final Long quantity
//            final String supplyChannelKey,
//            final String distChannelKey
    ) {

        return getCartById(cartId)
                .thenApply(ApiHttpResponse::getBody)
                .thenComposeAsync(cart ->
                        apiRoot.inStore(storeKey)
                                .carts()
                                .withId(cartId)
                                .post(cartUpdateBuilder ->
                                        cartUpdateBuilder
                                                .version(cart.getVersion())
                                                .plusActions(cartUpdateActionBuilder ->
                                                        cartUpdateActionBuilder.addLineItemBuilder()
                                                                .sku(sku)
                                                                .quantity(quantity)))
                                .execute());
    }

    public CompletableFuture<ApiHttpResponse<Cart>> addDiscountToCart(
            final String cartId,
            final String code) {

        return getCartById(cartId)
                .thenApply(ApiHttpResponse::getBody)
                .thenComposeAsync(cart ->
                        apiRoot.inStore(storeKey)
                                .carts()
                                .withId(cartId)
                                .post(cartUpdateBuilder ->
                                        cartUpdateBuilder
                                                .version(cart.getVersion())
                                                .plusActions(cartUpdateActionBuilder ->
                                                        cartUpdateActionBuilder.addDiscountCodeBuilder()
                                                                .code(code)))
                                .execute());
    }

    public CompletableFuture<ApiHttpResponse<Cart>> setShippingAddress(
            final AddressRequest addressRequest) {

        var address = AddressBuilder.of()
                .firstName(addressRequest.getFirstName())
                .lastName(addressRequest.getLastName())
                .country(addressRequest.getCountry())
                .email(addressRequest.getEmail())
                .build();
        var cartId = addressRequest.getCartId();

        ShippingMethod shippingMethod = apiRoot.shippingMethods()
                .matchingLocation()
                .get()
                .addCountry(addressRequest.getCountry())
                .executeBlocking().getBody().getResults().get(0);

        return getCartById(cartId)
                .thenApply(ApiHttpResponse::getBody)
                .thenComposeAsync(cart ->
                        apiRoot.inStore(storeKey)
                                .carts()
                                .withId(cartId)
                                .post(cartUpdateBuilder ->
                                        cartUpdateBuilder
                                                .version(cart.getVersion())
                                                .plusActions(cartUpdateActionBuilder ->
                                                        cartUpdateActionBuilder
                                                                .setShippingAddressBuilder()
                                                                .address(address))
                                                .plusActions(cartUpdateActionBuilder ->
                                                        cartUpdateActionBuilder
                                                                .setCustomerEmailBuilder()
                                                                .email(address.getEmail()))
                                                .plusActions(cartUpdateActionBuilder -> cartUpdateActionBuilder
                                                        .setShippingMethodBuilder()
                                                        .shippingMethod(ShippingMethodResourceIdentifierBuilder.of()
                                                                .id(shippingMethod.getId())
                                                                .build())))
                                .execute());
    }


    public CompletableFuture<ApiHttpResponse<Cart>> recalculate(final ApiHttpResponse<Cart> cartApiHttpResponse) {

        final Cart cart = cartApiHttpResponse.getBody();
        return
                apiRoot
                        .inStore(storeKey)
                        .carts()
                        .withId(cart.getId())
                        .post(
                                cartUpdateBuilder -> cartUpdateBuilder
                                        .version(cart.getVersion())
                                        .plusActions(
                                                cartUpdateActionBuilder -> cartUpdateActionBuilder
                                                        .recalculateBuilder()
                                                        .updateProductData(true)
                                        )
                        )
                        .execute();
    }

    public CompletableFuture<ApiHttpResponse<Cart>> setShippingMethod(final ApiHttpResponse<Cart> cartApiHttpResponse) {

        final Cart cart = cartApiHttpResponse.getBody();

        final ShippingMethod shippingMethod =
                apiRoot
                        .shippingMethods()
                        .matchingCart()
                        .get()
                        .withCartId(cart.getId())
                        .executeBlocking()
                        .getBody().getResults().get(0);
        return apiRoot
                .inStore(storeKey)
                .carts()
                .withId(cart.getId())
                .post(
                        cartUpdateBuilder -> cartUpdateBuilder
                                .version(cart.getVersion())
                                .plusActions(
                                        cartUpdateActionBuilder -> cartUpdateActionBuilder
                                                .setShippingMethodBuilder()
                                                .shippingMethod(
                                                        shippingMethodResourceIdentifierBuilder -> shippingMethodResourceIdentifierBuilder
                                                                .id(shippingMethod.getId())
                                                )
                                )
                )
                .execute();
    }

}
