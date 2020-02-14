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

package de.adorsys.psd2.xs2a.web.validator;

import de.adorsys.psd2.xs2a.web.Psd2PaymentMethodNameConstant;
import de.adorsys.psd2.xs2a.web.validator.body.payment.PaymentBodyValidator;
import de.adorsys.psd2.xs2a.web.validator.header.PaymentHeaderValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentMethodValidatorImpl extends AbstractMethodValidator {

    @Autowired
    protected PaymentMethodValidatorImpl(List<PaymentHeaderValidator> headerValidators,
                                         List<PaymentBodyValidator> bodyValidators) {
        super(ValidatorWrapper.builder()
                  .headerValidators(headerValidators)
                  .bodyValidators(bodyValidators)
                  .build());
    }

    @Override
    public String getMethodName() {
        return Psd2PaymentMethodNameConstant.INITIATE_PAYMENT.getValue();
    }
}
