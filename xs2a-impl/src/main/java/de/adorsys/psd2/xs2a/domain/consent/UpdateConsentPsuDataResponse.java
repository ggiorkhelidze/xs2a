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

package de.adorsys.psd2.xs2a.domain.consent;

import de.adorsys.psd2.xs2a.core.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.Xs2aScaStatus;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.AuthorisationProcessorResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

// Class can't be immutable, because it it used in aspect (links setting)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UpdateConsentPsuDataResponse extends AuthorisationProcessorResponse {

    public UpdateConsentPsuDataResponse(Xs2aScaStatus scaStatus, ErrorHolder errorHolder, String consentId, String authorisationId, PsuIdData psuIdData) {
        this(scaStatus, consentId, authorisationId, psuIdData);
        this.errorHolder = errorHolder;
    }

    public UpdateConsentPsuDataResponse(ErrorHolder errorHolder, String consentId, String authorisationId, PsuIdData psuIdData) {
        this(Xs2aScaStatus.FAILED, consentId, authorisationId, psuIdData);
        this.errorHolder = errorHolder;
    }

    public UpdateConsentPsuDataResponse(Xs2aScaStatus scaStatus, String consentId, String authorisationId, PsuIdData psuIdData) {
        this.scaStatus = scaStatus;
        this.consentId = consentId;
        this.authorisationId = authorisationId;
        this.psuData = psuIdData;
    }
}
