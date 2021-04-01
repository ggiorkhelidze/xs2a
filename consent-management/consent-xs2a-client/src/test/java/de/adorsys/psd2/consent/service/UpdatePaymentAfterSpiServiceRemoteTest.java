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

import de.adorsys.psd2.consent.config.PisPaymentRemoteUrls;
import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppRedirectUri;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePaymentAfterSpiServiceRemoteTest {
    private static final String ENCRYPTED_PAYMENT_ID = "encrypted payment id";
    private static final String URL = "some url";

    @InjectMocks
    private UpdatePaymentAfterSpiServiceRemote updatePaymentAfterSpiServiceRemote;

    @Mock
    private RestTemplate consentRestTemplate;
    @Mock
    private PisPaymentRemoteUrls pisPaymentRemoteUrls;

    @Test
    void updatePaymentStatus() {
        when(pisPaymentRemoteUrls.updatePaymentStatus()).thenReturn(URL);
        when(consentRestTemplate.exchange(eq(URL), eq(HttpMethod.PUT), isNull(), eq(Void.class), eq(ENCRYPTED_PAYMENT_ID), eq(Xs2aTransactionStatus.ACSP.name())))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        assertTrue(updatePaymentAfterSpiServiceRemote.updatePaymentStatus(ENCRYPTED_PAYMENT_ID, Xs2aTransactionStatus.ACSP).getPayload());
    }

    @Test
    void updatePaymentCancellationTppRedirectUri_success() {
        ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        TppRedirectUri tppRedirectUri = new TppRedirectUri("ok_url", "nok_url");

        when(pisPaymentRemoteUrls.updatePaymentCancellationRedirectURIs()).thenReturn(URL);
        when(consentRestTemplate.exchange(eq(URL), eq(HttpMethod.PUT), httpEntityCaptor.capture(), eq(Void.class), eq(ENCRYPTED_PAYMENT_ID))).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        assertTrue(updatePaymentAfterSpiServiceRemote.updatePaymentCancellationTppRedirectUri(ENCRYPTED_PAYMENT_ID, tppRedirectUri).getPayload());

        assertEquals(2, httpEntityCaptor.getValue().getHeaders().size());
        assertEquals("ok_url", httpEntityCaptor.getValue().getHeaders().get("tpp-redirect-uri").get(0));
        assertEquals("nok_url", httpEntityCaptor.getValue().getHeaders().get("tpp-nok-redirect-uri").get(0));
    }
}
