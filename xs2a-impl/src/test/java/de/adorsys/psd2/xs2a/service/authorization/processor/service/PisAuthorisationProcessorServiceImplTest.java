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

package de.adorsys.psd2.xs2a.service.authorization.processor.service;

import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.xs2a.core.authorisation.AuthenticationObject;
import de.adorsys.psd2.xs2a.core.authorisation.Authorisation;
import de.adorsys.psd2.xs2a.core.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.core.domain.MessageCategory;
import de.adorsys.psd2.xs2a.core.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.core.error.ErrorType;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.error.TppMessage;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ChallengeData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.PaymentServicesHolder;
import de.adorsys.psd2.xs2a.service.PisMappersHolder;
import de.adorsys.psd2.xs2a.service.SpiCommonService;
import de.adorsys.psd2.xs2a.service.authorization.Xs2aAuthorisationService;
import de.adorsys.psd2.xs2a.service.authorization.pis.DecoupledPisScaAuthorisationService;
import de.adorsys.psd2.xs2a.service.authorization.pis.EmbeddedPisScaAuthorisationService;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisScaAuthorisationService;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.AuthorisationProcessorRequest;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.AuthorisationProcessorResponse;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.PisAuthorisationProcessorRequest;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.*;
import de.adorsys.psd2.xs2a.spi.domain.common.SpiAmount;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiSinglePayment;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiPaymentExecutionResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.PaymentAuthorisationSpi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static de.adorsys.psd2.xs2a.core.error.ErrorType.PIS_400;
import static de.adorsys.psd2.xs2a.core.error.ErrorType.PIS_401;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PisAuthorisationProcessorServiceImplTest {
    private static final String TEST_PAYMENT_ID = "12345676";
    private static final String TEST_AUTHORISATION_ID = "assddsff";
    private static final PsuIdData TEST_PSU_DATA = new PsuIdData("test-user", null, null, null, null);
    private static final ScaApproach TEST_SCA_APPROACH = ScaApproach.EMBEDDED;
    private static final ScaStatus TEST_SCA_STATUS = ScaStatus.RECEIVED;
    private static final String TEST_PAYMENT_PRODUCT = "sepa- credit-transfers";
    private static final SpiSinglePayment TEST_SPI_SINGLE_PAYMENT = new SpiSinglePayment(TEST_PAYMENT_PRODUCT);
    private static final String TEST_AUTHENTICATION_METHOD_ID = "sms";
    private static final String TEST_AUTHENTICATION_TYPE = "SMS_OTP";
    private static final ErrorType TEST_ERROR_TYPE_400 = PIS_400;
    private static final TransactionStatus TEST_TRANSACTION_STATUS_SUCCESS = TransactionStatus.ACSC;
    private static final TransactionStatus TEST_TRANSACTION_STATUS_MULTILEVEL_SCA = TransactionStatus.PATC;

    @InjectMocks
    private PisAuthorisationProcessorServiceImpl pisAuthorisationProcessorService;
    @Mock
    private PisMappersHolder pisMappersHolder;
    @Mock
    private PaymentServicesHolder paymentServicesHolder;
    @Mock
    private Xs2aAuthorisationService xs2aAuthorisationService;
    @Mock
    private SpiCommonService spiService;
    @Mock
    private PaymentAuthorisationSpi paymentAuthorisationSpi;
    @Mock
    private List<PisScaAuthorisationService> services;

    @Test
    void updateAuthorisation_success() {
        // Given
        EmbeddedPisScaAuthorisationService embeddedPisScaAuthorisationService = Mockito.mock(EmbeddedPisScaAuthorisationService.class);
        DecoupledPisScaAuthorisationService decoupledPisScaAuthorisationService = Mockito.mock(DecoupledPisScaAuthorisationService.class);
        services = Arrays.asList(embeddedPisScaAuthorisationService, decoupledPisScaAuthorisationService);

        when(embeddedPisScaAuthorisationService.getScaApproachServiceType()).thenReturn(ScaApproach.EMBEDDED);

        PisAuthorisationProcessorServiceImpl pisAuthorisationProcessorService = new PisAuthorisationProcessorServiceImpl(services, null, null, null, null, null);

        //When
        pisAuthorisationProcessorService.updateAuthorisation(buildAuthorisationProcessorRequest(), buildAuthorisationProcessorResponse());

        //Then
        verify(embeddedPisScaAuthorisationService).updateAuthorisation(any(), eq(buildAuthorisationProcessorResponse()));
    }

    @Test
    void doScaReceived_authorisation_no_sca_success() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(new SpiAvailableScaMethodsResponse(false, Collections.emptyList()))
                                                                                                     .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .payload(buildSpiPaymentExecutionResponse(TEST_TRANSACTION_STATUS_SUCCESS))
                                                                                                 .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.FINALISED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(paymentServicesHolder).updatePaymentStatus(TEST_PAYMENT_ID, TEST_TRANSACTION_STATUS_SUCCESS);
    }

    @Test
    void doScaReceived_authorisation_one_sca_success() {
        // Given
        TEST_SPI_SINGLE_PAYMENT.setPaymentId(TEST_PAYMENT_ID);

        PisCommonPaymentResponse commonPaymentResponse = new PisCommonPaymentResponse();

        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(commonPaymentResponse));
        when(pisMappersHolder.mapToSpiPayment(commonPaymentResponse))
            .thenReturn(TEST_SPI_SINGLE_PAYMENT);
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(buildSingleScaMethodsResponse())
                                                                                                     .build());
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), any(), any())).thenReturn(SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                                                          .payload(buildSpiAuthorizationCodeResult())
                                                                                                          .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.SCAMETHODSELECTED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        assertThat(actual.getChosenScaMethod()).isEqualTo(buildAuthenticationObject());
        assertThat(actual.getChallengeData()).isEqualTo(buildChallengeData());
        verify(xs2aAuthorisationService).saveAuthenticationMethods(eq(TEST_AUTHORISATION_ID), any());
    }

    @Test
    void doScaReceived_authorisation_multiple_sca_success() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(buildMultipleScaMethodsResponse())
                                                                                                     .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.PSUAUTHENTICATED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        assertThat(actual.getAvailableScaMethods()).isEqualTo(buildXs2aAuthenticationObjectList());
        verify(xs2aAuthorisationService).saveAuthenticationMethods(eq(TEST_AUTHORISATION_ID), any());
    }

    @Test
    void doScaReceived_authorisation_authorise_Psu_with_error_failure() {
        // Given
        SpiResponse<SpiPsuAuthorisationResponse> spiResponse = SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                   .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                   .build();
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(spiResponse);
        when(spiService.mapToErrorHolder(eq(spiResponse), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaReceived_authorisation_authorise_Psu_incorrect_credentials_failure() {
        // Given
        SpiResponse<SpiPsuAuthorisationResponse> spiResponse = SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                   .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.FAILURE))
                                                                   .build();
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(spiResponse);

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual);
        ErrorHolder errorHolder = actual.getErrorHolder();
        TppMessageInformation tppMessageInformation = errorHolder.getTppMessageInformationList().get(0);
        assertNotNull(errorHolder);
        assertThat(errorHolder.getErrorType()).isEqualTo(PIS_401);
        assertThat(tppMessageInformation.getCategory()).isEqualTo(MessageCategory.ERROR);
        assertThat(tppMessageInformation.getMessageErrorCode()).isEqualTo(MessageErrorCode.PSU_CREDENTIALS_INVALID);
    }

    @Test
    void doScaReceived_authorisation_authorise_Psu_exemption_success() {
        // Given
        SpiResponse<SpiPsuAuthorisationResponse> spiResponse = SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                   .payload(new SpiPsuAuthorisationResponse(true, SpiAuthorisationStatus.SUCCESS))
                                                                   .build();
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(spiResponse);
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .payload(buildSpiPaymentExecutionResponse(TEST_TRANSACTION_STATUS_SUCCESS))
                                                                                                 .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.EXEMPTED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(paymentServicesHolder).updatePaymentStatus(TEST_PAYMENT_ID, TEST_TRANSACTION_STATUS_SUCCESS);
    }

    @Test
    void doScaReceived_authorisation_authorise_Psu_sca_exemption_payment_execution_failure() {

        // Given
        SpiResponse<SpiPsuAuthorisationResponse> spiResponse = SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                   .payload(new SpiPsuAuthorisationResponse(true, SpiAuthorisationStatus.SUCCESS))
                                                                   .build();
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(spiResponse);
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                                                 .build());
        when(spiService.mapToErrorHolder(any(), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaReceived_authorisation_decoupled_chosen_success() {
        // Given
        AuthorisationProcessorRequest request = buildAuthorisationProcessorRequest();
        Authorisation authorisation = request.getAuthorisation();
        authorisation.setChosenScaApproach(ScaApproach.DECOUPLED);
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());

        // When
        pisAuthorisationProcessorService.doScaReceived(request);

        // Then
        verify(paymentServicesHolder).proceedDecoupledInitiation(any(), any(), any());
    }

    @Test
    void doScaReceived_authorisation_request_availableScaMethods_failure() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                                                     .build());
        when(spiService.mapToErrorHolder(any(), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaReceived_authorisation_request_availableScaMethods_exemption_success() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        SpiAvailableScaMethodsResponse response = buildSingleScaMethodsResponse();
        response.setScaExempted(true);
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(response)
                                                                                                     .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .payload(buildSpiPaymentExecutionResponse(TEST_TRANSACTION_STATUS_SUCCESS))
                                                                                                 .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.EXEMPTED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(paymentServicesHolder).updatePaymentStatus(TEST_PAYMENT_ID, TEST_TRANSACTION_STATUS_SUCCESS);
    }

    @Test
    void doScaReceived_authorisation_request_availableScaMethods_exemption_payment_execution_failure() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        SpiAvailableScaMethodsResponse response = buildSingleScaMethodsResponse();
        response.setScaExempted(true);
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(response)
                                                                                                     .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                                                 .build());
        when(spiService.mapToErrorHolder(any(), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaReceived_authorisation_no_sca_payment_execution_failure() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(new SpiAvailableScaMethodsResponse(false, Collections.emptyList()))
                                                                                                     .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                                                 .build());
        when(spiService.mapToErrorHolder(any(), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaReceived_authorisation_one_sca_decoupled_chosen_success() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        SpiAvailableScaMethodsResponse response = buildSingleScaMethodsResponse();
        response.getAvailableScaMethods().get(0).setDecoupled(true);
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(response)
                                                                                                     .build());

        // When
        pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        verify(paymentServicesHolder).proceedDecoupledInitiation(any(), any(), any());
    }

    @Test
    void doScaReceived_authorisation_one_sca_requestAuthorisationCode_failure() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(buildSingleScaMethodsResponse())
                                                                                                     .build());
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), any(), any())).thenReturn(SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                                                          .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                                                          .build());
        when(spiService.mapToErrorHolder(any(), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaReceived_authorisation_request_requestAuthorisationCode_exemption_success() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = new PisCommonPaymentResponse();

        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(commonPaymentResponse));
        when(pisMappersHolder.mapToSpiPayment(commonPaymentResponse))
            .thenReturn(TEST_SPI_SINGLE_PAYMENT);
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(buildSingleScaMethodsResponse())
                                                                                                     .build());
        SpiAuthorizationCodeResult result = buildSpiAuthorizationCodeResult();
        result.setScaExempted(true);
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), any(), any())).thenReturn(SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                                                          .payload(result)
                                                                                                          .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .payload(buildSpiPaymentExecutionResponse(TEST_TRANSACTION_STATUS_SUCCESS))
                                                                                                 .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.EXEMPTED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(paymentServicesHolder).updatePaymentStatus(TEST_PAYMENT_ID, TEST_TRANSACTION_STATUS_SUCCESS);
    }

    @Test
    void doScaReceived_authorisation_request_requestAuthorisationCode_exemption_payment_execution_failure() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = new PisCommonPaymentResponse();

        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(commonPaymentResponse));
        when(pisMappersHolder.mapToSpiPayment(commonPaymentResponse))
            .thenReturn(TEST_SPI_SINGLE_PAYMENT);
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(buildSingleScaMethodsResponse())
                                                                                                     .build());
        SpiAuthorizationCodeResult result = buildSpiAuthorizationCodeResult();
        result.setScaExempted(true);
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), any(), any())).thenReturn(SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                                                          .payload(result)
                                                                                                          .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                                                 .build());
        when(spiService.mapToErrorHolder(any(), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaReceived_identification_success() {
        // Given
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(buildIdentificationAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.PSUIDENTIFIED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
    }

    @Test
    void doScaReceived_identification_no_psu_failure() {
        // Given
        AuthorisationProcessorRequest authorisationProcessorRequest = buildIdentificationAuthorisationProcessorRequest();
        ((Xs2aUpdatePisCommonPaymentPsuDataRequest) authorisationProcessorRequest.getUpdateAuthorisationRequest()).setPsuData(null);

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaReceived(authorisationProcessorRequest);

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
    }

    @Test
    void doScaPsuIdentified_authorisation_no_sca_success() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(new SpiAvailableScaMethodsResponse(false, Collections.emptyList()))
                                                                                                     .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .payload(buildSpiPaymentExecutionResponse(TEST_TRANSACTION_STATUS_SUCCESS))
                                                                                                 .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.FINALISED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(paymentServicesHolder).updatePaymentStatus(TEST_PAYMENT_ID, TEST_TRANSACTION_STATUS_SUCCESS);
    }

    @Test
    void doScaPsuIdentified_authorisation_one_sca_success() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = new PisCommonPaymentResponse();

        TEST_SPI_SINGLE_PAYMENT.setPaymentId(TEST_PAYMENT_ID);
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(commonPaymentResponse));

        when(pisMappersHolder.mapToSpiPayment(commonPaymentResponse))
            .thenReturn(TEST_SPI_SINGLE_PAYMENT);
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(buildSingleScaMethodsResponse())
                                                                                                     .build());
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), any(), any())).thenReturn(SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                                                          .payload(buildSpiAuthorizationCodeResult())
                                                                                                          .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.SCAMETHODSELECTED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        assertThat(actual.getChosenScaMethod()).isEqualTo(buildAuthenticationObject());
        assertThat(actual.getChallengeData()).isEqualTo(buildChallengeData());
        verify(xs2aAuthorisationService).saveAuthenticationMethods(eq(TEST_AUTHORISATION_ID), any());
    }

    @Test
    void doScaPsuIdentified_authorisation_multiple_sca_success() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(buildMultipleScaMethodsResponse())
                                                                                                     .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.PSUAUTHENTICATED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        assertThat(actual.getAvailableScaMethods()).isEqualTo(buildXs2aAuthenticationObjectList());
        verify(xs2aAuthorisationService).saveAuthenticationMethods(eq(TEST_AUTHORISATION_ID), any());
    }

    @Test
    void doScaPsuIdentified_authorisation_authorise_Psu_with_error_failure() {
        // Given
        SpiResponse<SpiPsuAuthorisationResponse> spiResponse = SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                   .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                   .build();
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(spiResponse);
        when(spiService.mapToErrorHolder(eq(spiResponse), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaPsuIdentified_authorisation_authorise_Psu_incorrect_credentials_failure() {
        // Given
        SpiResponse<SpiPsuAuthorisationResponse> spiResponse = SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                   .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.FAILURE))
                                                                   .build();
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(spiResponse);

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual);
        ErrorHolder errorHolder = actual.getErrorHolder();
        TppMessageInformation tppMessageInformation = errorHolder.getTppMessageInformationList().get(0);
        assertNotNull(errorHolder);
        assertThat(errorHolder.getErrorType()).isEqualTo(PIS_401);
        assertThat(tppMessageInformation.getCategory()).isEqualTo(MessageCategory.ERROR);
        assertThat(tppMessageInformation.getMessageErrorCode()).isEqualTo(MessageErrorCode.PSU_CREDENTIALS_INVALID);
    }

    @Test
    void doScaPsuIdentified_authorisation_authorise_Psu_exemption_success() {
        // Given
        SpiResponse<SpiPsuAuthorisationResponse> spiResponse = SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                   .payload(new SpiPsuAuthorisationResponse(true, SpiAuthorisationStatus.SUCCESS))
                                                                   .build();
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(spiResponse);
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .payload(buildSpiPaymentExecutionResponse(TEST_TRANSACTION_STATUS_SUCCESS))
                                                                                                 .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.EXEMPTED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(paymentServicesHolder).updatePaymentStatus(TEST_PAYMENT_ID, TEST_TRANSACTION_STATUS_SUCCESS);
    }

    @Test
    void doScaPsuIdentified_authorisation_authorise_Psu_sca_exemption_payment_execution_failure() {

        // Given
        SpiResponse<SpiPsuAuthorisationResponse> spiResponse = SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                   .payload(new SpiPsuAuthorisationResponse(true, SpiAuthorisationStatus.SUCCESS))
                                                                   .build();
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(spiResponse);
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                                                 .build());
        when(spiService.mapToErrorHolder(any(), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaPsuIdentified_authorisation_decoupled_chosen_success() {
        // Given
        AuthorisationProcessorRequest request = buildAuthorisationProcessorRequest();
        Authorisation authorisation = request.getAuthorisation();
        authorisation.setChosenScaApproach(ScaApproach.DECOUPLED);
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());

        // When
        pisAuthorisationProcessorService.doScaPsuIdentified(request);

        // Then
        verify(paymentServicesHolder).proceedDecoupledInitiation(any(), any(), any());
    }

    @Test
    void doScaPsuIdentified_authorisation_request_availableScaMethods_failure() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                                                     .build());
        when(spiService.mapToErrorHolder(any(), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaPsuIdentified_authorisation_request_availableScaMethods_exemption_success() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        SpiAvailableScaMethodsResponse response = buildSingleScaMethodsResponse();
        response.setScaExempted(true);
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(response)
                                                                                                     .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .payload(buildSpiPaymentExecutionResponse(TEST_TRANSACTION_STATUS_SUCCESS))
                                                                                                 .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.EXEMPTED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(paymentServicesHolder).updatePaymentStatus(TEST_PAYMENT_ID, TEST_TRANSACTION_STATUS_SUCCESS);
    }

    @Test
    void doScaPsuIdentified_authorisation_request_availableScaMethods_exemption_payment_execution_failure() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        SpiAvailableScaMethodsResponse response = buildSingleScaMethodsResponse();
        response.setScaExempted(true);
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(response)
                                                                                                     .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                                                 .build());
        when(spiService.mapToErrorHolder(any(), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaPsuIdentified_authorisation_no_sca_payment_execution_failure() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(new SpiAvailableScaMethodsResponse(false, Collections.emptyList()))
                                                                                                     .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                                                 .build());
        when(spiService.mapToErrorHolder(any(), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaPsuIdentified_authorisation_one_sca_decoupled_chosen_success() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        SpiAvailableScaMethodsResponse response = buildSingleScaMethodsResponse();
        response.getAvailableScaMethods().get(0).setDecoupled(true);
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(response)
                                                                                                     .build());

        // When
        pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        verify(paymentServicesHolder).proceedDecoupledInitiation(any(), any(), any());
    }

    @Test
    void doScaPsuIdentified_authorisation_one_sca_requestAuthorisationCode_failure() {
        // Given
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(buildSingleScaMethodsResponse())
                                                                                                     .build());
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), any(), any())).thenReturn(SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                                                          .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                                                          .build());
        when(spiService.mapToErrorHolder(any(), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaPsuIdentified_authorisation_request_requestAuthorisationCode_exemption_success() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = new PisCommonPaymentResponse();

        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(commonPaymentResponse));
        when(pisMappersHolder.mapToSpiPayment(any(PisCommonPaymentResponse.class))).thenReturn(TEST_SPI_SINGLE_PAYMENT);
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(buildSingleScaMethodsResponse())
                                                                                                     .build());
        SpiAuthorizationCodeResult result = buildSpiAuthorizationCodeResult();
        result.setScaExempted(true);
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), any(), any())).thenReturn(SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                                                          .payload(result)
                                                                                                          .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .payload(buildSpiPaymentExecutionResponse(TEST_TRANSACTION_STATUS_SUCCESS))
                                                                                                 .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.EXEMPTED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(paymentServicesHolder).updatePaymentStatus(TEST_PAYMENT_ID, TEST_TRANSACTION_STATUS_SUCCESS);
    }

    @Test
    void doScaPsuIdentified_authorisation_request_requestAuthorisationCode_exemption_payment_execution_failure() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = new PisCommonPaymentResponse();

        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(commonPaymentResponse));
        when(pisMappersHolder.mapToSpiPayment(commonPaymentResponse))
            .thenReturn(TEST_SPI_SINGLE_PAYMENT);
        when(paymentAuthorisationSpi.authorisePsu(any(), any(), any(), any(), any(), any())).thenReturn(SpiResponse.<SpiPsuAuthorisationResponse>builder()
                                                                                                            .payload(new SpiPsuAuthorisationResponse(false, SpiAuthorisationStatus.SUCCESS))
                                                                                                            .build());
        when(paymentAuthorisationSpi.requestAvailableScaMethods(any(), any(), any())).thenReturn(SpiResponse.<SpiAvailableScaMethodsResponse>builder()
                                                                                                     .payload(buildSingleScaMethodsResponse())
                                                                                                     .build());
        SpiAuthorizationCodeResult result = buildSpiAuthorizationCodeResult();
        result.setScaExempted(true);
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), any(), any())).thenReturn(SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                                                          .payload(result)
                                                                                                          .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), any(), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                 .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                                                 .build());
        when(spiService.mapToErrorHolder(any(), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaPsuIdentified_identification_success() {
        // Given
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(buildIdentificationAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.PSUIDENTIFIED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
    }

    @Test
    void doScaPsuIdentified_identification_no_psu_failure() {
        // Given
        AuthorisationProcessorRequest authorisationProcessorRequest = buildIdentificationAuthorisationProcessorRequest();
        ((Xs2aUpdatePisCommonPaymentPsuDataRequest) authorisationProcessorRequest.getUpdateAuthorisationRequest()).setPsuData(null);

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuIdentified(authorisationProcessorRequest);

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
    }

    @Test
    void doScaPsuAuthenticated_embedded_success() {
        // Given
        AuthorisationProcessorRequest processorRequest = buildAuthorisationProcessorRequest();
        PisCommonPaymentResponse commonPaymentResponse = new PisCommonPaymentResponse();

        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(commonPaymentResponse));
        when(xs2aAuthorisationService.isAuthenticationMethodDecoupled(eq(TEST_AUTHORISATION_ID), any())).thenReturn(false);
        when(pisMappersHolder.mapToSpiPayment(commonPaymentResponse))
            .thenReturn(TEST_SPI_SINGLE_PAYMENT);
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), any(), any())).thenReturn(SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                                                          .payload(buildSpiAuthorizationCodeResult())
                                                                                                          .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        PisCommonPaymentResponse pisCommonPaymentResponse = new PisCommonPaymentResponse();
        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(pisCommonPaymentResponse));
        SpiSinglePayment spiPayment = new SpiSinglePayment("sepa-credit-transfers");
        spiPayment.setPaymentId(TEST_PAYMENT_ID);
        when(pisMappersHolder.mapToSpiPayment(pisCommonPaymentResponse)).thenReturn(spiPayment);

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuAuthenticated(processorRequest);

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.SCAMETHODSELECTED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
    }

    @Test
    void doScaPsuAuthenticated_decoupled_success() {
        // Given
        when(xs2aAuthorisationService.isAuthenticationMethodDecoupled(eq(TEST_AUTHORISATION_ID), any())).thenReturn(true);

        // When
        pisAuthorisationProcessorService.doScaPsuAuthenticated(buildAuthorisationProcessorRequest());

        // Then
        verify(xs2aAuthorisationService).updateScaApproach(TEST_AUTHORISATION_ID, ScaApproach.DECOUPLED);
        verify(paymentServicesHolder).proceedDecoupledInitiation(any(), any(), any());
    }

    @Test
    void doScaPsuAuthenticated_embedded_spi_hasError_failure() {
        // Given
        when(xs2aAuthorisationService.isAuthenticationMethodDecoupled(eq(TEST_AUTHORISATION_ID), any())).thenReturn(false);
        SpiResponse<SpiAuthorizationCodeResult> spiResponse = SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                  .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                  .build();
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), any(), any())).thenReturn(spiResponse);
        when(spiService.mapToErrorHolder(eq(spiResponse), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400)
                                                                                 .tppMessages(TppMessageInformation.of(MessageErrorCode.FORMAT_ERROR))
                                                                                 .build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuAuthenticated(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaPsuAuthenticated_embedded_empty_result_failure() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = new PisCommonPaymentResponse();
        when(xs2aAuthorisationService.isAuthenticationMethodDecoupled(eq(TEST_AUTHORISATION_ID), any())).thenReturn(false);

        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(commonPaymentResponse));
        when(pisMappersHolder.mapToSpiPayment(commonPaymentResponse))
            .thenReturn(TEST_SPI_SINGLE_PAYMENT);
        SpiResponse<SpiAuthorizationCodeResult> spiResponse = SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                  .payload(buildEmptySpiAuthorizationCodeResult())
                                                                  .build();
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), any(), any())).thenReturn(spiResponse);

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuAuthenticated(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        ErrorHolder errorHolder = actual.getErrorHolder();
        TppMessageInformation tppMessageInformation = errorHolder.getTppMessageInformationList().get(0);
        assertNotNull(errorHolder);
        assertThat(errorHolder.getErrorType()).isEqualTo(PIS_400);
        assertThat(tppMessageInformation.getCategory()).isEqualTo(MessageCategory.ERROR);
        assertThat(tppMessageInformation.getMessageErrorCode()).isEqualTo(MessageErrorCode.FORMAT_ERROR);
    }

    @Test
    void doScaPsuAuthenticated_embedded_sca_exemption_success() {
        // Given
        when(xs2aAuthorisationService.isAuthenticationMethodDecoupled(eq(TEST_AUTHORISATION_ID), any())).thenReturn(false);
        PisCommonPaymentResponse commonPaymentResponse = new PisCommonPaymentResponse();

        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(commonPaymentResponse));
        when(pisMappersHolder.mapToSpiPayment(commonPaymentResponse))
            .thenReturn(TEST_SPI_SINGLE_PAYMENT);
        SpiAuthorizationCodeResult spiAuthorizationCodeResult = buildSpiAuthorizationCodeResult();
        spiAuthorizationCodeResult.setScaExempted(true);
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), eq(TEST_SPI_SINGLE_PAYMENT), any())).thenReturn(SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                                                                                .payload(spiAuthorizationCodeResult)
                                                                                                                                .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), eq(TEST_SPI_SINGLE_PAYMENT), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                                       .payload(buildSpiPaymentExecutionResponse(TEST_TRANSACTION_STATUS_SUCCESS))
                                                                                                                       .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuAuthenticated(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.EXEMPTED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(paymentServicesHolder).updatePaymentStatus(TEST_PAYMENT_ID, TEST_TRANSACTION_STATUS_SUCCESS);
    }

    @Test
    void doScaPsuAuthenticated_embedded_sca_exemption_payment_execution_failure() {
        // Given
        when(xs2aAuthorisationService.isAuthenticationMethodDecoupled(eq(TEST_AUTHORISATION_ID), any())).thenReturn(false);
        PisCommonPaymentResponse commonPaymentResponse = new PisCommonPaymentResponse();

        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(commonPaymentResponse));
        when(pisMappersHolder.mapToSpiPayment(commonPaymentResponse))
            .thenReturn(TEST_SPI_SINGLE_PAYMENT);
        SpiAuthorizationCodeResult spiAuthorizationCodeResult = buildSpiAuthorizationCodeResult();
        spiAuthorizationCodeResult.setScaExempted(true);
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), eq(TEST_SPI_SINGLE_PAYMENT), any())).thenReturn(SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                                                                                .payload(spiAuthorizationCodeResult)
                                                                                                                                .build());
        SpiResponse<SpiPaymentExecutionResponse> spiResponse = SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                   .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                   .build();
        when(paymentServicesHolder.executePaymentWithoutSca(any(), eq(TEST_SPI_SINGLE_PAYMENT), any())).thenReturn(spiResponse);
        when(spiService.mapToErrorHolder(eq(spiResponse), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400).build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuAuthenticated(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaPsuAuthenticated_embedded_sca_exemption_multilevel_status_success() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = new PisCommonPaymentResponse();
        when(xs2aAuthorisationService.isAuthenticationMethodDecoupled(eq(TEST_AUTHORISATION_ID), any())).thenReturn(false);
        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(commonPaymentResponse));
        when(pisMappersHolder.mapToSpiPayment(any(PisCommonPaymentResponse.class))).thenReturn(TEST_SPI_SINGLE_PAYMENT);
        SpiAuthorizationCodeResult spiAuthorizationCodeResult = buildSpiAuthorizationCodeResult();
        spiAuthorizationCodeResult.setScaExempted(true);
        when(paymentAuthorisationSpi.requestAuthorisationCode(any(), any(), eq(TEST_SPI_SINGLE_PAYMENT), any())).thenReturn(SpiResponse.<SpiAuthorizationCodeResult>builder()
                                                                                                                                .payload(spiAuthorizationCodeResult)
                                                                                                                                .build());
        when(paymentServicesHolder.executePaymentWithoutSca(any(), eq(TEST_SPI_SINGLE_PAYMENT), any())).thenReturn(SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                                                                       .payload(buildSpiPaymentExecutionResponse(TEST_TRANSACTION_STATUS_MULTILEVEL_SCA))
                                                                                                                       .build());
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaPsuAuthenticated(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.EXEMPTED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(paymentServicesHolder).updatePaymentStatus(TEST_PAYMENT_ID, TEST_TRANSACTION_STATUS_MULTILEVEL_SCA);
        verify(paymentServicesHolder).updateMultilevelSca(TEST_PAYMENT_ID, true);
    }

    @Test
    void doScaMethodSelected_success() {
        // Given
        SpiResponse<SpiPaymentExecutionResponse> spiResponse = SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                   .payload(buildSpiPaymentExecutionResponse(TEST_TRANSACTION_STATUS_SUCCESS))
                                                                   .build();
        when(paymentServicesHolder.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any())).thenReturn(spiResponse);
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        PisCommonPaymentResponse pisCommonPaymentResponse = new PisCommonPaymentResponse();
        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(pisCommonPaymentResponse));
        SpiSinglePayment spiPayment = new SpiSinglePayment("sepa-credit-transfers");
        spiPayment.setPaymentId(TEST_PAYMENT_ID);
        when(pisMappersHolder.mapToSpiPayment(pisCommonPaymentResponse)).thenReturn(spiPayment);

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaMethodSelected(buildAuthorisationProcessorRequest());

        //Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.FINALISED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(paymentServicesHolder).updatePaymentStatus(TEST_PAYMENT_ID, TEST_TRANSACTION_STATUS_SUCCESS);
    }

    @Test
    void doScaMethodSelected_multilevel_sca_success() {
        // Given
        SpiResponse<SpiPaymentExecutionResponse> spiResponse = SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                   .payload(buildSpiPaymentExecutionResponse(TEST_TRANSACTION_STATUS_MULTILEVEL_SCA))
                                                                   .build();
        when(paymentServicesHolder.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any())).thenReturn(spiResponse);

        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        PisCommonPaymentResponse pisCommonPaymentResponse = new PisCommonPaymentResponse();
        when(paymentServicesHolder.getPisCommonPaymentById(TEST_PAYMENT_ID)).thenReturn(Optional.of(pisCommonPaymentResponse));
        SpiSinglePayment spiPayment = new SpiSinglePayment("sepa-credit-transfers");
        spiPayment.setPaymentId(TEST_PAYMENT_ID);
        when(pisMappersHolder.mapToSpiPayment(pisCommonPaymentResponse)).thenReturn(spiPayment);

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaMethodSelected(buildAuthorisationProcessorRequest());

        //Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.FINALISED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(paymentServicesHolder).updateMultilevelSca(TEST_PAYMENT_ID, true);
    }

    @Test
    void doScaMethodSelected_verifySca_fail_failure() {
        // Given
        SpiResponse<SpiPaymentExecutionResponse> spiResponse = SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                   .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                   .build();
        when(paymentServicesHolder.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any())).thenReturn(spiResponse);
        when(spiService.mapToErrorHolder(eq(spiResponse), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400)
                                                                                 .tppMessages(TppMessageInformation.of(MessageErrorCode.FORMAT_ERROR))
                                                                                 .build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaMethodSelected(buildAuthorisationProcessorRequest());

        //Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaMethodSelected_verifySca_fail_attemptFailure() {
        // Given
        SpiResponse<SpiPaymentExecutionResponse> spiResponse = SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                   .payload(new SpiPaymentExecutionResponse(SpiAuthorisationStatus.ATTEMPT_FAILURE))
                                                                   .error(new TppMessage(MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"))
                                                                   .build();
        when(paymentServicesHolder.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any())).thenReturn(spiResponse);
        when(spiService.mapToErrorHolder(eq(spiResponse), any())).thenReturn(ErrorHolder.builder(TEST_ERROR_TYPE_400)
                                                                                 .tppMessages(TppMessageInformation.of(MessageErrorCode.FORMAT_ERROR))
                                                                                 .build());

        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaMethodSelected(buildAuthorisationProcessorRequest());

        //Then
        assertNotNull(actual);
        assertNotNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.RECEIVED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
        verify(spiService).mapToErrorHolder(any(), any());
    }

    @Test
    void doScaExempted_success() {
        // Given
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaExempted(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.EXEMPTED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
    }

    @Test
    void doScaStarted_success() {
        AuthorisationProcessorRequest authorisationProcessorRequest = buildEmptyAuthorisationProcessorRequest();
        assertThrows(UnsupportedOperationException.class,
                     () -> pisAuthorisationProcessorService.doScaStarted(authorisationProcessorRequest));
    }

    @Test
    void doScaFinalised_success() {
        // Given
        SpiAmount spiAmount = new SpiAmount(Currency.getInstance("EUR"), BigDecimal.valueOf(34));
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = new SpiCurrencyConversionInfo(spiAmount, spiAmount, spiAmount, spiAmount);
        when(paymentServicesHolder.getCurrencyConversionInfo(any(), any(), any(), any()))
            .thenReturn(SpiResponse.<SpiCurrencyConversionInfo>builder()
                            .payload(spiCurrencyConversionInfo)
                            .build());
        // When
        AuthorisationProcessorResponse actual = pisAuthorisationProcessorService.doScaFinalised(buildAuthorisationProcessorRequest());

        // Then
        assertNotNull(actual);
        assertNull(actual.getErrorHolder());
        assertTrue(actual instanceof Xs2aUpdatePisCommonPaymentPsuDataResponse);
        assertThat(actual.getScaStatus()).isEqualTo(ScaStatus.FINALISED);
        assertThat(actual.getAuthorisationId()).isEqualTo(TEST_AUTHORISATION_ID);
        assertThat(actual.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
        assertThat(actual.getPsuData()).isEqualTo(TEST_PSU_DATA);
    }

    @Test
    void doScaFailed_success() {
        AuthorisationProcessorRequest authorisationProcessorRequest = buildEmptyAuthorisationProcessorRequest();

        assertThrows(UnsupportedOperationException.class,
                     () -> pisAuthorisationProcessorService.doScaStarted(authorisationProcessorRequest));
    }

    private AuthorisationProcessorRequest buildEmptyAuthorisationProcessorRequest() {
        return new PisAuthorisationProcessorRequest(null,
                                                    null,
                                                    null,
                                                    null);
    }

    private AuthorisationProcessorRequest buildAuthorisationProcessorRequest() {
        Xs2aUpdatePisCommonPaymentPsuDataRequest request = new Xs2aUpdatePisCommonPaymentPsuDataRequest();
        request.setPaymentId(TEST_PAYMENT_ID);
        request.setAuthorisationId(TEST_AUTHORISATION_ID);
        request.setPsuData(TEST_PSU_DATA);
        request.setPaymentService(PaymentType.SINGLE);
        request.setPaymentProduct(TEST_PAYMENT_PRODUCT);
        Authorisation authorisation = new Authorisation();
        return new PisAuthorisationProcessorRequest(TEST_SCA_APPROACH,
                                                    TEST_SCA_STATUS,
                                                    request,
                                                    authorisation);
    }

    private AuthorisationProcessorRequest buildIdentificationAuthorisationProcessorRequest() {
        AuthorisationProcessorRequest authorisationProcessorRequest = buildAuthorisationProcessorRequest();
        ((Xs2aUpdatePisCommonPaymentPsuDataRequest) authorisationProcessorRequest.getUpdateAuthorisationRequest()).setUpdatePsuIdentification(true);
        return authorisationProcessorRequest;
    }

    private SpiAuthorizationCodeResult buildSpiAuthorizationCodeResult() {
        SpiAuthorizationCodeResult spiAuthorizationCodeResult = new SpiAuthorizationCodeResult();
        AuthenticationObject method = new AuthenticationObject();
        method.setAuthenticationMethodId(TEST_AUTHENTICATION_METHOD_ID);
        method.setAuthenticationType(TEST_AUTHENTICATION_TYPE);
        spiAuthorizationCodeResult.setSelectedScaMethod(method);
        spiAuthorizationCodeResult.setChallengeData(buildChallengeData());
        return spiAuthorizationCodeResult;
    }

    private ChallengeData buildChallengeData() {
        return new ChallengeData(null, Collections.singletonList("some data"), "some link", 100, null, "info");
    }

    private SpiAuthorizationCodeResult buildEmptySpiAuthorizationCodeResult() {
        return new SpiAuthorizationCodeResult();
    }

    private SpiPaymentExecutionResponse buildSpiPaymentExecutionResponse(TransactionStatus status) {
        return new SpiPaymentExecutionResponse(status);
    }

    private AuthorisationProcessorResponse buildAuthorisationProcessorResponse() {
        return new AuthorisationProcessorResponse();
    }

    private SpiAvailableScaMethodsResponse buildMultipleScaMethodsResponse() {
        return new SpiAvailableScaMethodsResponse(false, buildSpiAuthenticationObjectList());
    }

    private List<AuthenticationObject> buildXs2aAuthenticationObjectList() {
        List<AuthenticationObject> authenticationObjects = new ArrayList<>();
        AuthenticationObject sms = new AuthenticationObject();
        sms.setAuthenticationType("SMS_OTP");
        sms.setAuthenticationMethodId("sms");
        authenticationObjects.add(sms);
        AuthenticationObject push = new AuthenticationObject();
        push.setAuthenticationType("PUSH_OTP");
        push.setAuthenticationMethodId("push");
        push.setDecoupled(true);
        authenticationObjects.add(push);
        return authenticationObjects;
    }

    private List<AuthenticationObject> buildSpiAuthenticationObjectList() {
        List<AuthenticationObject> spiAuthenticationObjects = new ArrayList<>();
        AuthenticationObject sms = new AuthenticationObject();
        sms.setAuthenticationType("SMS_OTP");
        sms.setAuthenticationMethodId("sms");
        spiAuthenticationObjects.add(sms);
        AuthenticationObject push = new AuthenticationObject();
        push.setAuthenticationType("PUSH_OTP");
        push.setAuthenticationMethodId("push");
        push.setDecoupled(true);
        spiAuthenticationObjects.add(push);
        return spiAuthenticationObjects;
    }

    private SpiAvailableScaMethodsResponse buildSingleScaMethodsResponse() {
        return new SpiAvailableScaMethodsResponse(false, buildSpiAuthenticationObjectSingleValueList());
    }

    private AuthenticationObject buildAuthenticationObject() {
        AuthenticationObject sms = new AuthenticationObject();
        sms.setAuthenticationType("SMS_OTP");
        sms.setAuthenticationMethodId("sms");
        return sms;
    }

    private List<AuthenticationObject> buildSpiAuthenticationObjectSingleValueList() {
        List<AuthenticationObject> spiAuthenticationObjects = new ArrayList<>();
        AuthenticationObject sms = new AuthenticationObject();
        sms.setAuthenticationType("SMS_OTP");
        sms.setAuthenticationMethodId("sms");
        spiAuthenticationObjects.add(sms);
        return spiAuthenticationObjects;
    }
}
