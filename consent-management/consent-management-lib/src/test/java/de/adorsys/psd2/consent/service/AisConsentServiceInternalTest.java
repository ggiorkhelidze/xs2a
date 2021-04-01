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

package de.adorsys.psd2.consent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import de.adorsys.psd2.consent.api.ActionStatus;
import de.adorsys.psd2.consent.api.CmsError;
import de.adorsys.psd2.consent.api.CmsResponse;
import de.adorsys.psd2.consent.api.WrongChecksumException;
import de.adorsys.psd2.consent.api.ais.AdditionalAccountInformationType;
import de.adorsys.psd2.consent.api.ais.AisConsentActionRequest;
import de.adorsys.psd2.consent.api.ais.CmsConsent;
import de.adorsys.psd2.consent.domain.account.AisConsentAction;
import de.adorsys.psd2.consent.domain.account.AspspAccountAccess;
import de.adorsys.psd2.consent.domain.consent.ConsentEntity;
import de.adorsys.psd2.consent.repository.AisConsentActionRepository;
import de.adorsys.psd2.consent.repository.AisConsentVerifyingRepository;
import de.adorsys.psd2.consent.repository.AuthorisationRepository;
import de.adorsys.psd2.consent.service.account.AccountAccessUpdater;
import de.adorsys.psd2.consent.service.mapper.AccessMapper;
import de.adorsys.psd2.consent.service.mapper.CmsConsentMapper;
import de.adorsys.psd2.core.data.Xs2aConsentAccountAccess;
import de.adorsys.psd2.xs2a.core.consent.Xs2aConsentStatus;
import de.adorsys.xs2a.reader.JsonReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AisConsentServiceInternalTest {
    private static final String CONSENT_ID = "4b112130-6a96-4941-a220-2da8a4af2c65";
    private static final String TPP_ID = "TPP ID";
    private static final String REQUEST_URI = "/v1/accounts";

    private JsonReader jsonReader = new JsonReader();

    @Mock
    private AisConsentVerifyingRepository aisConsentRepository;
    @Mock
    private AisConsentActionRepository aisConsentActionRepository;
    @Mock
    private AuthorisationRepository authorisationRepository;
    @Mock
    private AisConsentConfirmationExpirationService aisConsentConfirmationExpirationService;
    @Mock
    private AisConsentUsageService aisConsentUsageService;
    @Mock
    private OneOffConsentExpirationService oneOffConsentExpirationService;
    @Mock
    private CmsConsentMapper cmsConsentMapper;
    @Mock
    private AccessMapper accessMapper;
    @Mock
    private AccountAccessUpdater accountAccessUpdater;

    @InjectMocks
    private AisConsentServiceInternal aisConsentServiceInternal;

    @Test
    void checkConsentAndSaveActionLog_shouldSaveActionLog() throws WrongChecksumException {
        AisConsentActionRequest aisConsentActionRequest = new AisConsentActionRequest(TPP_ID, CONSENT_ID, ActionStatus.SUCCESS, REQUEST_URI, true, null, null);
        ConsentEntity consentEntity = jsonReader.getObjectFromFile("json/service/ais-consent-service/consent-entity.json", ConsentEntity.class);
        when(aisConsentRepository.getActualAisConsent(CONSENT_ID)).thenReturn(Optional.of(consentEntity));
        ArgumentCaptor<AisConsentAction> aisConsentActionCaptor = ArgumentCaptor.forClass(AisConsentAction.class);

        CmsResponse<CmsResponse.VoidResponse> response = aisConsentServiceInternal.checkConsentAndSaveActionLog(aisConsentActionRequest);

        assertTrue(response.isSuccessful());
        verify(aisConsentActionRepository).save(aisConsentActionCaptor.capture());
        AisConsentAction capturedAction = aisConsentActionCaptor.getValue();
        assertEquals(TPP_ID, capturedAction.getTppId());
        assertEquals(CONSENT_ID, capturedAction.getRequestedConsentId());
        assertEquals(ActionStatus.SUCCESS, capturedAction.getActionStatus());
    }

    @Test
    void checkConsentAndSaveActionLog_noConsent() throws WrongChecksumException {
        AisConsentActionRequest aisConsentActionRequest = new AisConsentActionRequest(TPP_ID, CONSENT_ID, ActionStatus.SUCCESS, REQUEST_URI, true, null, null);
        when(aisConsentRepository.getActualAisConsent(CONSENT_ID)).thenReturn(Optional.empty());

        CmsResponse<CmsResponse.VoidResponse> response = aisConsentServiceInternal.checkConsentAndSaveActionLog(aisConsentActionRequest);

        assertTrue(response.isSuccessful());
        verify(aisConsentActionRepository, never()).save(any());
        verify(aisConsentUsageService, never()).incrementUsage(any(), any());
    }

    @Test
    void checkConsentAndSaveActionLog_shouldExpireOldConsents() throws WrongChecksumException {
        AisConsentActionRequest aisConsentActionRequest = new AisConsentActionRequest(TPP_ID, CONSENT_ID, ActionStatus.SUCCESS, REQUEST_URI, true, null, null);
        ConsentEntity consentEntity = jsonReader.getObjectFromFile("json/service/ais-consent-service/consent-entity-past-validUntil.json", ConsentEntity.class);
        when(aisConsentRepository.getActualAisConsent(CONSENT_ID)).thenReturn(Optional.of(consentEntity));
        ConsentEntity expiredConsent = jsonReader.getObjectFromFile("json/service/ais-consent-service/consent-entity-past-validUntil-expired.json", ConsentEntity.class);
        when(aisConsentConfirmationExpirationService.expireConsent(consentEntity)).thenReturn(expiredConsent);

        CmsResponse<CmsResponse.VoidResponse> response = aisConsentServiceInternal.checkConsentAndSaveActionLog(aisConsentActionRequest);

        assertTrue(response.isSuccessful());
        verify(aisConsentConfirmationExpirationService).expireConsent(consentEntity);
    }

    @Test
    void checkConsentAndSaveActionLog_shouldIncrementUsage() throws WrongChecksumException {
        AisConsentActionRequest aisConsentActionRequest = new AisConsentActionRequest(TPP_ID, CONSENT_ID, ActionStatus.SUCCESS, REQUEST_URI, true, null, null);
        ConsentEntity consentEntity = jsonReader.getObjectFromFile("json/service/ais-consent-service/consent-entity.json", ConsentEntity.class);
        when(aisConsentRepository.getActualAisConsent(CONSENT_ID)).thenReturn(Optional.of(consentEntity));

        CmsResponse<CmsResponse.VoidResponse> response = aisConsentServiceInternal.checkConsentAndSaveActionLog(aisConsentActionRequest);

        assertTrue(response.isSuccessful());
        verify(aisConsentUsageService).incrementUsage(consentEntity, aisConsentActionRequest);
    }

    @Test
    void checkConsentAndSaveActionLog_noUpdateUsageInRequest_shouldIgnoreUsage() throws WrongChecksumException {
        AisConsentActionRequest aisConsentActionRequest = new AisConsentActionRequest(TPP_ID, CONSENT_ID, ActionStatus.SUCCESS, REQUEST_URI, false, null, null);
        ConsentEntity consentEntity = jsonReader.getObjectFromFile("json/service/ais-consent-service/consent-entity.json", ConsentEntity.class);
        when(aisConsentRepository.getActualAisConsent(CONSENT_ID)).thenReturn(Optional.of(consentEntity));

        CmsResponse<CmsResponse.VoidResponse> response = aisConsentServiceInternal.checkConsentAndSaveActionLog(aisConsentActionRequest);

        assertTrue(response.isSuccessful());
        verify(aisConsentUsageService, never()).incrementUsage(any(), any());
    }

    @Test
    void checkConsentAndSaveActionLog_shouldExpireOneOffConsents() throws WrongChecksumException {
        AisConsentActionRequest aisConsentActionRequest = new AisConsentActionRequest(TPP_ID, CONSENT_ID, ActionStatus.SUCCESS, REQUEST_URI, true, null, null);
        ConsentEntity consentEntity = jsonReader.getObjectFromFile("json/service/ais-consent-service/consent-entity-one-off.json", ConsentEntity.class);
        when(aisConsentRepository.getActualAisConsent(CONSENT_ID)).thenReturn(Optional.of(consentEntity));
        CmsConsent cmsConsent = jsonReader.getObjectFromFile("json/service/ais-consent-service/cms-consent-one-off.json", CmsConsent.class);
        when(cmsConsentMapper.mapToCmsConsent(eq(consentEntity), anyList(), anyMap())).thenReturn(cmsConsent);
        when(oneOffConsentExpirationService.isConsentExpired(cmsConsent, 1L)).thenReturn(true);
        ArgumentCaptor<ConsentEntity> consentEntityCaptor = ArgumentCaptor.forClass(ConsentEntity.class);

        CmsResponse<CmsResponse.VoidResponse> response = aisConsentServiceInternal.checkConsentAndSaveActionLog(aisConsentActionRequest);

        assertTrue(response.isSuccessful());
        verify(aisConsentRepository).verifyAndSave(consentEntityCaptor.capture());
        ConsentEntity capturedConsentEntity = consentEntityCaptor.getValue();
        assertEquals(Xs2aConsentStatus.EXPIRED, capturedConsentEntity.getConsentStatus());
    }

    @Test
    void updateAspspAccountAccess() throws WrongChecksumException {
        ConsentEntity consentEntity = jsonReader.getObjectFromFile("json/service/ais-consent-service/consent-entity.json", ConsentEntity.class);
        when(aisConsentRepository.getActualAisConsent(CONSENT_ID)).thenReturn(Optional.of(consentEntity));
        List<AspspAccountAccess> aspspAccountAccesses = jsonReader.getObjectFromFile("json/service/ais-consent-service/aspsp-account-accesses.json", new TypeReference<>() {
        });
        Xs2aConsentAccountAccess existingAccountAccess = jsonReader.getObjectFromFile("json/service/ais-consent-service/account-access-existing.json", Xs2aConsentAccountAccess.class);
        when(accessMapper.mapAspspAccessesToAccountAccess(aspspAccountAccesses, AdditionalAccountInformationType.DEDICATED_ACCOUNTS, AdditionalAccountInformationType.NONE)).thenReturn(existingAccountAccess);
        Xs2aConsentAccountAccess accountAccess = jsonReader.getObjectFromFile("json/service/ais-consent-service/account-access.json", Xs2aConsentAccountAccess.class);
        Xs2aConsentAccountAccess updatedAccountAccess = jsonReader.getObjectFromFile("json/service/ais-consent-service/account-access-updated.json", Xs2aConsentAccountAccess.class);
        when(accountAccessUpdater.updateAccountReferencesInAccess(existingAccountAccess, accountAccess)).thenReturn(updatedAccountAccess);
        CmsConsent cmsConsent = jsonReader.getObjectFromFile("json/service/ais-consent-service/cms-consent.json", CmsConsent.class);
        when(cmsConsentMapper.mapToCmsConsent(eq(consentEntity), anyList(), anyMap())).thenReturn(cmsConsent);
        when(aisConsentRepository.verifyAndUpdate(consentEntity)).thenReturn(consentEntity);

        CmsResponse<CmsConsent> response = aisConsentServiceInternal.updateAspspAccountAccess(CONSENT_ID, accountAccess);

        assertTrue(response.isSuccessful());
        verify(accountAccessUpdater).updateAccountReferencesInAccess(existingAccountAccess, accountAccess);
    }

    @Test
    void updateAspspAccountAccess_withEmptyAccounts_shouldFillAccountsWithAccountReferences() throws WrongChecksumException {
        ConsentEntity consentEntity = jsonReader.getObjectFromFile("json/service/ais-consent-service/consent-entity.json", ConsentEntity.class);
        when(aisConsentRepository.getActualAisConsent(CONSENT_ID)).thenReturn(Optional.of(consentEntity));
        List<AspspAccountAccess> aspspAccountAccesses = jsonReader.getObjectFromFile("json/service/ais-consent-service/aspsp-account-accesses.json", new TypeReference<>() {
        });
        Xs2aConsentAccountAccess existingAccountAccess = jsonReader.getObjectFromFile("json/service/ais-consent-service/account-access-existing.json", Xs2aConsentAccountAccess.class);
        when(accessMapper.mapAspspAccessesToAccountAccess(aspspAccountAccesses, AdditionalAccountInformationType.DEDICATED_ACCOUNTS, AdditionalAccountInformationType.NONE)).thenReturn(existingAccountAccess);
        Xs2aConsentAccountAccess requestedAccountAccess = jsonReader.getObjectFromFile("json/service/ais-consent-service/account-access-requested.json", Xs2aConsentAccountAccess.class);
        Xs2aConsentAccountAccess updatedAccountAccess = jsonReader.getObjectFromFile("json/service/ais-consent-service/account-access-updated.json", Xs2aConsentAccountAccess.class);
        when(accountAccessUpdater.updateAccountReferencesInAccess(eq(existingAccountAccess), any())).thenReturn(updatedAccountAccess);
        CmsConsent cmsConsent = jsonReader.getObjectFromFile("json/service/ais-consent-service/cms-consent.json", CmsConsent.class);
        when(cmsConsentMapper.mapToCmsConsent(eq(consentEntity), anyList(), anyMap())).thenReturn(cmsConsent);
        when(aisConsentRepository.verifyAndUpdate(consentEntity)).thenReturn(consentEntity);
        ArgumentCaptor<Xs2aConsentAccountAccess> requestedAccountAccessCaptor = ArgumentCaptor.forClass(Xs2aConsentAccountAccess.class);
        Xs2aConsentAccountAccess accountAccessWithFilledAccounts = jsonReader.getObjectFromFile("json/service/ais-consent-service/account-access-requested-filled-accounts.json", Xs2aConsentAccountAccess.class);

        CmsResponse<CmsConsent> response = aisConsentServiceInternal.updateAspspAccountAccess(CONSENT_ID, requestedAccountAccess);

        assertTrue(response.isSuccessful());
        verify(accountAccessUpdater).updateAccountReferencesInAccess(eq(existingAccountAccess), requestedAccountAccessCaptor.capture());
        Xs2aConsentAccountAccess capturedAccountAccess = requestedAccountAccessCaptor.getValue();
        assertEquals(accountAccessWithFilledAccounts, capturedAccountAccess);
    }

    @Test
    void updateAspspAccountAccess_noConsent() throws WrongChecksumException {
        when(aisConsentRepository.getActualAisConsent(CONSENT_ID)).thenReturn(Optional.empty());
        Xs2aConsentAccountAccess accountAccess = jsonReader.getObjectFromFile("json/service/ais-consent-service/account-access.json", Xs2aConsentAccountAccess.class);

        CmsResponse<CmsConsent> response = aisConsentServiceInternal.updateAspspAccountAccess(CONSENT_ID, accountAccess);

        assertTrue(response.hasError());
        assertEquals(CmsError.LOGICAL_ERROR, response.getError());
    }
}
