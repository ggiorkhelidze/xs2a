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

package de.adorsys.psd2.xs2a.service.validator.pis.authorisation.initiation;

import de.adorsys.psd2.xs2a.core.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.OauthPaymentValidator;
import de.adorsys.psd2.xs2a.service.validator.pis.AbstractPisValidator;
import de.adorsys.psd2.xs2a.service.validator.pis.CommonPaymentObject;
import org.springframework.stereotype.Component;

/**
 * Validator to be used for validating get payment initiation authorisations request according to some business rules
 */
@Component
public class GetPaymentInitiationAuthorisationsValidator extends AbstractPisValidator<CommonPaymentObject> {
    private final OauthPaymentValidator oauthPaymentValidator;

    public GetPaymentInitiationAuthorisationsValidator(OauthPaymentValidator oauthPaymentValidator) {
        this.oauthPaymentValidator = oauthPaymentValidator;
    }

    /**
     * Validates get payment initiation authorisations request
     *
     * @param paymentObject payment information object
     * @return valid result if the payment is valid, invalid result with appropriate error otherwise
     */
    @Override
    protected ValidationResult executeBusinessValidation(CommonPaymentObject paymentObject) {
        return oauthPaymentValidator.validate(paymentObject.getPisCommonPaymentResponse());
    }
}
