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

package de.adorsys.psd2.xs2a.domain.pis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import de.adorsys.psd2.xs2a.core.authorisation.Xs2aAuthenticationObject;
import de.adorsys.psd2.xs2a.core.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.core.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import de.adorsys.psd2.xs2a.core.pis.Xs2aAmount;
import de.adorsys.psd2.xs2a.core.profile.NotificationSupportedMode;
import de.adorsys.psd2.xs2a.core.sca.Xs2aChallengeData;
import de.adorsys.psd2.xs2a.core.sca.Xs2aScaStatus;
import de.adorsys.psd2.xs2a.domain.Links;
import de.adorsys.psd2.xs2a.service.spi.InitialSpiAspspConsentDataProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
public abstract class PaymentInitiationResponse {
    private Xs2aScaStatus scaStatus;
    @JsonUnwrapped
    private Xs2aTransactionStatus transactionStatus;
    private Xs2aAmount transactionFees;
    private Boolean transactionFeeIndicator;
    private boolean multilevelScaRequired;
    private String paymentId;
    private List<Xs2aAuthenticationObject> scaMethods;
    private Xs2aChallengeData challengeData;
    private String psuMessage;
    private MessageErrorCode[] tppMessages;
    @JsonProperty("_links")
    private Links links = new Links();
    private String authorizationId;
    private InitialSpiAspspConsentDataProvider aspspConsentDataProvider;
    private String aspspAccountId;
    private ErrorHolder errorHolder;
    private String internalRequestId;
    private List<NotificationSupportedMode> tppNotificationContentPreferred;
    private Set<TppMessageInformation> tppMessageInformation;
    private Xs2aAmount currencyConversionFee;
    private Xs2aAmount estimatedTotalAmount;
    private Xs2aAmount estimatedInterbankSettlementAmount;

    PaymentInitiationResponse(ErrorHolder errorHolder) {
        this.errorHolder = errorHolder;
    }

    public boolean hasError() {
        return errorHolder != null;
    }
}
