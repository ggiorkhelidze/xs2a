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

package de.adorsys.psd2.xs2a.service.authorization.processor;

import de.adorsys.psd2.xs2a.core.sca.Xs2aScaStatus;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.AuthorisationProcessorRequest;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.AuthorisationProcessorResponse;
import de.adorsys.psd2.xs2a.service.authorization.processor.service.AuthorisationProcessorService;

public class ExemptedAuthorisationProcessor extends AuthorisationProcessor {

    public ExemptedAuthorisationProcessor(AuthorisationProcessorServiceProvider provider) {
        super(provider);
    }

    @Override
    public Xs2aScaStatus getScaStatus() {
        return Xs2aScaStatus.EXEMPTED;
    }

    @Override
    protected AuthorisationProcessorResponse execute(AuthorisationProcessorRequest request,
                                                     AuthorisationProcessorService processorService) {
        return processorService.doScaExempted(request);
    }
}
