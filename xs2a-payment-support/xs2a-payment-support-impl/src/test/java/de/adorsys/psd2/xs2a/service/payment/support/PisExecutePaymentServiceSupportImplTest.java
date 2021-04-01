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

package de.adorsys.psd2.xs2a.service.payment.support;

import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.service.payment.support.mapper.spi.SpiPaymentMapper;
import de.adorsys.psd2.xs2a.service.profile.StandardPaymentProductsResolver;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiScaConfirmation;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiBulkPayment;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiPaymentInfo;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiPeriodicPayment;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiSinglePayment;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiPaymentExecutionResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.BulkPaymentSpi;
import de.adorsys.psd2.xs2a.spi.service.CommonPaymentSpi;
import de.adorsys.psd2.xs2a.spi.service.PeriodicPaymentSpi;
import de.adorsys.psd2.xs2a.spi.service.SinglePaymentSpi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PisExecutePaymentServiceSupportImplTest {
    private static final String STANDARD_PAYMENT_PRODUCT = "sepa-credit-transfers";
    private static final String RAW_PAYMENT_PRODUCT = "raw-product";
    private static final SpiContextData SPI_CONTEXT_DATA = TestSpiDataProvider.defaultSpiContextData();
    private static final SpiSinglePayment SPI_SINGLE_PAYMENT = new SpiSinglePayment(STANDARD_PAYMENT_PRODUCT);
    private static final SpiPeriodicPayment SPI_PERIODIC_PAYMENT = new SpiPeriodicPayment(STANDARD_PAYMENT_PRODUCT);
    private static final SpiBulkPayment SPI_BULK_PAYMENT = new SpiBulkPayment();
    private static final SpiScaConfirmation SPI_SCA_CONFIRMATION = new SpiScaConfirmation();
    private static final SpiPaymentExecutionResponse SPI_PAYMENT_EXECUTION_RESPONSE =
        new SpiPaymentExecutionResponse(Xs2aTransactionStatus.ACCP);

    @Mock
    private StandardPaymentProductsResolver standardPaymentProductsResolver;
    @Mock
    private CommonPaymentSpi commonPaymentSpi;
    @Mock
    private SinglePaymentSpi singlePaymentSpi;
    @Mock
    private PeriodicPaymentSpi periodicPaymentSpi;
    @Mock
    private BulkPaymentSpi bulkPaymentSpi;
    @Mock
    private SpiPaymentMapper spiPaymentMapper;

    @Mock
    private SpiAspspConsentDataProvider mockSpiAspspConsentDataProvider;

    @InjectMocks
    private PisExecutePaymentServiceSupportImpl pisExecutePaymentServiceSupport;

    @Test
    void verifyScaAuthorisationAndExecutePayment_raw() {
        // Given
        when(standardPaymentProductsResolver.isRawPaymentProduct(RAW_PAYMENT_PRODUCT)).thenReturn(true);

        SpiPaymentInfo rawSpiPayment = buildSpiPaymentInfo(RAW_PAYMENT_PRODUCT, PaymentType.SINGLE);

        SpiResponse<SpiPaymentExecutionResponse> expectedResponse = buildSpiResponse();
        when(commonPaymentSpi.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(SPI_CONTEXT_DATA, SPI_SCA_CONFIRMATION, rawSpiPayment, mockSpiAspspConsentDataProvider))
            .thenReturn(expectedResponse);

        // When
        SpiResponse<SpiPaymentExecutionResponse> spiConfirmationCodeCheckingResponseSpiResponse =
            pisExecutePaymentServiceSupport.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(SPI_CONTEXT_DATA, SPI_SCA_CONFIRMATION, rawSpiPayment, mockSpiAspspConsentDataProvider);

        // Then
        assertEquals(expectedResponse, spiConfirmationCodeCheckingResponseSpiResponse);

        verify(singlePaymentSpi, never()).verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any());
        verify(periodicPaymentSpi, never()).verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any());
        verify(bulkPaymentSpi, never()).verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any());
    }

    @Test
    void verifyScaAuthorisationAndExecutePayment_single() {
        // Given
        SpiPaymentInfo standardSpiPayment = buildSpiPaymentInfo(STANDARD_PAYMENT_PRODUCT, PaymentType.SINGLE);

        when(spiPaymentMapper.mapToSpiSinglePayment(standardSpiPayment)).thenReturn(SPI_SINGLE_PAYMENT);

        SpiResponse<SpiPaymentExecutionResponse> expectedResponse = buildSpiResponse();
        when(singlePaymentSpi.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(SPI_CONTEXT_DATA, SPI_SCA_CONFIRMATION, SPI_SINGLE_PAYMENT, mockSpiAspspConsentDataProvider))
            .thenReturn(expectedResponse);

        // When
        SpiResponse<SpiPaymentExecutionResponse> spiConfirmationCodeCheckingResponseSpiResponse =
            pisExecutePaymentServiceSupport.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(SPI_CONTEXT_DATA, SPI_SCA_CONFIRMATION, standardSpiPayment, mockSpiAspspConsentDataProvider);

        // Then
        assertEquals(expectedResponse, spiConfirmationCodeCheckingResponseSpiResponse);

        verify(commonPaymentSpi, never()).verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any());
        verify(periodicPaymentSpi, never()).verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any());
        verify(bulkPaymentSpi, never()).verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any());
    }

    @Test
    void verifyScaAuthorisationAndExecutePayment_periodic() {
        // Given
        SpiPaymentInfo standardSpiPayment = buildSpiPaymentInfo(STANDARD_PAYMENT_PRODUCT, PaymentType.PERIODIC);

        when(spiPaymentMapper.mapToSpiPeriodicPayment(standardSpiPayment)).thenReturn(SPI_PERIODIC_PAYMENT);

        SpiResponse<SpiPaymentExecutionResponse> expectedResponse = buildSpiResponse();
        when(periodicPaymentSpi.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(SPI_CONTEXT_DATA, SPI_SCA_CONFIRMATION, SPI_PERIODIC_PAYMENT, mockSpiAspspConsentDataProvider))
            .thenReturn(expectedResponse);

        // When
        SpiResponse<SpiPaymentExecutionResponse> spiConfirmationCodeCheckingResponseSpiResponse =
            pisExecutePaymentServiceSupport.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(SPI_CONTEXT_DATA, SPI_SCA_CONFIRMATION, standardSpiPayment, mockSpiAspspConsentDataProvider);

        // Then
        assertEquals(expectedResponse, spiConfirmationCodeCheckingResponseSpiResponse);

        verify(commonPaymentSpi, never()).verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any());
        verify(singlePaymentSpi, never()).verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any());
        verify(bulkPaymentSpi, never()).verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any());
    }

    @Test
    void verifyScaAuthorisationAndExecutePayment_bulk() {
        // Given
        SpiPaymentInfo standardSpiPayment = buildSpiPaymentInfo(STANDARD_PAYMENT_PRODUCT, PaymentType.BULK);

        when(spiPaymentMapper.mapToSpiBulkPayment(standardSpiPayment)).thenReturn(SPI_BULK_PAYMENT);

        SpiScaConfirmation spiScaConfirmation = new SpiScaConfirmation();
        SpiResponse<SpiPaymentExecutionResponse> expectedResponse = buildSpiResponse();
        when(bulkPaymentSpi.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(SPI_CONTEXT_DATA, spiScaConfirmation, SPI_BULK_PAYMENT, mockSpiAspspConsentDataProvider))
            .thenReturn(expectedResponse);

        // When
        SpiResponse<SpiPaymentExecutionResponse> spiConfirmationCodeCheckingResponseSpiResponse =
            pisExecutePaymentServiceSupport.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(SPI_CONTEXT_DATA, spiScaConfirmation, standardSpiPayment, mockSpiAspspConsentDataProvider);

        // Then
        assertEquals(expectedResponse, spiConfirmationCodeCheckingResponseSpiResponse);

        verify(commonPaymentSpi, never()).verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any());
        verify(singlePaymentSpi, never()).verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any());
        verify(periodicPaymentSpi, never()).verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(any(), any(), any(), any());
    }

    @Test
    void executePaymentWithoutSca_raw() {
        // Given
        when(standardPaymentProductsResolver.isRawPaymentProduct(RAW_PAYMENT_PRODUCT)).thenReturn(true);

        SpiPaymentInfo rawSpiPayment = buildSpiPaymentInfo(RAW_PAYMENT_PRODUCT, PaymentType.SINGLE);

        SpiResponse<SpiPaymentExecutionResponse> expectedResponse = buildSpiExecutionResponse();
        when(commonPaymentSpi.executePaymentWithoutSca(SPI_CONTEXT_DATA, rawSpiPayment, mockSpiAspspConsentDataProvider))
            .thenReturn(expectedResponse);

        // When
        SpiResponse<SpiPaymentExecutionResponse> spiConfirmationCodeCheckingResponseSpiResponse =
            pisExecutePaymentServiceSupport.executePaymentWithoutSca(SPI_CONTEXT_DATA, rawSpiPayment, mockSpiAspspConsentDataProvider);

        // Then
        assertEquals(expectedResponse, spiConfirmationCodeCheckingResponseSpiResponse);

        verify(singlePaymentSpi, never()).executePaymentWithoutSca(any(), any(), any());
        verify(periodicPaymentSpi, never()).executePaymentWithoutSca(any(), any(), any());
        verify(bulkPaymentSpi, never()).executePaymentWithoutSca(any(), any(), any());
    }

    @Test
    void executePaymentWithoutSca_single() {
        // Given
        SpiPaymentInfo standardSpiPayment = buildSpiPaymentInfo(STANDARD_PAYMENT_PRODUCT, PaymentType.SINGLE);

        when(spiPaymentMapper.mapToSpiSinglePayment(standardSpiPayment)).thenReturn(SPI_SINGLE_PAYMENT);

        SpiResponse<SpiPaymentExecutionResponse> expectedResponse = buildSpiExecutionResponse();
        when(singlePaymentSpi.executePaymentWithoutSca(SPI_CONTEXT_DATA, SPI_SINGLE_PAYMENT, mockSpiAspspConsentDataProvider))
            .thenReturn(expectedResponse);

        // When
        SpiResponse<SpiPaymentExecutionResponse> spiConfirmationCodeCheckingResponseSpiResponse =
            pisExecutePaymentServiceSupport.executePaymentWithoutSca(SPI_CONTEXT_DATA, standardSpiPayment, mockSpiAspspConsentDataProvider);

        // Then
        assertEquals(expectedResponse, spiConfirmationCodeCheckingResponseSpiResponse);

        verify(commonPaymentSpi, never()).executePaymentWithoutSca(any(), any(), any());
        verify(periodicPaymentSpi, never()).executePaymentWithoutSca(any(), any(), any());
        verify(bulkPaymentSpi, never()).executePaymentWithoutSca(any(), any(), any());
    }

    @Test
    void executePaymentWithoutSca_periodic() {
        // Given
        SpiPaymentInfo standardSpiPayment = buildSpiPaymentInfo(STANDARD_PAYMENT_PRODUCT, PaymentType.PERIODIC);

        when(spiPaymentMapper.mapToSpiPeriodicPayment(standardSpiPayment)).thenReturn(SPI_PERIODIC_PAYMENT);

        SpiResponse<SpiPaymentExecutionResponse> expectedResponse = buildSpiExecutionResponse();
        when(periodicPaymentSpi.executePaymentWithoutSca(SPI_CONTEXT_DATA, SPI_PERIODIC_PAYMENT, mockSpiAspspConsentDataProvider))
            .thenReturn(expectedResponse);

        // When
        SpiResponse<SpiPaymentExecutionResponse> spiConfirmationCodeCheckingResponseSpiResponse =
            pisExecutePaymentServiceSupport.executePaymentWithoutSca(SPI_CONTEXT_DATA, standardSpiPayment, mockSpiAspspConsentDataProvider);

        // Then
        assertEquals(expectedResponse, spiConfirmationCodeCheckingResponseSpiResponse);

        verify(commonPaymentSpi, never()).executePaymentWithoutSca(any(), any(), any());
        verify(singlePaymentSpi, never()).executePaymentWithoutSca(any(), any(), any());
        verify(bulkPaymentSpi, never()).executePaymentWithoutSca(any(), any(), any());
    }

    @Test
    void executePaymentWithoutSca_bulk() {
        // Given
        SpiPaymentInfo standardSpiPayment = buildSpiPaymentInfo(STANDARD_PAYMENT_PRODUCT, PaymentType.BULK);

        when(spiPaymentMapper.mapToSpiBulkPayment(standardSpiPayment)).thenReturn(SPI_BULK_PAYMENT);

        SpiResponse<SpiPaymentExecutionResponse> expectedResponse = buildSpiExecutionResponse();
        when(bulkPaymentSpi.executePaymentWithoutSca(SPI_CONTEXT_DATA, SPI_BULK_PAYMENT, mockSpiAspspConsentDataProvider))
            .thenReturn(expectedResponse);

        // When
        SpiResponse<SpiPaymentExecutionResponse> spiConfirmationCodeCheckingResponseSpiResponse =
            pisExecutePaymentServiceSupport.executePaymentWithoutSca(SPI_CONTEXT_DATA, standardSpiPayment, mockSpiAspspConsentDataProvider);

        // Then
        assertEquals(expectedResponse, spiConfirmationCodeCheckingResponseSpiResponse);

        verify(commonPaymentSpi, never()).executePaymentWithoutSca(any(), any(), any());
        verify(singlePaymentSpi, never()).executePaymentWithoutSca(any(), any(), any());
        verify(periodicPaymentSpi, never()).executePaymentWithoutSca(any(), any(), any());
    }

    private SpiResponse<SpiPaymentExecutionResponse> buildSpiResponse() {
        return SpiResponse.<SpiPaymentExecutionResponse>builder()
                   .payload(SPI_PAYMENT_EXECUTION_RESPONSE)
                   .build();
    }

    private SpiResponse<SpiPaymentExecutionResponse> buildSpiExecutionResponse() {
        return SpiResponse.<SpiPaymentExecutionResponse>builder()
                   .payload(SPI_PAYMENT_EXECUTION_RESPONSE)
                   .build();
    }

    private SpiPaymentInfo buildSpiPaymentInfo(String paymentProduct, PaymentType paymentType) {
        SpiPaymentInfo spiPaymentInfo = new SpiPaymentInfo(paymentProduct);
        spiPaymentInfo.setPaymentType(paymentType);
        return spiPaymentInfo;
    }
}
