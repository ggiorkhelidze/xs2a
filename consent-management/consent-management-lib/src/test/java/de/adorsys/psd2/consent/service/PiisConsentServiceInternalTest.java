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

import de.adorsys.psd2.consent.api.CmsResponse;
import de.adorsys.psd2.consent.api.ais.CmsConsent;
import de.adorsys.psd2.consent.domain.CmsPsuData;
import de.adorsys.psd2.consent.domain.consent.ConsentEntity;
import de.adorsys.psd2.consent.repository.ConsentJpaRepository;
import de.adorsys.psd2.consent.repository.specification.PiisConsentEntitySpecification;
import de.adorsys.psd2.consent.service.mapper.CmsConsentMapper;
import de.adorsys.psd2.consent.service.migration.PiisConsentLazyMigrationService;
import de.adorsys.psd2.xs2a.core.consent.Xs2aConsentStatus;
import de.adorsys.psd2.xs2a.core.profile.AccountReferenceSelector;
import de.adorsys.psd2.xs2a.core.profile.AccountReferenceType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PiisConsentServiceInternalTest {
    private static final Currency CURRENCY = Currency.getInstance("EUR");
    private static final String IBAN = "DE62500105179972514662";
    private static final String WRONG_IBAN = "FR7030066926176517166656113";
    private static final String PSU_ID = "PSU-123";
    private static final String PSU_ID_TYPE = "Some type";
    private static final String PSU_CORPORATE_ID = "Some corporate id";
    private static final String PSU_CORPORATE_ID_TYPE = "Some corporate id type";
    private static final String PSU_IP_ADDRESS = "Some ip address";
    private static final OffsetDateTime CREATION_TIMESTAMP = OffsetDateTime.of(2019, 2, 4, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final AccountReferenceSelector SELECTOR_IBAN = new AccountReferenceSelector(AccountReferenceType.IBAN, IBAN);
    private static final Specification<ConsentEntity> SPECIFICATION_IBAN = (root, cq, cb) -> null;

    @InjectMocks
    private PiisConsentServiceInternal piisConsentServiceInternal;

    @Mock
    private ConsentJpaRepository consentJpaRepository;
    @Mock
    private CmsConsentMapper cmsConsentMapper;
    @Mock
    private PiisConsentEntitySpecification piisConsentEntitySpecification;
    @Mock
    private PiisConsentLazyMigrationService piisConsentLazyMigrationService;

    @Test
    void getPiisConsentListByAccountIdentifier() {
        // Given
        when(piisConsentEntitySpecification.byCurrencyAndAccountReferenceSelector(CURRENCY, SELECTOR_IBAN))
            .thenReturn(SPECIFICATION_IBAN);
        ConsentEntity validConsentEntity = buildConsentEntity();
        when(consentJpaRepository.findAll(SPECIFICATION_IBAN))
            .thenReturn(Collections.singletonList(validConsentEntity));
        CmsConsent validConsent = buildCmsConsent();
        when(cmsConsentMapper.mapToCmsConsent(validConsentEntity, Collections.emptyList(), Collections.emptyMap()))
            .thenReturn(validConsent);
        when(piisConsentLazyMigrationService.migrateIfNeeded(validConsentEntity))
            .thenReturn(validConsentEntity);
        CmsConsent expected = buildCmsConsent();

        // When
        CmsResponse<List<CmsConsent>> piisConsentsResponse = piisConsentServiceInternal.getPiisConsentListByAccountIdentifier(CURRENCY,
                                                                                                                              new AccountReferenceSelector(AccountReferenceType.IBAN, IBAN));

        // Then
        assertTrue(piisConsentsResponse.isSuccessful());
        List<CmsConsent> piisConsents = piisConsentsResponse.getPayload();
        assertFalse(piisConsents.isEmpty());
        assertEquals(expected, piisConsents.get(0));
    }

    @Test
    void getPiisConsentListByAccountIdentifier_noCurrency() {
        // Given
        when(piisConsentEntitySpecification.byAccountReferenceSelector(SELECTOR_IBAN))
            .thenReturn(SPECIFICATION_IBAN);
        ConsentEntity validConsentEntity = buildConsentEntity();
        when(consentJpaRepository.findAll(SPECIFICATION_IBAN))
            .thenReturn(Collections.singletonList(validConsentEntity));
        CmsConsent validConsent = buildCmsConsent();
        when(cmsConsentMapper.mapToCmsConsent(validConsentEntity, Collections.emptyList(), Collections.emptyMap()))
            .thenReturn(validConsent);
        when(piisConsentLazyMigrationService.migrateIfNeeded(validConsentEntity))
            .thenReturn(validConsentEntity);
        CmsConsent expected = buildCmsConsent();

        // When
        CmsResponse<List<CmsConsent>> piisConsentsResponse = piisConsentServiceInternal.getPiisConsentListByAccountIdentifier(null,
                                                                                                                              new AccountReferenceSelector(AccountReferenceType.IBAN, IBAN));

        // Then
        assertTrue(piisConsentsResponse.isSuccessful());
        List<CmsConsent> piisConsents = piisConsentsResponse.getPayload();
        assertFalse(piisConsents.isEmpty());
        assertEquals(expected, piisConsents.get(0));
    }

    @Test
    void getPiisConsentListByAccountIdentifier_wrongIban() {
        // When
        CmsResponse<List<CmsConsent>> piisConsentsResponse = piisConsentServiceInternal.getPiisConsentListByAccountIdentifier(CURRENCY,
                                                                                                                              new AccountReferenceSelector(AccountReferenceType.IBAN, WRONG_IBAN));
        // Then
        assertTrue(piisConsentsResponse.getPayload().isEmpty());
    }

    private ConsentEntity buildConsentEntity() {
        ConsentEntity piisConsentEntity = new ConsentEntity();
        piisConsentEntity.setConsentStatus(Xs2aConsentStatus.VALID);
        piisConsentEntity.setPsuDataList(Collections.singletonList(buildPsuData()));
        piisConsentEntity.setCreationTimestamp(CREATION_TIMESTAMP);
        return piisConsentEntity;
    }

    private CmsPsuData buildPsuData() {
        return new CmsPsuData(PSU_ID, PSU_ID_TYPE, PSU_CORPORATE_ID, PSU_CORPORATE_ID_TYPE, PSU_IP_ADDRESS);
    }

    private PsuIdData buildPsuIdData() {
        return new PsuIdData(PSU_ID, PSU_ID_TYPE, PSU_CORPORATE_ID, PSU_CORPORATE_ID_TYPE, PSU_IP_ADDRESS);
    }

    private CmsConsent buildCmsConsent() {
        CmsConsent piisConsent = new CmsConsent();
        piisConsent.setPsuIdDataList(Collections.singletonList(buildPsuIdData()));
        piisConsent.setConsentStatus(Xs2aConsentStatus.VALID);
        piisConsent.setCreationTimestamp(CREATION_TIMESTAMP);
        return piisConsent;
    }
}

