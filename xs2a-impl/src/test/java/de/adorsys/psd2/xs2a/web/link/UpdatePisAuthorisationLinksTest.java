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

import de.adorsys.psd2.xs2a.core.authorisation.Xs2aAuthenticationObject;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.sca.Xs2aScaStatus;
import de.adorsys.psd2.xs2a.domain.Xs2aHrefType;
import de.adorsys.psd2.xs2a.domain.Links;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aCreatePisAuthorisationRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.ScaApproachResolver;
import de.adorsys.xs2a.reader.JsonReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.adorsys.psd2.xs2a.core.profile.PaymentType.SINGLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePisAuthorisationLinksTest {
    private static final String HTTP_URL = "http://url";
    private static final String PAYMENT_PRODUCT = "sepa-credit-transfers";
    private static final String PAYMENT_ID = "1111111111111";
    private static final String AUTHORISATION_ID = "463318a0-1e33-45d8-8209-e16444b18dda";

    @Mock
    private ScaApproachResolver scaApproachResolver;

    private UpdatePisAuthorisationLinks links;
    private Xs2aCreatePisAuthorisationRequest request;
    private Xs2aUpdatePisCommonPaymentPsuDataResponse response;
    private Xs2aAuthenticationObject authenticationObject;

    private Links expectedLinks;

    @BeforeEach
    void setUp() {
        expectedLinks = new Links();

        JsonReader jsonReader = new JsonReader();
        request = new Xs2aCreatePisAuthorisationRequest(PAYMENT_ID, null, PAYMENT_PRODUCT, SINGLE, "");
        response = new Xs2aUpdatePisCommonPaymentPsuDataResponse();
        response.setAuthorisationId(AUTHORISATION_ID);

        authenticationObject = jsonReader.getObjectFromFile("json/link/authentication-object.json", Xs2aAuthenticationObject.class);
    }

    @Test
    void isScaStatusMethodAuthenticated() {
        response.setScaStatus(Xs2aScaStatus.PSUAUTHENTICATED);
        links = new UpdatePisAuthorisationLinks(HTTP_URL, scaApproachResolver, response, request);

        expectedLinks.setScaStatus(new Xs2aHrefType("http://url/v1/payments/sepa-credit-transfers/1111111111111/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        expectedLinks.setSelectAuthenticationMethod(new Xs2aHrefType("http://url/v1/payments/sepa-credit-transfers/1111111111111/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        assertEquals(expectedLinks, links);
    }

    @Test
    void isAnotherScaStatus_failed() {
        response.setScaStatus(Xs2aScaStatus.FAILED);
        links = new UpdatePisAuthorisationLinks(HTTP_URL, scaApproachResolver, response, request);

        expectedLinks.setScaStatus(new Xs2aHrefType("http://url/v1/payments/sepa-credit-transfers/1111111111111/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        assertEquals(expectedLinks, links);
    }

    @Test
    void isScaStatusMethodSelectedAndEmbedded() {
        when(scaApproachResolver.getScaApproach(AUTHORISATION_ID)).thenReturn(ScaApproach.EMBEDDED);

        response.setScaStatus(Xs2aScaStatus.SCAMETHODSELECTED);
        response.setChosenScaMethod(authenticationObject);
        links = new UpdatePisAuthorisationLinks(HTTP_URL, scaApproachResolver, response, request);

        expectedLinks.setScaStatus(new Xs2aHrefType("http://url/v1/payments/sepa-credit-transfers/1111111111111/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        expectedLinks.setAuthoriseTransaction(new Xs2aHrefType("http://url/v1/payments/sepa-credit-transfers/1111111111111/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        assertEquals(expectedLinks, links);
    }

    @Test
    void isScaStatusFinalised() {
        response.setScaStatus(Xs2aScaStatus.FINALISED);
        links = new UpdatePisAuthorisationLinks(HTTP_URL, scaApproachResolver, response, request);

        expectedLinks.setScaStatus(new Xs2aHrefType("http://url/v1/payments/sepa-credit-transfers/1111111111111/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        assertEquals(expectedLinks, links);
    }

    @Test
    void isScaStatusMethodIdentified() {
        response.setScaStatus(Xs2aScaStatus.PSUIDENTIFIED);
        links = new UpdatePisAuthorisationLinks(HTTP_URL, scaApproachResolver, response, request);

        expectedLinks.setScaStatus(new Xs2aHrefType("http://url/v1/payments/sepa-credit-transfers/1111111111111/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        expectedLinks.setUpdatePsuAuthentication(new Xs2aHrefType("http://url/v1/payments/sepa-credit-transfers/1111111111111/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        assertEquals(expectedLinks, links);
    }
}
