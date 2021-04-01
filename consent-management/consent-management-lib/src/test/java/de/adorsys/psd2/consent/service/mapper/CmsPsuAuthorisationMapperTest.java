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

package de.adorsys.psd2.consent.service.mapper;

import de.adorsys.psd2.consent.domain.AuthorisationEntity;
import de.adorsys.psd2.consent.domain.CmsPsuData;
import de.adorsys.psd2.consent.psu.api.CmsPsuAuthorisation;
import de.adorsys.psd2.consent.psu.api.CmsPsuConfirmationOfFundsAuthorisation;
import de.adorsys.psd2.xs2a.core.authorisation.AuthorisationType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.sca.Xs2aScaStatus;
import de.adorsys.xs2a.reader.JsonReader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CmsPsuAuthorisationMapperImpl.class, PsuDataMapper.class})
class CmsPsuAuthorisationMapperTest {
    private static final String EXTERNAL_ID = "123";
    private static final String PARENT_ID = "22d0a277-56c4-4c22-a582-9a9790ed9ddd";
    private static final String OK_REDIRECT_URI = "OK redirect";
    private static final String NOK_REDIRECT_URI = "Not OK redirect";
    private static final OffsetDateTime EXPIRATION_TIMESTAMP = OffsetDateTime.now().plusDays(1);
    private static final String PSU_ID = "PSU ID";
    private static final String PSU_ID_TYPE = "PSU ID type";

    @Autowired
    private CmsPsuAuthorisationMapper cmsPsuAuthorisationMapper;

    private JsonReader jsonReader;

    @BeforeEach
    void init() {
        jsonReader = new JsonReader();
    }

    @Test
    void mapToCmsPsuAuthorisationPis_success() {
        // Given
        AuthorisationEntity pisAuthorization = buildPisAuthorisation();

        // When
        CmsPsuAuthorisation actual = cmsPsuAuthorisationMapper.mapToCmsPsuAuthorisation(pisAuthorization);

        // Then
        CmsPsuAuthorisation expected = jsonReader.getObjectFromFile("json/service/mapper/cms-psu-authorisation-pis.json", CmsPsuAuthorisation.class);
        expected.setAuthorisationExpirationTimestamp(EXPIRATION_TIMESTAMP);

        assertEquals(expected, actual);
    }

    @Test
    void mapToCmsPsuAuthorisationAis_success() {
        // Given
        AuthorisationEntity aisConsentAuthorization = buildAisConsentAuthorisation();

        // When
        CmsPsuAuthorisation actual = cmsPsuAuthorisationMapper.mapToCmsPsuAuthorisation(aisConsentAuthorization);

        // Then
        CmsPsuAuthorisation expected = jsonReader.getObjectFromFile("json/service/mapper/cms-psu-authorisation-ais.json", CmsPsuAuthorisation.class);
        expected.setAuthorisationExpirationTimestamp(EXPIRATION_TIMESTAMP);
        assertEquals(expected, actual);
    }

    @Test
    void mapToCmsPsuAuthorisationPisCancellation_success() {
        // Given
        AuthorisationEntity cancellationAuthorisation = buildPisCancellationAuthorisation();

        // When
        CmsPsuAuthorisation actual = cmsPsuAuthorisationMapper.mapToCmsPsuAuthorisation(cancellationAuthorisation);

        // Then
        CmsPsuAuthorisation expected = jsonReader.getObjectFromFile("json/service/mapper/cms-psu-pis-cancellation-authorisation.json", CmsPsuAuthorisation.class);
        expected.setAuthorisationExpirationTimestamp(EXPIRATION_TIMESTAMP);

        assertEquals(expected, actual);
    }

    @Test
    void mapToCmsPsuConfirmationOfFundsAuthorisation_success() {
        // Given
        AuthorisationEntity piisAuthorization = buildPiisAuthorisation();

        // When
        CmsPsuConfirmationOfFundsAuthorisation cmsPsuConfirmationOfFundsAuthorisation = cmsPsuAuthorisationMapper.mapToCmsPsuConfirmationOfFundsAuthorisation(piisAuthorization);

        // Then
        CmsPsuConfirmationOfFundsAuthorisation expected = jsonReader.getObjectFromFile("json/service/mapper/cms-psu-authorisation-piis.json", CmsPsuConfirmationOfFundsAuthorisation.class);
        expected.setAuthorisationExpirationTimestamp(EXPIRATION_TIMESTAMP);
        expected.setRedirectUrlExpirationTimestamp(EXPIRATION_TIMESTAMP);

        assertEquals(expected, cmsPsuConfirmationOfFundsAuthorisation);
    }

