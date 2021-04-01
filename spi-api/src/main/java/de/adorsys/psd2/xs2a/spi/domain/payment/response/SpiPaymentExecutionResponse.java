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

package de.adorsys.psd2.xs2a.spi.domain.payment.response;

import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A response object that is returned by the ASPSP after the successful execution of payment
 */
@EqualsAndHashCode(callSuper = true)
public final class SpiPaymentExecutionResponse extends SpiPaymentResponse {
    @Getter
    private Xs2aTransactionStatus transactionStatus;

    public SpiPaymentExecutionResponse(Xs2aTransactionStatus transactionStatus) {
        this(null, transactionStatus);
    }

    public SpiPaymentExecutionResponse(SpiAuthorisationStatus spiAuthorisationStatus) {
        this(spiAuthorisationStatus, null);
    }

    public SpiPaymentExecutionResponse(SpiAuthorisationStatus spiAuthorisationStatus,
                                       Xs2aTransactionStatus transactionStatus) {
        super(spiAuthorisationStatus);
        this.transactionStatus = transactionStatus;
    }
}
