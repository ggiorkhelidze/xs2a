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

package de.adorsys.psd2.consent.api.pis;

import de.adorsys.psd2.xs2a.core.authorisation.Authorisation;
import de.adorsys.psd2.xs2a.core.pis.InternalPaymentStatus;
import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@ApiModel(description = "Pis payment initialisation common payment response", value = "PisCommonPaymentResponse")
public class PisCommonPaymentResponse implements CommonPaymentData {
    @ApiModelProperty(value = "Payment data", required = true)
    private List<PisPayment> payments;

    @ApiModelProperty(value = "Payment product", required = true, example = "sepa-credit-transfers")
    private String paymentProduct;

    @ApiModelProperty(value = "Payment type: BULK, SINGLE or PERIODIC.", required = true, example = "SINGLE")
    private PaymentType paymentType;

    @ApiModelProperty(value = "Tpp information", required = true)
    private TppInfo tppInfo;

    @ApiModelProperty(value = "An external exposed identification of the created common payment", required = true, example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7")
    private String externalId;

    @ApiModelProperty(value = "List of corresponding PSU", required = true)
    private List<PsuIdData> psuData;

    @ApiModelProperty(value = "Payment info")
    private byte[] paymentData;

    @ApiModelProperty(value = "Transaction status", required = true)
    private Xs2aTransactionStatus transactionStatus;

    @ApiModelProperty(value = "Internal payment status", required = true)
    private InternalPaymentStatus internalPaymentStatus;

    @ApiModelProperty(value = "Timestamp of the last payment transaction status changing")
    private OffsetDateTime statusChangeTimestamp;

    @ApiModelProperty(value = "List of corresponding PSU", required = true)
    private List<Authorisation> authorisations = new ArrayList<>();

    @ApiModelProperty(value = "Defines whether the payment requires multilevel SCA", example = "true")
    private boolean multilevelScaRequired;

    @ApiModelProperty(value = "Timestamp of the payment creation")
    private OffsetDateTime creationTimestamp;

    @ApiModelProperty(value = "Response content type")
    private String contentType;

    @ApiModelProperty(value = "Response instance id")
    private String instanceId;

    @ApiModelProperty(value = "Signing basket blocked")
    private boolean signingBasketBlocked;

    @ApiModelProperty(value = "Signing basket authorised")
    private boolean signingBasketAuthorised;

    public Optional<Authorisation> findAuthorisationInPayment(String authorisationId) {
        return authorisations.stream()
                   .filter(auth -> auth.getAuthorisationId().equals(authorisationId))
                   .findFirst();
    }
}
