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

package de.adorsys.psd2.consent.service.aspsp;

import de.adorsys.psd2.consent.domain.payment.PisCommonPaymentData;
import de.adorsys.psd2.consent.service.CommonPaymentDataService;
import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AspspPaymentServiceInternalTest {
    private static final String DEFAULT_SERVICE_INSTANCE_ID = "UNDEFINED";
    private static final String PAYMENT_ID = "payment id";
    private static final Xs2aTransactionStatus TRANSACTION_STATUS = Xs2aTransactionStatus.RJCT;

    private PisCommonPaymentData pisCommonPaymentData;

    @InjectMocks
    private AspspPaymentServiceInternal aspspPaymentServiceInternal;

    @Mock
    private CommonPaymentDataService commonPaymentDataService;

    @BeforeEach
    void setUp() {
        pisCommonPaymentData = buildPisCommonPaymentData();
    }

    @Test
    void updatePaymentStatus_Success() {
        // Given
        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID)).thenReturn(Optional.of(pisCommonPaymentData));
        when(commonPaymentDataService.updateStatusInPaymentData(pisCommonPaymentData, TRANSACTION_STATUS)).thenReturn(true);

        // When
        boolean actualResponse = aspspPaymentServiceInternal.updatePaymentStatus(PAYMENT_ID, TRANSACTION_STATUS, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertTrue(actualResponse);
    }

    @Test
    void updatePaymentStatus_Error() {
        // Given
        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID)).thenReturn(Optional.empty());

        // When
        boolean actualResponse = aspspPaymentServiceInternal.updatePaymentStatus(PAYMENT_ID, TRANSACTION_STATUS, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertFalse(actualResponse);
    }

    private PisCommonPaymentData buildPisCommonPaymentData() {
        PisCommonPaymentData pisCommonPaymentData = new PisCommonPaymentData();
        pisCommonPaymentData.setTransactionStatus(Xs2aTransactionStatus.RCVD);
        pisCommonPaymentData.setPaymentType(PaymentType.SINGLE);
        pisCommonPaymentData.setPaymentId(PAYMENT_ID);
        pisCommonPaymentData.setCreationTimestamp(OffsetDateTime.of(2018, 10, 10, 10, 10, 10, 10, ZoneOffset.UTC));
        return pisCommonPaymentData;
    }
}
