package com.training.handson.services;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.AddressDraftBuilder;
import com.commercetools.api.models.customer.AnonymousCartSignInMode;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerSignInResult;
import com.commercetools.api.models.customer.CustomerSigninBuilder;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.training.handson.dto.CustomFieldRequest;
import com.training.handson.dto.CustomerCreateRequest;
import io.vrap.rmf.base.client.ApiHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class CustomerService {

    private final ProjectApiRoot apiRoot;
    private final String storeKey;

    public CustomerService(ProjectApiRoot apiRoot, String storeKey) {
        this.apiRoot = apiRoot;
        this.storeKey = storeKey;
    }

    public CompletableFuture<ApiHttpResponse<Customer>> getCustomerByKey(String customerKey) {
        return apiRoot
                .inStore(storeKey)
                .customers()
                .withKey(customerKey)
                .get()
                .execute();
    }

    public CompletableFuture<ApiHttpResponse<Customer>> getCustomerById(String customerId) {
        return apiRoot
                .inStore(storeKey)
                .customers()
                .withId(customerId)
                .get()
                .execute();
    }


    public CompletableFuture<ApiHttpResponse<CustomerSignInResult>> createCustomer(
            final CustomerCreateRequest customerCreateRequest) {

        return apiRoot.inStore(storeKey)
                .customers()
                .post(customerDraftBuilder -> customerDraftBuilder
                        .anonymousCart(cartResourceIdentifierBuilder -> cartResourceIdentifierBuilder.id(customerCreateRequest.getAnonymousCartId()))
                        .email(customerCreateRequest.getEmail())
                        .password(customerCreateRequest.getPassword())
                        .addresses(AddressDraftBuilder.of().email(customerCreateRequest.getEmail())
                                .firstName(customerCreateRequest.getFirstName())
                                .lastName(customerCreateRequest.getLastName())
                                .country(customerCreateRequest.getCountry())
                                .build())
                        .defaultBillingAddress(0)
                        .defaultShippingAddress(0)
                )
                .execute();
    }

    public CompletableFuture<ApiHttpResponse<CustomerSignInResult>> loginCustomer(
            final CustomerCreateRequest customerCreateRequest) {

        CustomerSigninBuilder customerSigninBuilder = CustomerSigninBuilder.of()
                .email(customerCreateRequest.getEmail())
                .password(customerCreateRequest.getPassword())
                .anonymousCart(builder -> builder.id(customerCreateRequest.getAnonymousCartId()));

        if (customerCreateRequest.getAnonymousCartId() != null && !customerCreateRequest.getAnonymousCartId().isEmpty()) {
            customerSigninBuilder.anonymousCart(cri -> cri.id(customerCreateRequest.getAnonymousCartId()))
                    .anonymousCartSignInMode(AnonymousCartSignInMode.USE_AS_NEW_ACTIVE_CUSTOMER_CART);
        }

        return apiRoot.inStore(storeKey)
                .login()
                .post(customerSigninBuilder.build())
                .execute();
    }

    public CompletableFuture<ApiHttpResponse<Customer>> setCustomFields(
            final CustomFieldRequest customFieldRequest) {

        // TODO: Set a custom type reference and update custom field values from the request
        return CompletableFuture.completedFuture(
                new ApiHttpResponse<>(501, null, Customer.of())
        );
    }

    public CompletableFuture<ApiHttpResponse<CustomerGroup>> getCustomerGroupByKey(String customerGroupKey) {
        return
                apiRoot
                        .customerGroups()
                        .withKey(customerGroupKey)
                        .get()
                        .execute();
    }

    public CompletableFuture<ApiHttpResponse<Customer>> assignCustomerToCustomerGroup(
            final String customerKey,
            final String customerGroupKey) {

        return getCustomerByKey(customerKey)
                .thenComposeAsync(customerApiHttpResponse ->
                        apiRoot
                                .inStore(storeKey)
                                .customers()
                                .withKey(customerKey)
                                .post(
                                        customerUpdateBuilder -> customerUpdateBuilder
                                                .version(customerApiHttpResponse.getBody().getVersion())
                                                .plusActions(
                                                        customerUpdateActionBuilder -> customerUpdateActionBuilder
                                                                .setCustomerGroupBuilder()
                                                                .customerGroup(customerGroupResourceIdentifierBuilder -> customerGroupResourceIdentifierBuilder.key(customerGroupKey))
                                                )
                                )
                                .execute()
                );
    }

}
