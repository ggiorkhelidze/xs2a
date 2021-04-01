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

package de.adorsys.psd2.consent.api.service;

import de.adorsys.psd2.consent.api.CmsResponse;
import de.adorsys.psd2.xs2a.core.pis.InternalPaymentStatus;
import de.adorsys.psd2.xs2a.core.pis.Xs2aTransactionStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppRedirectUri;
import org.jetbrains.annotations.NotNull;

/**
 * Service to be used to update payment status ONLY after getting SPI service result.
 * Should not be used for any other business logic purposes.
 * <p>
 * This is base version of the service that contains all method declarations.
 * Should not be implemented directly, consider using one of the interfaces that extends this one.
 *
 * @see UpdatePaymentAfterSpiService
 * @see UpdatePaymentAfterSpiServiceEncrypted
 */
interface UpdatePaymentAfterSpiServiceBase {

    /**
     * Updates a Status of Payment object by its ID and PSU ID
     *
     * @param paymentId ID of Payment
     * @param status    Status of Payment to be set
     * @return true if the status was updated, false otherwise
     */
    CmsResponse<Boolean> updatePaymentStatus(@NotNull String paymentId, @NotNull Xs2aTransactionStatus status);

    /**
     * Updates a Status of Payment object by its ID and PSU ID
     *
     * @param paymentId ID of Payment
     * @param status    Status of Payment to be set
     * @return true if the status was updated, false otherwise
     */
    CmsResponse<Boolean> updateInternalPaymentStatus(@NotNull String paymentId, @NotNull InternalPaymentStatus status);

    /**
     * Updates a Tpp Info of Payment object by its ID
     *
     * @param paymentId      ID of Payment
     * @param tppRedirectUri Tpp redirect URIs
     * @return true if the status was updated, false otherwise
     */
    CmsResponse<Boolean> updatePaymentCancellationTppRedirectUri(@NotNull String paymentId, @NotNull TppRedirectUri tppRedirectUri);

    /**
     * Updates cancellation internal request ID of Payment object by its ID
     *
     * @param paymentId         ID of Payment
     * @param internalRequestId Internal Request ID
     * @return true if the internal request ID was updated, false otherwise
     */
    CmsResponse<Boolean> updatePaymentCancellationInternalRequestId(@NotNull String paymentId, @NotNull String internalRequestId);
}
