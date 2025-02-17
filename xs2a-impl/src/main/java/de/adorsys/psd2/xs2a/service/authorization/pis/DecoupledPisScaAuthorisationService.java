/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
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

package de.adorsys.psd2.xs2a.service.authorization.pis;

import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.service.mapper.cms_xs2a_mappers.Xs2aPisCommonPaymentMapper;
import org.springframework.stereotype.Service;

@Service
public class DecoupledPisScaAuthorisationService extends AbstractPisScaAuthorisationService {

    public DecoupledPisScaAuthorisationService(PisAuthorisationService authorisationService, Xs2aPisCommonPaymentMapper pisCommonPaymentMapper) {
        super(authorisationService, pisCommonPaymentMapper);
    }

    @Override
    public ScaApproach getScaApproachServiceType() {
        return ScaApproach.DECOUPLED;
    }
}
