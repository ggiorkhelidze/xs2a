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

package de.adorsys.psd2.consent.web.aspsp.controller;

import de.adorsys.psd2.consent.aspsp.api.pis.AspspPaymentService;
import de.adorsys.psd2.consent.web.aspsp.config.ObjectMapperTestConfig;
import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class CmsAspspPisTransactionControllerTest {
    private final String PAYMENT_ID = "paymentID";
    private final String INSTANCE_ID = "UNDEFINED";
    private final Xs2aTransactionStatus TRANSACTION_STATUS = Xs2aTransactionStatus.ACCC;
    private final String TRANSACTION_STATUS_NAME = TRANSACTION_STATUS.name();
    private final String UPDATE_PAYMENT_STATUS_URL = "/aspsp-api/v1/pis/transaction-status/paymentID/status/{transaction-status}";

    private MockMvc mockMvc;
    private HttpHeaders httpHeaders = new HttpHeaders();

    @Mock
    private AspspPaymentService aspspPaymentService;

    @InjectMocks
    private CmsAspspPisTransactionController cmsAspspPisTransactionController;

    @BeforeEach
    void setUp() {
        httpHeaders.add("instance-id", INSTANCE_ID);

        ObjectMapperTestConfig objectMapperTestConfig = new ObjectMapperTestConfig();

        mockMvc = MockMvcBuilders.standaloneSetup(cmsAspspPisTransactionController)
                      .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapperTestConfig.getXs2aObjectMapper()))
                      .build();
    }

    @Test
    void updatePaymentStatus() throws Exception {
        when(aspspPaymentService.updatePaymentStatus(PAYMENT_ID, TRANSACTION_STATUS, INSTANCE_ID)).thenReturn(true);

        mockMvc.perform(put(UPDATE_PAYMENT_STATUS_URL, TRANSACTION_STATUS_NAME)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .headers(httpHeaders))
            .andExpect(status().is(HttpStatus.OK.value()));

        verify(aspspPaymentService).updatePaymentStatus(PAYMENT_ID, TRANSACTION_STATUS, INSTANCE_ID);
    }

    @Test
    void updatePaymentStatus_serviceError() throws Exception {
        when(aspspPaymentService.updatePaymentStatus(PAYMENT_ID, TRANSACTION_STATUS, INSTANCE_ID)).thenReturn(false);

        mockMvc.perform(put(UPDATE_PAYMENT_STATUS_URL, TRANSACTION_STATUS_NAME)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .headers(httpHeaders))
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        verify(aspspPaymentService).updatePaymentStatus(PAYMENT_ID, TRANSACTION_STATUS, INSTANCE_ID);
    }

    @Test
    void updatePaymentStatus_wrongStatus() throws Exception {
        String invalidStatusName = "invalidStatus";

        mockMvc.perform(put(UPDATE_PAYMENT_STATUS_URL, invalidStatusName)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .headers(httpHeaders))
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        verify(aspspPaymentService, never()).updatePaymentStatus(any(), any(), any());
    }
}
