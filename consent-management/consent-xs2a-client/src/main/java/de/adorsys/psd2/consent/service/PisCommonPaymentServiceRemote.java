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
import de.adorsys.psd2.consent.api.pis.CreatePisCommonPaymentResponse;
import de.adorsys.psd2.consent.api.pis.PisCommonPaymentResponse;
import de.adorsys.psd2.consent.api.pis.proto.PisPaymentInfo;
import de.adorsys.psd2.consent.api.service.PisCommonPaymentServiceEncrypted;
import de.adorsys.psd2.consent.config.CmsRestException;
import de.adorsys.psd2.consent.config.PisCommonPaymentRemoteUrls;
import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static de.adorsys.psd2.consent.api.CmsError.TECHNICAL_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class PisCommonPaymentServiceRemote implements PisCommonPaymentServiceEncrypted {
    @Qualifier("consentRestTemplate")
    private final RestTemplate consentRestTemplate;
    private final PisCommonPaymentRemoteUrls remotePisCommonPaymentUrls;

    @Override
    public CmsResponse<CreatePisCommonPaymentResponse> createCommonPayment(PisPaymentInfo request) {
        try {
            CreatePisCommonPaymentResponse body = consentRestTemplate.postForEntity(remotePisCommonPaymentUrls.createPisCommonPayment(), request, CreatePisCommonPaymentResponse.class).getBody();
            return CmsResponse.<CreatePisCommonPaymentResponse>builder()
                       .payload(body)
                       .build();
        } catch (CmsRestException cmsRestException) {
            log.warn("Remote common payment creation failed");
        }

        return CmsResponse.<CreatePisCommonPaymentResponse>builder()
                   .error(TECHNICAL_ERROR)
                   .build();
    }

    @Override
    public CmsResponse<Xs2aTransactionStatus> getPisCommonPaymentStatusById(String paymentId) {
        return CmsResponse.<Xs2aTransactionStatus>builder()
                   .error(TECHNICAL_ERROR)
                   .build();
    }

    @Override
    public CmsResponse<PisCommonPaymentResponse> getCommonPaymentById(String paymentId) {
        try {
            PisCommonPaymentResponse body = consentRestTemplate.getForEntity(remotePisCommonPaymentUrls.getPisCommonPaymentById(), PisCommonPaymentResponse.class, paymentId).getBody();
            return CmsResponse.<PisCommonPaymentResponse>builder()
                       .payload(body)
                       .build();
        } catch (CmsRestException cmsRestException) {
            log.warn("Remote get common payment by ID failed");
        }

        return CmsResponse.<PisCommonPaymentResponse>builder()
                   .error(TECHNICAL_ERROR)
                   .build();
    }

    @Override
    public CmsResponse<Boolean> updateCommonPaymentStatusById(String paymentId, Xs2aTransactionStatus status) {
        try {
            HttpStatus statusCode = consentRestTemplate.exchange(remotePisCommonPaymentUrls.updatePisCommonPaymentStatus(), HttpMethod.PUT,
                                                                 null, Void.class, paymentId, status.getTransactionStatus()).getStatusCode();

            return CmsResponse.<Boolean>builder()
                       .payload(statusCode == HttpStatus.OK)
                       .build();
        } catch (CmsRestException cmsRestException) {
            log.warn("Remote update common payment status by ID failed");
        }

        return CmsResponse.<Boolean>builder()
                   .payload(false)
                   .build();
    }

    @Override
    public CmsResponse<String> getDecryptedId(String encryptedId) {
        try {
            String body = consentRestTemplate.getForEntity(remotePisCommonPaymentUrls.getPaymentIdByEncryptedString(), String.class, encryptedId).getBody();
            return CmsResponse.<String>builder()
                       .payload(body)
                       .build();
        } catch (CmsRestException cmsRestException) {
            log.warn("Remote decrypt encrypted common payment ID failed");
        }

        return CmsResponse.<String>builder()
                   .error(TECHNICAL_ERROR)
                   .build();
    }

    @Override
    public CmsResponse<Boolean> updateMultilevelSca(String paymentId, boolean multilevelScaRequired) {
        try {
            Boolean body = consentRestTemplate.exchange(remotePisCommonPaymentUrls.updateMultilevelScaRequired(), HttpMethod.PUT, null, Boolean.class, paymentId, multilevelScaRequired).getBody();
            return CmsResponse.<Boolean>builder()
                       .payload(body)
                       .build();
        } catch (CmsRestException cmsRestException) {
            log.info("Payment ID: [{}]. No payment could be found by given payment ID.", paymentId);
        }

        return CmsResponse.<Boolean>builder()
                   .payload(false)
                   .build();
    }

    @Override
    public CmsResponse<List<PsuIdData>> getPsuDataListByPaymentId(String paymentId) {
        try {
            PsuIdData[] body = consentRestTemplate.getForEntity(remotePisCommonPaymentUrls.getPsuDataByPaymentId(), PsuIdData[].class, paymentId).getBody();
            if (body != null) {
                return CmsResponse.<List<PsuIdData>>builder()
                           .payload(Arrays.asList(body))
                           .build();
            }
        } catch (CmsRestException cmsRestException) {
            log.warn("Remote get PSU data list by paymentId {} failed", paymentId);
        }

        return CmsResponse.<List<PsuIdData>>builder()
                   .error(TECHNICAL_ERROR)
                   .build();
    }
}
