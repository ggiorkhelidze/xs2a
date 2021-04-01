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

package de.adorsys.psd2.core.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.adorsys.psd2.core.data.ais.AisConsentData;
import de.adorsys.psd2.xs2a.core.profile.Xs2aAccountReference;
import de.adorsys.psd2.xs2a.core.profile.Xs2aAdditionalInformationAccess;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Value
public class Xs2aConsentAccountAccess {
    public static final Xs2aConsentAccountAccess EMPTY_ACCESS = new Xs2aConsentAccountAccess(Collections.emptyList(),
                                                                       Collections.emptyList(),
                                                                       Collections.emptyList(),
                                                                       new Xs2aAdditionalInformationAccess(Collections.emptyList(), Collections.emptyList()));

    private List<Xs2aAccountReference> accounts;
    private List<Xs2aAccountReference> balances;
    private List<Xs2aAccountReference> transactions;
    private Xs2aAdditionalInformationAccess additionalInformationAccess;

    @JsonIgnore
    public boolean isNotEmpty(AisConsentData aisConsentData) {
        return !(CollectionUtils.isEmpty(accounts)
                     && CollectionUtils.isEmpty(balances)
                     && CollectionUtils.isEmpty(transactions)
                     && aisConsentData.getAllPsd2() == null
                     && aisConsentData.getAvailableAccounts() == null
                     && aisConsentData.getAvailableAccountsWithBalance() == null);
    }
}
