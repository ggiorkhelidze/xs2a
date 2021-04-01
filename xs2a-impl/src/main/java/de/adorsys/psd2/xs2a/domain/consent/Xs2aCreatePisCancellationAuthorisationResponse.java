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

package de.adorsys.psd2.xs2a.domain.consent;

import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.sca.Xs2aScaStatus;
import de.adorsys.psd2.xs2a.domain.Links;
import de.adorsys.psd2.xs2a.domain.authorisation.AuthorisationResponseType;
import de.adorsys.psd2.xs2a.domain.authorisation.CancellationAuthorisationResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
public class Xs2aCreatePisCancellationAuthorisationResponse implements CancellationAuthorisationResponse {
    @NotNull
    private String authorisationId;
    private Xs2aScaStatus scaStatus;
    private PaymentType paymentType;
    private Links links = new Links();
    private String internalRequestId;

    public Xs2aCreatePisCancellationAuthorisationResponse(@NotNull String authorisationId, Xs2aScaStatus scaStatus, PaymentType paymentType, String internalRequestId) {
        this.authorisationId = authorisationId;
        this.scaStatus = scaStatus;
        this.paymentType = paymentType;
        this.internalRequestId = internalRequestId;
    }

    @NotNull
    @Override
    public AuthorisationResponseType getAuthorisationResponseType() {
        return AuthorisationResponseType.START;
    }

    @Override
    public String getInternalRequestId() {
        return internalRequestId;
    }
}