    private AuthorisationEntity buildPiisAuthorisation() {
        AuthorisationEntity piisAuthorization = new AuthorisationEntity();
        piisAuthorization.setType(AuthorisationType.CONSENT);
        piisAuthorization.setExternalId(EXTERNAL_ID);
        piisAuthorization.setParentExternalId(PARENT_ID);
        piisAuthorization.setScaStatus(Xs2aScaStatus.RECEIVED);
        piisAuthorization.setPsuData(new CmsPsuData(PSU_ID, PSU_ID_TYPE, null, null, null));
        piisAuthorization.setScaApproach(ScaApproach.EMBEDDED);
        piisAuthorization.setTppOkRedirectUri(OK_REDIRECT_URI);
        piisAuthorization.setTppNokRedirectUri(NOK_REDIRECT_URI);
        piisAuthorization.setAuthorisationExpirationTimestamp(EXPIRATION_TIMESTAMP);
        piisAuthorization.setRedirectUrlExpirationTimestamp(EXPIRATION_TIMESTAMP);
        return piisAuthorization;
    }

    @NotNull
    private AuthorisationEntity buildPisAuthorisation() {
        AuthorisationEntity pisAuthorization = new AuthorisationEntity();
        pisAuthorization.setType(AuthorisationType.PIS_CREATION);
        pisAuthorization.setExternalId(EXTERNAL_ID);
        pisAuthorization.setScaStatus(Xs2aScaStatus.RECEIVED);
        pisAuthorization.setPsuData(new CmsPsuData(PSU_ID, PSU_ID_TYPE, "", "", ""));
        pisAuthorization.setAuthorisationExpirationTimestamp(EXPIRATION_TIMESTAMP);
        pisAuthorization.setScaApproach(ScaApproach.EMBEDDED);
        pisAuthorization.setTppOkRedirectUri(OK_REDIRECT_URI);
        pisAuthorization.setTppNokRedirectUri(NOK_REDIRECT_URI);
        return pisAuthorization;
    }

    @NotNull
    private AuthorisationEntity buildPisCancellationAuthorisation() {
        AuthorisationEntity pisAuthorization = new AuthorisationEntity();
        pisAuthorization.setType(AuthorisationType.PIS_CANCELLATION);
        pisAuthorization.setExternalId(EXTERNAL_ID);
        pisAuthorization.setScaStatus(Xs2aScaStatus.RECEIVED);
        pisAuthorization.setPsuData(new CmsPsuData(PSU_ID, PSU_ID_TYPE, "", "", ""));
        pisAuthorization.setAuthorisationExpirationTimestamp(EXPIRATION_TIMESTAMP);
        pisAuthorization.setScaApproach(ScaApproach.EMBEDDED);
        pisAuthorization.setTppOkRedirectUri(OK_REDIRECT_URI);
        pisAuthorization.setTppNokRedirectUri(NOK_REDIRECT_URI);
        return pisAuthorization;
    }

    @NotNull
    private AuthorisationEntity buildAisConsentAuthorisation() {
        AuthorisationEntity authorization = new AuthorisationEntity();
        authorization.setType(AuthorisationType.CONSENT);
        authorization.setExternalId(EXTERNAL_ID);
        authorization.setScaStatus(Xs2aScaStatus.RECEIVED);
        authorization.setPsuData(new CmsPsuData("PSU ID", "PSU ID type", "", "", ""));
        authorization.setAuthorisationExpirationTimestamp(EXPIRATION_TIMESTAMP);
        authorization.setScaApproach(ScaApproach.EMBEDDED);
        authorization.setTppOkRedirectUri(OK_REDIRECT_URI);
        authorization.setTppNokRedirectUri(NOK_REDIRECT_URI);
        return authorization;
    }
}
