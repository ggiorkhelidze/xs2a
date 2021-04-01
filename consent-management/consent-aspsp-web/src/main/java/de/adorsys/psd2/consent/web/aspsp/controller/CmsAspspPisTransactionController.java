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

package de.adorsys.psd2.consent.web.aspsp.controller;

import de.adorsys.psd2.consent.aspsp.api.CmsAspspPisTransactionApi;
import de.adorsys.psd2.consent.aspsp.api.pis.AspspPaymentService;
import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CmsAspspPisTransactionController implements CmsAspspPisTransactionApi {
    private final AspspPaymentService aspspPaymentService;

    @Override
    public ResponseEntity<Void> updatePaymentStatus(String paymentId, String status, String instanceId) {
        Xs2aTransactionStatus transactionStatus;
        try {
            transactionStatus = Xs2aTransactionStatus.valueOf(status);
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.badRequest().build();
        }
        return aspspPaymentService.updatePaymentStatus(paymentId, transactionStatus, instanceId)
            ? ResponseEntity.ok().build()
            : ResponseEntity.badRequest().build();
    }
}


