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

package de.adorsys.psd2.logger.context;

import de.adorsys.psd2.xs2a.core.consent.Xs2aConsentStatus;
import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import de.adorsys.psd2.xs2a.core.sca.Xs2aScaStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class MdcLoggingContextService implements LoggingContextService {
    private static final String TRANSACTION_STATUS_KEY = "transactionStatus";
    private static final String CONSENT_STATUS_KEY = "consentStatus";
    private static final String SCA_STATUS_KEY = "scaStatus";
    private static final String INTERNAL_REQUEST_ID_KEY = "internal-request-id";
    private static final String X_REQUEST_ID_KEY = "x-request-id";
    private static final String INSTANCE_ID_KEY = "instance-id";

    @Override
    public void storeConsentStatus(@NotNull Xs2aConsentStatus consentStatus) {
        MDC.put(CONSENT_STATUS_KEY, consentStatus.getValue());
    }

    @Override
    public String getConsentStatus() {
        return MDC.get(CONSENT_STATUS_KEY);
    }

    @Override
    public void storeTransactionStatus(@NotNull Xs2aTransactionStatus transactionStatus) {
        MDC.put(TRANSACTION_STATUS_KEY, transactionStatus.getTransactionStatus());
    }

    @Override
    public String getTransactionStatus() {
        return MDC.get(TRANSACTION_STATUS_KEY);
    }

    @Override
    public void storeScaStatus(@NotNull Xs2aScaStatus scaStatus) {
        MDC.put(SCA_STATUS_KEY, scaStatus.getValue());
    }

    @Override
    public void storeTransactionAndScaStatus(@NotNull Xs2aTransactionStatus transactionStatus, @Nullable Xs2aScaStatus scaStatus) {
        storeTransactionStatus(transactionStatus);
        if (scaStatus != null) {
            storeScaStatus(scaStatus);
        }
    }

    @Override
    public String getScaStatus() {
        return MDC.get(SCA_STATUS_KEY);
    }

    @Override
    public void storeRequestInformation(RequestInfo requestInfo) {
        if (requestInfo == null) {
            return;
        }

        MDC.put(INTERNAL_REQUEST_ID_KEY, requestInfo.getInternalRequestId());
        MDC.put(X_REQUEST_ID_KEY, requestInfo.getXRequestId());
        MDC.put(INSTANCE_ID_KEY, requestInfo.getInstanceId());
    }

    @Override
    public RequestInfo getRequestInformation() {
        String internalRequestId = MDC.get(INTERNAL_REQUEST_ID_KEY);
        String xRequestId = MDC.get(X_REQUEST_ID_KEY);
        String instanceId = MDC.get(INSTANCE_ID_KEY);

        return new RequestInfo(internalRequestId, xRequestId, instanceId);
    }

    @Override
    public void clearContext() {
        MDC.clear();
    }
}
