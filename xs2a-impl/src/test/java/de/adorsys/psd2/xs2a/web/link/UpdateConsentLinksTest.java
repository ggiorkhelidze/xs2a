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

import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.Xs2aScaStatus;
import de.adorsys.psd2.xs2a.domain.Xs2aHrefType;
import de.adorsys.psd2.xs2a.domain.Links;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.ScaApproachResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAisConsentLinksImplTest {
    private static final String HTTP_URL = "http://url";
    private static final String CONSENT_ID = "9mp1PaotpXSToNCiu4GLwd6mq";
    private static final String AUTHORISATION_ID = "463318a0-1e33-45d8-8209-e16444b18dda";

    @Mock
    private ScaApproachResolver scaApproachResolver;

    private UpdateAisConsentLinksImpl links;

    private Links expectedLinks;

    @BeforeEach
    void setUp() {
        expectedLinks = new Links();
    }

    @Test
    void isScaStatusMethodAuthenticated() {
        UpdateConsentPsuDataResponse response = buildUpdateConsentPsuDataResponse(Xs2aScaStatus.PSUAUTHENTICATED);
        links = new UpdateAisConsentLinksImpl(HTTP_URL, scaApproachResolver, response);

        expectedLinks.setScaStatus(new Xs2aHrefType("http://url/v1/consents/9mp1PaotpXSToNCiu4GLwd6mq/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        expectedLinks.setSelectAuthenticationMethod(new Xs2aHrefType("http://url/v1/consents/9mp1PaotpXSToNCiu4GLwd6mq/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        assertEquals(expectedLinks, links);
    }

    @Test
    void isAnotherScaStatus_failed() {
        UpdateConsentPsuDataResponse response = buildUpdateConsentPsuDataResponse(Xs2aScaStatus.FAILED);
        links = new UpdateAisConsentLinksImpl(HTTP_URL, scaApproachResolver, response);

        expectedLinks.setScaStatus(new Xs2aHrefType("http://url/v1/consents/9mp1PaotpXSToNCiu4GLwd6mq/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        assertEquals(expectedLinks, links);
    }

    @Test
    void isScaStatusMethodSelectedAndDecoupleApproach() {
        when(scaApproachResolver.getScaApproach(AUTHORISATION_ID)).thenReturn(ScaApproach.DECOUPLED);

        UpdateConsentPsuDataResponse response = buildUpdateConsentPsuDataResponse(Xs2aScaStatus.SCAMETHODSELECTED);
        links = new UpdateAisConsentLinksImpl(HTTP_URL, scaApproachResolver, response);

        expectedLinks.setScaStatus(new Xs2aHrefType("http://url/v1/consents/9mp1PaotpXSToNCiu4GLwd6mq/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        assertEquals(expectedLinks, links);
    }

    @Test
    void isScaStatusMethodSelectedAndRedirectApproach() {
        when(scaApproachResolver.getScaApproach(AUTHORISATION_ID)).thenReturn(ScaApproach.REDIRECT);

        UpdateConsentPsuDataResponse response = buildUpdateConsentPsuDataResponse(Xs2aScaStatus.SCAMETHODSELECTED);
        links = new UpdateAisConsentLinksImpl(HTTP_URL, scaApproachResolver, response);

        expectedLinks.setScaStatus(new Xs2aHrefType("http://url/v1/consents/9mp1PaotpXSToNCiu4GLwd6mq/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        expectedLinks.setAuthoriseTransaction(new Xs2aHrefType("http://url/v1/consents/9mp1PaotpXSToNCiu4GLwd6mq/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        assertEquals(expectedLinks, links);
    }

    @Test
    void isScaStatusFinalised() {
        UpdateConsentPsuDataResponse response = buildUpdateConsentPsuDataResponse(Xs2aScaStatus.FINALISED);
        links = new UpdateAisConsentLinksImpl(HTTP_URL, scaApproachResolver, response);

        expectedLinks.setScaStatus(new Xs2aHrefType("http://url/v1/consents/9mp1PaotpXSToNCiu4GLwd6mq/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        assertEquals(expectedLinks, links);
    }

    @Test
    void isScaStatusMethodIdentified() {
        UpdateConsentPsuDataResponse response = buildUpdateConsentPsuDataResponse(Xs2aScaStatus.PSUIDENTIFIED);
        links = new UpdateAisConsentLinksImpl(HTTP_URL, scaApproachResolver, response);

        expectedLinks.setScaStatus(new Xs2aHrefType("http://url/v1/consents/9mp1PaotpXSToNCiu4GLwd6mq/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        expectedLinks.setUpdatePsuAuthentication(new Xs2aHrefType("http://url/v1/consents/9mp1PaotpXSToNCiu4GLwd6mq/authorisations/463318a0-1e33-45d8-8209-e16444b18dda"));
        assertEquals(expectedLinks, links);
    }

    private UpdateConsentPsuDataResponse buildUpdateConsentPsuDataResponse(Xs2aScaStatus scaStatus) {
        return new UpdateConsentPsuDataResponse(scaStatus, CONSENT_ID, AUTHORISATION_ID, new PsuIdData());
    }
}
