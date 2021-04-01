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

package de.adorsys.psd2.xs2a.domain.account;

import de.adorsys.psd2.xs2a.core.domain.address.Xs2aAddress;
import de.adorsys.psd2.xs2a.core.profile.Xs2aAccountReference;
import lombok.Value;

@Value
public class Xs2aTrustedBeneficiaries {
    private String trustedBeneficiaryId;
    private Xs2aAccountReference debtorAccount;
    private Xs2aAccountReference creditorAccount;
    private String creditorAgent;
    private String creditorName;
    private String creditorAlias;
    private String creditorId;
    private Xs2aAddress creditorAddress;
}
