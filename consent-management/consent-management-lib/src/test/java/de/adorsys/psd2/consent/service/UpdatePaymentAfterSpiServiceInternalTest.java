/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
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
import de.adorsys.psd2.consent.domain.payment.PisCommonPaymentData;
import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppRedirectUri;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdatePaymentAfterSpiServiceInternalTest {
    private static final String PAYMENT_ID = "payment id";
    private static final String INTERNAL_REQUEST_ID = "5c2d5564-367f-4e03-a621-6bef76fa4208";

    @InjectMocks
    private UpdatePaymentAfterSpiServiceInternal updatePaymentAfterSpiServiceInternal;

    @Mock
    private CommonPaymentDataService commonPaymentDataService;
    private TppRedirectUri tppRedirectUri;
    private PisCommonPaymentData pisCommonPaymentData;

    @BeforeEach
    void setUp() {
        tppRedirectUri = new TppRedirectUri("ok_url", "nok_url");
        pisCommonPaymentData = new PisCommonPaymentData();
    }

    @Test
    void updatePaymentStatus_success() {
        // Given
        pisCommonPaymentData.setTransactionStatus(Xs2aTransactionStatus.ACCP);

        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, null)).thenReturn(Optional.of(pisCommonPaymentData));
        when(commonPaymentDataService.updateStatusInPaymentData(pisCommonPaymentData, Xs2aTransactionStatus.ACSP)).thenReturn(true);

        // When
        CmsResponse<Boolean> actual = updatePaymentAfterSpiServiceInternal.updatePaymentStatus(PAYMENT_ID, Xs2aTransactionStatus.ACSP);

        // Then
        assertTrue(actual.isSuccessful());

        assertTrue(actual.getPayload());
        verify(commonPaymentDataService, times(1)).getPisCommonPaymentData(anyString(), isNull());
        verify(commonPaymentDataService, times(1)).updateStatusInPaymentData(any(PisCommonPaymentData.class), eq(Xs2aTransactionStatus.ACSP));
    }

    @Test
    void updatePaymentStatus_transactionStatusIsFinalised() {
        // Given
        pisCommonPaymentData.setTransactionStatus(Xs2aTransactionStatus.RJCT);

        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, null)).thenReturn(Optional.of(pisCommonPaymentData));

        // When
        CmsResponse<Boolean> actual = updatePaymentAfterSpiServiceInternal.updatePaymentStatus(PAYMENT_ID, Xs2aTransactionStatus.ACSP);

        // Then
        assertTrue(actual.isSuccessful());

        assertFalse(actual.getPayload());

        verify(commonPaymentDataService, times(1)).getPisCommonPaymentData(anyString(), isNull());
        verify(commonPaymentDataService, never()).updateStatusInPaymentData(any(PisCommonPaymentData.class), any(Xs2aTransactionStatus.class));
    }

    @Test
    void updatePaymentStatus_paymentDataIsEmpty() {
        // Given
        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, null)).thenReturn(Optional.empty());

        // When
        CmsResponse<Boolean> actual = updatePaymentAfterSpiServiceInternal.updatePaymentStatus(PAYMENT_ID, Xs2aTransactionStatus.ACSP);

        // Then
        assertTrue(actual.isSuccessful());

        assertFalse(actual.getPayload());
        verify(commonPaymentDataService, times(1)).getPisCommonPaymentData(anyString(), isNull());
        verify(commonPaymentDataService, never()).updateStatusInPaymentData(any(PisCommonPaymentData.class), any(Xs2aTransactionStatus.class));
    }

    @Test
    void updatePaymentCancellationTppRedirectUri_success() {
        // Given
        pisCommonPaymentData.setTransactionStatus(Xs2aTransactionStatus.ACCP);

        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, null)).thenReturn(Optional.of(pisCommonPaymentData));
        when(commonPaymentDataService.updateCancelTppRedirectURIs(pisCommonPaymentData, tppRedirectUri)).thenReturn(true);

        // When
        CmsResponse<Boolean> actual = updatePaymentAfterSpiServiceInternal.updatePaymentCancellationTppRedirectUri(PAYMENT_ID, tppRedirectUri);

        // Then
        assertTrue(actual.isSuccessful());

        assertTrue(actual.getPayload());
        verify(commonPaymentDataService, times(1)).getPisCommonPaymentData(anyString(), isNull());
        verify(commonPaymentDataService, times(1)).updateCancelTppRedirectURIs(pisCommonPaymentData, tppRedirectUri);
    }

    @Test
    void updatePaymentCancellationTppRedirectUri_transactionStatusIsFinalised() {
        // Given
        pisCommonPaymentData.setTransactionStatus(Xs2aTransactionStatus.RJCT);

        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, null)).thenReturn(Optional.of(pisCommonPaymentData));

        // When
        CmsResponse<Boolean> actual = updatePaymentAfterSpiServiceInternal.updatePaymentCancellationTppRedirectUri(PAYMENT_ID, tppRedirectUri);

        // Then
        assertTrue(actual.isSuccessful());

        assertFalse(actual.getPayload());
        verify(commonPaymentDataService, times(1)).getPisCommonPaymentData(anyString(), isNull());
        verify(commonPaymentDataService, never()).updateCancelTppRedirectURIs(any(PisCommonPaymentData.class), eq(tppRedirectUri));
    }

    @Test
    void updatePaymentCancellationTppRedirectUri_paymentDataIsEmpty() {
        // Given
        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, null)).thenReturn(Optional.empty());

        // When
        CmsResponse<Boolean> actual = updatePaymentAfterSpiServiceInternal.updatePaymentCancellationTppRedirectUri(PAYMENT_ID, tppRedirectUri);

        // Then
        assertTrue(actual.isSuccessful());

        assertFalse(actual.getPayload());
        verify(commonPaymentDataService, times(1)).getPisCommonPaymentData(anyString(), isNull());
        verify(commonPaymentDataService, never()).updateCancelTppRedirectURIs(any(PisCommonPaymentData.class), eq(tppRedirectUri));
    }

    @Test
    void updatePaymentCancellationInternalRequestId_success() {
        // Given
        pisCommonPaymentData.setTransactionStatus(Xs2aTransactionStatus.ACCP);

        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, null)).thenReturn(Optional.of(pisCommonPaymentData));
        when(commonPaymentDataService.updatePaymentCancellationInternalRequestId(pisCommonPaymentData, INTERNAL_REQUEST_ID)).thenReturn(true);

        // When
        CmsResponse<Boolean> actual = updatePaymentAfterSpiServiceInternal.updatePaymentCancellationInternalRequestId(PAYMENT_ID, INTERNAL_REQUEST_ID);

        // Then
        assertTrue(actual.isSuccessful());

        assertTrue(actual.getPayload());
        verify(commonPaymentDataService, times(1)).getPisCommonPaymentData(anyString(), isNull());
        verify(commonPaymentDataService, times(1)).updatePaymentCancellationInternalRequestId(pisCommonPaymentData, INTERNAL_REQUEST_ID);
    }
}
