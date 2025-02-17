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

package de.adorsys.psd2.xs2a.service.authorization.piis;

import de.adorsys.psd2.xs2a.core.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.core.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.core.error.ErrorType;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.error.TppMessage;
import de.adorsys.psd2.xs2a.core.mapper.ServiceType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.consent.ConsentAuthorisationsParameters;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationDecoupledScaResponse;
import de.adorsys.psd2.xs2a.spi.domain.piis.SpiPiisConsent;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.PiisConsentSpi;
import de.adorsys.psd2.xs2a.util.reader.TestSpiDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonDecoupledPiisServiceTest {
    private static final String CONSENT_ID = "Test consentId";
    private static final String PSU_ID = "Test psuId";
    private static final String AUTHORISATION = "Bearer 1111111";
    private static final String AUTHORISATION_ID = "Test authorisationId";
    private static final String PSU_SUCCESS_MESSAGE = "Test psuSuccessMessage";
    private static final ScaStatus FAILED_SCA_STATUS = ScaStatus.FAILED;
    private static final SpiPsuData SPI_PSU_DATA = SpiPsuData.builder().build();
    private static final PsuIdData PSU_ID_DATA = new PsuIdData(PSU_ID, null, null, null, null);
    private static final SpiContextData SPI_CONTEXT_DATA = TestSpiDataProvider.buildWithPsuTppAuthToken(SPI_PSU_DATA, new TppInfo(), AUTHORISATION);
    private static final String PSU_ERROR_MESSAGE = "Test psuErrorMessage";
    private static final ScaStatus METHOD_SELECTED_SCA_STATUS = ScaStatus.SCAMETHODSELECTED;
    private static final String AUTHENTICATION_METHOD_ID = "Test authentication method id";
    private static final UpdateConsentPsuDataResponse UPDATE_CONSENT_PSU_DATA_RESPONSE = buildUpdateConsentPsuDataResponse();

    @InjectMocks
    private CommonDecoupledPiisService commonDecoupledPiisService;
    @Mock
    private PiisConsentSpi piisConsentSpi;
    @Mock
    private SpiErrorMapper spiErrorMapper;
    @Mock
    private SpiContextDataProvider spiContextDataProvider;
    @Mock
    private SpiPiisConsent spiPiisConsent;
    @Mock
    private ConsentAuthorisationsParameters request;
    @Mock
    private SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;
    @Mock
    private SpiAspspConsentDataProvider spiAspspConsentDataProvider;

    @BeforeEach
    void setUp() {
        when(spiContextDataProvider.provideWithPsuIdData(PSU_ID_DATA))
            .thenReturn(SPI_CONTEXT_DATA);

        when(aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)).thenReturn(spiAspspConsentDataProvider);

    }

    @Test
    void proceedDecoupledApproach_Success() {
        // Given
        when(piisConsentSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, AUTHENTICATION_METHOD_ID, spiPiisConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(new SpiAuthorisationDecoupledScaResponse(ScaStatus.SCAMETHODSELECTED, PSU_SUCCESS_MESSAGE)));

        // When
        UpdateConsentPsuDataResponse actualResponse = commonDecoupledPiisService.proceedDecoupledApproach(CONSENT_ID, AUTHORISATION_ID, spiPiisConsent, AUTHENTICATION_METHOD_ID, PSU_ID_DATA);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getPsuMessage()).isEqualTo(PSU_SUCCESS_MESSAGE);
        assertThat(actualResponse.getScaStatus()).isEqualTo(METHOD_SELECTED_SCA_STATUS);
    }

    @Test
    void proceedDecoupledApproach_Failure_StartScaDecoupledHasError() {
        // Given
        SpiAuthorisationDecoupledScaResponse spiAuthorisationDecoupledScaResponse = new SpiAuthorisationDecoupledScaResponse(ScaStatus.SCAMETHODSELECTED, PSU_ERROR_MESSAGE);
        when(piisConsentSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, AUTHENTICATION_METHOD_ID, spiPiisConsent, spiAspspConsentDataProvider))
            .thenReturn(buildErrorSpiResponse(spiAuthorisationDecoupledScaResponse));
        when(spiErrorMapper.mapToErrorHolder(buildErrorSpiResponse(spiAuthorisationDecoupledScaResponse), ServiceType.PIIS))
            .thenReturn(ErrorHolder
                            .builder(ErrorType.AIS_400)
                            .tppMessages(TppMessageInformation.of(MessageErrorCode.FORMAT_ERROR))
                            .build());

        // When
        UpdateConsentPsuDataResponse actualResponse = commonDecoupledPiisService.proceedDecoupledApproach(CONSENT_ID, AUTHORISATION_ID, spiPiisConsent, AUTHENTICATION_METHOD_ID, PSU_ID_DATA);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getScaStatus()).isEqualTo(FAILED_SCA_STATUS);
        assertThat(actualResponse.getErrorHolder().getErrorType()).isEqualTo(ErrorType.AIS_400);
    }

    // Needed because SpiResponse is final, so it's impossible to mock it
    private <T> SpiResponse<T> buildSuccessSpiResponse(T payload) {
        return SpiResponse.<T>builder()
                   .payload(payload)
                   .build();
    }

    // Needed because SpiResponse is final, so it's impossible to mock it
    private <T> SpiResponse<T> buildErrorSpiResponse(T payload) {
        return SpiResponse.<T>builder()
                   .payload(payload)
                   .error(new TppMessage(MessageErrorCode.FORMAT_ERROR))
                   .build();
    }

    private static UpdateConsentPsuDataResponse buildUpdateConsentPsuDataResponse() {
        UpdateConsentPsuDataResponse response = new UpdateConsentPsuDataResponse(METHOD_SELECTED_SCA_STATUS, CONSENT_ID, AUTHORISATION_ID, PSU_ID_DATA);
        response.setPsuMessage(PSU_SUCCESS_MESSAGE);
        return response;
    }
}
