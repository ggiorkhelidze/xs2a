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

package de.adorsys.psd2.xs2a.service.validator;

import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.sca.Xs2aScaStatus;

import java.util.EnumSet;

public class EndpointAccessChecker {

    protected boolean isAccessible(ScaApproach chosenScaApproach, Xs2aScaStatus scaStatus, boolean confirmationCodeCase) {
        if (ScaApproach.REDIRECT == chosenScaApproach) {
            return EnumSet.of(Xs2aScaStatus.UNCONFIRMED, Xs2aScaStatus.FAILED, Xs2aScaStatus.FINALISED).contains(scaStatus) && confirmationCodeCase;
        } else if (ScaApproach.DECOUPLED == chosenScaApproach) {
            return Xs2aScaStatus.SCAMETHODSELECTED != scaStatus;
        }
        return true;
    }
}
