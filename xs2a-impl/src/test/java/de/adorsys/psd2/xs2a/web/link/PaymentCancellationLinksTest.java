/*
 * Copyright 2018-2020 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package de.adorsys.psd2.xs2a.web.link;

import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.profile.ScaRedirectFlow;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.domain.Xs2aHrefType;
import de.adorsys.psd2.xs2a.domain.Links;
import de.adorsys.psd2.xs2a.domain.pis.CancelPaymentResponse;
import de.adorsys.psd2.xs2a.service.RedirectIdService;
import de.adorsys.psd2.xs2a.service.ScaApproachResolver;
import de.adorsys.psd2.xs2a.web.RedirectLinkBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCancellationLinksTest {

    private static final String HTTP_URL = "http://url";
    private static final String PAYMENT_PRODUCT = "sepa-credit-transfers";
    private static final String PAYMENT_ID = "1111111111111";
    private static final String PAYMENT_SERVICE = "payments";
    private static final String AUTHORISATION_ID = "463318a0-1e33-45d8-8209-e16444b18dda";
    private static final Xs2aHrefType REDIRECT_LINK = new Xs2aHrefType("built_redirect_link");
    private static final Xs2aHrefType CANCEL_AUTH_LINK = new Xs2aHrefType("http://url/v1/payments/sepa-credit-transfers/1111111111111/cancellation-authorisations");
    private static final Xs2aHrefType PIS_CANCELLATION_AUTH_LINK_URL = new Xs2aHrefType(CANCEL_AUTH_LINK.getHref() + "/" + AUTHORISATION_ID);
    public static final Xs2aHrefType START_AUTHORISATION_LINK = new Xs2aHrefType("http://url/v1/payments/sepa-credit-transfers/1111111111111/cancellation-authorisations");
    private static final Xs2aHrefType SELF_LINK = new Xs2aHrefType("http://url/v1/payments/sepa-credit-transfers/1111111111111");
    private static final Xs2aHrefType STATUS_LINK = new Xs2aHrefType("http://url/v1/payments/sepa-credit-transfers/1111111111111/status");
    private static final String CONFIRMATION_LINK = "confirmation_link";
    private static final PsuIdData PSU_DATA = new PsuIdData("psuId", "psuIdType", "psuCorporateId", "psuCorporateIdType", "psuIpAddress");
    private static final PsuIdData PSU_DATA_EMPTY = new PsuIdData(null, null, null, null, null);
    private PaymentCancellationLinks links;
    private Links expectedLinks;
    private CancelPaymentResponse response;
    private static final String INTERNAL_REQUEST_ID = "5c2d5564-367f-4e03-a621-6bef76fa4208";

    @Mock
    private ScaApproachResolver scaApproachResolver;
    @Mock
    private RedirectLinkBuilder redirectLinkBuilder;
    @Mock
    private RedirectIdService redirectIdService;

    @BeforeEach
    void setUp() {
        expectedLinks = new Links();

        response = new CancelPaymentResponse();
        response.setAuthorizationId(AUTHORISATION_ID);
        response.setPaymentId(PAYMENT_ID);
        response.setPaymentProduct(PAYMENT_PRODUCT);
        response.setPaymentType(PaymentType.SINGLE);
        response.setPsuData(PSU_DATA);
        response.setTransactionStatus(Xs2aTransactionStatus.ACCP);
        response.setInternalRequestId(INTERNAL_REQUEST_ID);
    }

    @Test
    void buildCancellationLinks_redirect_implicit() {
        boolean isExplicitMethod = false;
        when(scaApproachResolver.resolveScaApproach()).thenReturn(ScaApproach.REDIRECT);
        when(redirectIdService.generateRedirectId(AUTHORISATION_ID)).thenReturn(AUTHORISATION_ID);
        when(redirectLinkBuilder.buildPaymentCancellationScaRedirectLink(eq(PAYMENT_ID), eq(AUTHORISATION_ID), anyString(), eq(""))).thenReturn(REDIRECT_LINK.getHref());

        links = new PaymentCancellationLinks(HTTP_URL, scaApproachResolver, redirectLinkBuilder, redirectIdService, response, isExplicitMethod, ScaRedirectFlow.REDIRECT, false, "");

        expectedLinks.setSelf(SELF_LINK);
        expectedLinks.setStatus(STATUS_LINK);
        expectedLinks.setScaRedirect(REDIRECT_LINK);
        expectedLinks.setScaStatus(PIS_CANCELLATION_AUTH_LINK_URL);

        assertEquals(expectedLinks, links);
    }

    @Test
    void buildCancellationLinks_redirect_implicit_confirmation() {

        // Given
        boolean isExplicitMethod = false;
        when(scaApproachResolver.resolveScaApproach()).thenReturn(ScaApproach.REDIRECT);
        when(redirectIdService.generateRedirectId(AUTHORISATION_ID)).thenReturn(AUTHORISATION_ID);
        when(redirectLinkBuilder.buildPaymentCancellationScaRedirectLink(eq(PAYMENT_ID), eq(AUTHORISATION_ID), anyString(), eq(""))).thenReturn(REDIRECT_LINK.getHref());
        when(redirectLinkBuilder.buildPisCancellationConfirmationLink(PAYMENT_SERVICE, PAYMENT_PRODUCT, PAYMENT_ID, AUTHORISATION_ID)).thenReturn(CONFIRMATION_LINK);

        // When
        links = new PaymentCancellationLinks(HTTP_URL, scaApproachResolver, redirectLinkBuilder, redirectIdService, response, isExplicitMethod, ScaRedirectFlow.REDIRECT, true, "");

        expectedLinks.setSelf(SELF_LINK);
        expectedLinks.setStatus(STATUS_LINK);
        expectedLinks.setScaRedirect(REDIRECT_LINK);
        expectedLinks.setScaStatus(PIS_CANCELLATION_AUTH_LINK_URL);
        expectedLinks.setConfirmation(new Xs2aHrefType("http://url/confirmation_link"));

        // Then
        assertEquals(expectedLinks, links);
    }

    @Test
    void buildCancellationLinks_redirect_explicit() {
        boolean isExplicitMethod = true;
        when(scaApproachResolver.resolveScaApproach()).thenReturn(ScaApproach.REDIRECT);

        links = new PaymentCancellationLinks(HTTP_URL, scaApproachResolver, redirectLinkBuilder, redirectIdService, response, isExplicitMethod, ScaRedirectFlow.REDIRECT, false, "");

        expectedLinks.setSelf(SELF_LINK);
        expectedLinks.setStatus(STATUS_LINK);
        expectedLinks.setStartAuthorisation(CANCEL_AUTH_LINK);

        assertEquals(expectedLinks, links);
    }

    @Test
    void buildCancellationLinks_embedded_implicit() {
        boolean isExplicitMethod = false;
        when(scaApproachResolver.resolveScaApproach()).thenReturn(ScaApproach.EMBEDDED);

        links = new PaymentCancellationLinks(HTTP_URL, scaApproachResolver, redirectLinkBuilder, redirectIdService, response, isExplicitMethod, ScaRedirectFlow.REDIRECT, false, "");

        expectedLinks.setSelf(SELF_LINK);
        expectedLinks.setStatus(STATUS_LINK);
        expectedLinks.setUpdatePsuAuthentication(PIS_CANCELLATION_AUTH_LINK_URL);
        expectedLinks.setScaStatus(PIS_CANCELLATION_AUTH_LINK_URL);

        assertEquals(expectedLinks, links);
    }

    @Test
    void buildCancellationLinks_embedded_explicit() {
        boolean isExplicitMethod = true;
        when(scaApproachResolver.resolveScaApproach()).thenReturn(ScaApproach.EMBEDDED);

        links = new PaymentCancellationLinks(HTTP_URL, scaApproachResolver, redirectLinkBuilder, redirectIdService, response, isExplicitMethod, ScaRedirectFlow.REDIRECT, false, "");

        expectedLinks.setSelf(SELF_LINK);
        expectedLinks.setStatus(STATUS_LINK);
        expectedLinks.setStartAuthorisationWithPsuAuthentication(CANCEL_AUTH_LINK);

        assertEquals(expectedLinks, links);
    }

    @Test
    void buildCancellationLinks_decoupled_implicit() {
        boolean isExplicitMethod = false;
        when(scaApproachResolver.resolveScaApproach()).thenReturn(ScaApproach.DECOUPLED);

        links = new PaymentCancellationLinks(HTTP_URL, scaApproachResolver, redirectLinkBuilder, redirectIdService, response, isExplicitMethod, ScaRedirectFlow.REDIRECT, false, "");

        expectedLinks.setSelf(SELF_LINK);
        expectedLinks.setStatus(STATUS_LINK);
        expectedLinks.setUpdatePsuAuthentication(PIS_CANCELLATION_AUTH_LINK_URL);
        expectedLinks.setScaStatus(PIS_CANCELLATION_AUTH_LINK_URL);

        assertEquals(expectedLinks, links);
    }

    @Test
    void buildCancellationLinks_decoupled_explicit() {
        boolean isExplicitMethod = true;
        when(scaApproachResolver.resolveScaApproach()).thenReturn(ScaApproach.DECOUPLED);

        links = new PaymentCancellationLinks(HTTP_URL, scaApproachResolver, redirectLinkBuilder, redirectIdService, response, isExplicitMethod, ScaRedirectFlow.REDIRECT, false, "");

        expectedLinks.setSelf(SELF_LINK);
        expectedLinks.setStatus(STATUS_LINK);
        expectedLinks.setStartAuthorisationWithPsuAuthentication(CANCEL_AUTH_LINK);

        assertEquals(expectedLinks, links);
    }

    @Test
    void buildCancellationLinks_redirect_oAuth_preStep_implicit() {
        // Given
        boolean isExplicitMethod = false;
        when(scaApproachResolver.resolveScaApproach()).thenReturn(ScaApproach.REDIRECT);
        when(redirectIdService.generateRedirectId(AUTHORISATION_ID)).thenReturn(AUTHORISATION_ID);
        when(redirectLinkBuilder.buildPaymentCancellationScaRedirectLink(eq(PAYMENT_ID), eq(AUTHORISATION_ID), anyString(), eq(""))).thenReturn(REDIRECT_LINK.getHref());

        // When
        links = new PaymentCancellationLinks(HTTP_URL, scaApproachResolver, redirectLinkBuilder, redirectIdService, response, isExplicitMethod, ScaRedirectFlow.OAUTH_PRE_STEP, false, "");

        // Then
        expectedLinks.setSelf(SELF_LINK);
        expectedLinks.setStatus(STATUS_LINK);
        expectedLinks.setScaRedirect(REDIRECT_LINK);
        expectedLinks.setScaStatus(PIS_CANCELLATION_AUTH_LINK_URL);

        assertEquals(expectedLinks, links);
    }

    @Test
    void buildCancellationLinks_scaOAuth_explicit() {
        boolean isExplicitMethod = true;
        when(scaApproachResolver.resolveScaApproach()).thenReturn(ScaApproach.REDIRECT);

        links = new PaymentCancellationLinks(HTTP_URL, scaApproachResolver, redirectLinkBuilder, redirectIdService, response, isExplicitMethod, ScaRedirectFlow.OAUTH, false, "");

        expectedLinks.setSelf(SELF_LINK);
        expectedLinks.setStatus(STATUS_LINK);
        expectedLinks.setStartAuthorisation(START_AUTHORISATION_LINK);

        assertEquals(expectedLinks, links);
    }

    @Test
    void buildCancellationLinks_embedded_implicit_psuEmpty() {
        response.setPsuData(PSU_DATA_EMPTY);

        boolean isExplicitMethod = false;
        when(scaApproachResolver.resolveScaApproach()).thenReturn(ScaApproach.EMBEDDED);

        links = new PaymentCancellationLinks(HTTP_URL, scaApproachResolver, redirectLinkBuilder, redirectIdService, response, isExplicitMethod, ScaRedirectFlow.REDIRECT, false, "");

        expectedLinks.setSelf(SELF_LINK);
        expectedLinks.setStatus(STATUS_LINK);
        expectedLinks.setUpdatePsuAuthentication(PIS_CANCELLATION_AUTH_LINK_URL);
        expectedLinks.setScaStatus(PIS_CANCELLATION_AUTH_LINK_URL);

        assertEquals(expectedLinks, links);
    }

    @Test
    void buildCancellationLinks_embedded_explicit_psuEmpty() {
        response.setPsuData(PSU_DATA_EMPTY);

        boolean isExplicitMethod = true;
        when(scaApproachResolver.resolveScaApproach()).thenReturn(ScaApproach.EMBEDDED);

        links = new PaymentCancellationLinks(HTTP_URL, scaApproachResolver, redirectLinkBuilder, redirectIdService, response, isExplicitMethod, ScaRedirectFlow.REDIRECT, false, "");

        expectedLinks.setSelf(SELF_LINK);
        expectedLinks.setStatus(STATUS_LINK);
        expectedLinks.setStartAuthorisationWithPsuAuthentication(CANCEL_AUTH_LINK);

        assertEquals(expectedLinks, links);
    }

    @Test
    void buildCancellationLinks_status_RJCT() {
        boolean isExplicitMethod = true;
        response.setTransactionStatus(Xs2aTransactionStatus.RJCT);

        links = new PaymentCancellationLinks(HTTP_URL, scaApproachResolver, redirectLinkBuilder, redirectIdService, response, isExplicitMethod, ScaRedirectFlow.REDIRECT, false, "");

        assertEquals(expectedLinks, links);
    }
}
