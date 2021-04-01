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

package de.adorsys.psd2.xs2a.service.validator.authorisation;

import de.adorsys.psd2.xs2a.core.authorisation.Authorisation;
import de.adorsys.psd2.xs2a.core.authorisation.AuthorisationType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.Xs2aScaStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;

@Component
public class AuthorisationStatusChecker {

    public boolean isFinalised(PsuIdData psuDataFromRequest, List<Authorisation> authorisations, AuthorisationType authorisationType) {

        return authorisations
                   .stream()
                   .filter(auth -> psuDataFromRequest.contentEquals(auth.getPsuIdData()))
                   .filter(auth -> auth.getAuthorisationType() == authorisationType)
                   .anyMatch(auth -> EnumSet.of(Xs2aScaStatus.FINALISED, Xs2aScaStatus.EXEMPTED).contains(auth.getScaStatus()));
    }
}
