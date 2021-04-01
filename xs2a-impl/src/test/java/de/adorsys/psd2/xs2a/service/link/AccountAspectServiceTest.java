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

package de.adorsys.psd2.xs2a.service.link;

import de.adorsys.psd2.aspsp.profile.domain.AspspSettings;
import de.adorsys.psd2.core.data.Xs2aConsentAccountAccess;
import de.adorsys.psd2.core.data.ais.AisConsent;
import de.adorsys.psd2.core.data.ais.AisConsentData;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.account.Xs2aAccountDetails;
import de.adorsys.psd2.xs2a.domain.account.Xs2aAccountDetailsHolder;
import de.adorsys.psd2.xs2a.domain.account.Xs2aAccountListHolder;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aCreatePisCancellationAuthorisationResponse;
import de.adorsys.psd2.xs2a.service.profile.AspspProfileServiceWrapper;
import de.adorsys.psd2.xs2a.web.link.AccountDetailsLinks;
import de.adorsys.xs2a.reader.JsonReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static de.adorsys.psd2.xs2a.core.domain.TppMessageInformation.of;
import static de.adorsys.psd2.xs2a.core.error.ErrorType.AIS_400;
import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.CONSENT_UNKNOWN_400;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAspectServiceTest {

    @InjectMocks
    private AccountAspectService service;

    @Mock
    private AspspProfileServiceWrapper aspspProfileServiceWrapper;

    private Xs2aAccountDetails accountDetails;
    private AisConsent aisConsent;
    private ResponseObject responseObject;
    private JsonReader jsonReader = new JsonReader();

    @BeforeEach
    void setUp() {
        aisConsent = jsonReader.getObjectFromFile("json/aspect/ais-consent.json", AisConsent.class);
        Xs2aConsentAccountAccess accountAccess = jsonReader.getObjectFromFile("json/aspect/account-access.json", Xs2aConsentAccountAccess.class);
        aisConsent.setTppAccountAccesses(accountAccess);
        aisConsent.setAspspAccountAccesses(accountAccess);

        aisConsent.setConsentData(new AisConsentData(null, null, null,
                                                     false));

        accountDetails = jsonReader.getObjectFromFile("json/aspect/account_details.json", Xs2aAccountDetails.class);
    }

    @Test
    void getAccountDetailsAspect_success() {
        // Given
        AspspSettings aspspSettings = jsonReader.getObjectFromFile("json/aspect/aspsp-settings.json", AspspSettings.class);
        when(aspspProfileServiceWrapper.isForceXs2aBaseLinksUrl())
            .thenReturn(aspspSettings.getCommon().isForceXs2aBaseLinksUrl());
        when(aspspProfileServiceWrapper.getXs2aBaseLinksUrl())
            .thenReturn(aspspSettings.getCommon().getXs2aBaseLinksUrl());

        responseObject = ResponseObject.<Xs2aAccountDetailsHolder>builder()
                             .body(new Xs2aAccountDetailsHolder(accountDetails, aisConsent))
                             .build();
        // When
        ResponseObject actualResponse = service.getAccountDetailsAspect(responseObject);

        // Then
        assertNotNull(accountDetails.getLinks());
        assertTrue(accountDetails.getLinks() instanceof AccountDetailsLinks);

        assertFalse(actualResponse.hasError());
    }

    @Test
    void getAccountDetailsAspect_withError_shouldAddTextErrorMessage() {
        // Given
        responseObject = ResponseObject.<Xs2aCreatePisCancellationAuthorisationResponse>builder()
                             .fail(AIS_400, of(CONSENT_UNKNOWN_400))
                             .build();
        // When
        ResponseObject actualResponse = service.getAccountDetailsAspect(responseObject);

        // Then
        assertTrue(actualResponse.hasError());
    }

    @Test
    void getAccountDetailsListAspect_success() {
        // Given
        AspspSettings aspspSettings = jsonReader.getObjectFromFile("json/aspect/aspsp-settings.json", AspspSettings.class);
        when(aspspProfileServiceWrapper.isForceXs2aBaseLinksUrl())
            .thenReturn(aspspSettings.getCommon().isForceXs2aBaseLinksUrl());
        when(aspspProfileServiceWrapper.getXs2aBaseLinksUrl())
            .thenReturn(aspspSettings.getCommon().getXs2aBaseLinksUrl());

        responseObject = ResponseObject.<Xs2aAccountListHolder>builder()
                             .body(new Xs2aAccountListHolder(Collections.singletonList(accountDetails), aisConsent))
                             .build();
        // When
        ResponseObject actualResponse = service.getAccountDetailsListAspect(responseObject);

        // Then
        assertNotNull(accountDetails.getLinks());
        assertTrue(accountDetails.getLinks() instanceof AccountDetailsLinks);
        assertFalse(actualResponse.hasError());
    }

    @Test
    void getAccountDetailsListAspect_withError_shouldAddTextErrorMessage() {
        // Given
        responseObject = ResponseObject.<Xs2aCreatePisCancellationAuthorisationResponse>builder()
                             .fail(AIS_400, of(CONSENT_UNKNOWN_400))
                             .build();
        // When
        ResponseObject actualResponse = service.getAccountDetailsListAspect(responseObject);

        // Then
        assertTrue(actualResponse.hasError());
    }
}
