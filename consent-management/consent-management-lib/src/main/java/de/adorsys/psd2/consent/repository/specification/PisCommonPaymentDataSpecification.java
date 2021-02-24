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

package de.adorsys.psd2.consent.repository.specification;

import de.adorsys.psd2.consent.domain.TppInfoEntity;
import de.adorsys.psd2.consent.domain.payment.PisCommonPaymentData;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Join;
import java.time.LocalDate;

import static de.adorsys.psd2.consent.repository.specification.EntityAttribute.*;
import static de.adorsys.psd2.consent.repository.specification.EntityAttributeSpecificationProvider.provideSpecificationForEntityAttribute;
import static de.adorsys.psd2.consent.repository.specification.EntityAttributeSpecificationProvider.provideSpecificationForJoinedEntityAttribute;

@Service
@RequiredArgsConstructor
public class PisCommonPaymentDataSpecification {
    private final CommonSpecification<PisCommonPaymentData> commonSpecification;

    public Specification<PisCommonPaymentData> byPaymentId(String paymentId) {
        return Specification.where(provideSpecificationForEntityAttribute(PAYMENT_ID_ATTRIBUTE, paymentId));
    }

    public Specification<PisCommonPaymentData> byPaymentIdAndInstanceId(String paymentId, String instanceId) {
        return Specification.<PisCommonPaymentData>where(provideSpecificationForEntityAttribute(PAYMENT_ID_ATTRIBUTE, paymentId))
                   .and(provideSpecificationForEntityAttribute(INSTANCE_ID_ATTRIBUTE, instanceId));
    }

    /**
     * Returns specification for PisCommonPaymentData entity for filtering payments by TPP authorisation number, creation date, PSU ID data and instance ID.
     *
     * @param tppAuthorisationNumber mandatory TPP authorisation number
     * @param createDateFrom         optional creation date that limits results to payments created after this date(inclusive)
     * @param createDateTo           optional creation date that limits results to payments created before this date(inclusive)
     * @param psuIdData              optional PSU ID data
     * @param instanceId             optional instance ID
     * @param additionalTppInfo      Additional TPP information
     * @return resulting specification for PisCommonPaymentData entity
     */
    public Specification<PisCommonPaymentData> byTppIdAndCreationPeriodAndPsuIdDataAndInstanceIdAndAdditionalTppInfo(@NotNull String tppAuthorisationNumber,
                                                                                                                     @Nullable LocalDate createDateFrom,
                                                                                                                     @Nullable LocalDate createDateTo,
                                                                                                                     @Nullable PsuIdData psuIdData,
                                                                                                                     @Nullable String instanceId,
                                                                                                                     @Nullable String additionalTppInfo) {
        return Specification.where(byTppAuthorisationNumber(tppAuthorisationNumber))
                   .and(commonSpecification.byCreationTimestamp(createDateFrom, createDateTo))
                   .and(commonSpecification.byPsuIdDataInList(psuIdData))
                   .and(commonSpecification.byInstanceId(instanceId))
                   .and(byAdditionalTppInfo(additionalTppInfo));
    }

    /**
     * Returns specification for PisCommonPaymentData entity for filtering payments by PSU ID Data, creation date and instance ID.
     *
     * @param psuIdData      mandatory PSU ID data
     * @param createDateFrom optional creation date that limits resulting data to payments created after this date(inclusive)
     * @param createDateTo   optional creation date that limits resulting data to payments created before this date(inclusive)
     * @param instanceId     optional instance ID
     * @param additionalTppInfo      Additional TPP information
     * @return resulting specification for PisCommonPaymentData entity
     */
    public Specification<PisCommonPaymentData> byPsuIdDataAndCreationPeriodAndInstanceIdAndAdditionalTppInfo(@NotNull PsuIdData psuIdData,
                                                                                                             @Nullable LocalDate createDateFrom,
                                                                                                             @Nullable LocalDate createDateTo,
                                                                                                             @Nullable String instanceId,
                                                                                                             @Nullable String additionalTppInfo) {
        return commonSpecification.byPsuIdDataAndCreationPeriodAndInstanceId(psuIdData, createDateFrom, createDateTo, instanceId)
            .and(byAdditionalTppInfo(additionalTppInfo));
    }

    /**
     * Returns specification for PisCommonPaymentData entity for filtering payments by aspsp account id, creation date and instance ID.
     *
     * @param aspspAccountId Bank specific account identifier
     * @param createDateFrom optional creation date that limits resulting data to payments created after this date(inclusive)
     * @param createDateTo   optional creation date that limits resulting data to payments created before this date(inclusive)
     * @param instanceId     optional instance ID
     * @param additionalTppInfo      Additional TPP information
     * @return resulting specification for PisCommonPaymentData entity
     */
    public Specification<PisCommonPaymentData> byAspspAccountIdAndCreationPeriodAndInstanceIdAndAdditionalTppInfo(@NotNull String aspspAccountId,
                                                                                                                  @Nullable LocalDate createDateFrom,
                                                                                                                  @Nullable LocalDate createDateTo,
                                                                                                                  @Nullable String instanceId,
                                                                                                                  @Nullable String additionalTppInfo) {
        return Specification.where(byAspspAccountId(aspspAccountId))
                   .and(commonSpecification.byCreationTimestamp(createDateFrom, createDateTo))
                   .and(commonSpecification.byInstanceId(instanceId))
                   .and(byAdditionalTppInfo(additionalTppInfo));
    }

    private Specification<PisCommonPaymentData> byAspspAccountId(@Nullable String aspspAccountId) {
        return provideSpecificationForEntityAttribute(ASPSP_ACCOUNT_ID_ATTRIBUTE, aspspAccountId);
    }

    /**
     * Returns specification for PisCommonPaymentData entity for filtering data by TPP authorisation number.
     *
     * <p>
     * If optional parameter is not provided, this specification will not affect resulting data.
     *
     * @param tppAuthorisationNumber optional TPP authorisation number
     * @return resulting specification
     */
    private Specification<PisCommonPaymentData> byTppAuthorisationNumber(@Nullable String tppAuthorisationNumber) {
        return (root, query, cb) -> {
            Join<PisCommonPaymentData, TppInfoEntity> tppInfoJoin = root.join(TPP_INFO_ATTRIBUTE);
            return provideSpecificationForJoinedEntityAttribute(tppInfoJoin, TPP_INFO_AUTHORISATION_NUMBER_ATTRIBUTE, tppAuthorisationNumber)
                       .toPredicate(root, query, cb);
        };
    }

    private Specification<PisCommonPaymentData> byAdditionalTppInfo(@Nullable String additionalTppInfo) {
        return provideSpecificationForEntityAttribute(ADDITIONAL_TPP_INFORMATION_ATTRIBUTE, additionalTppInfo);
    }
}
