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

package de.adorsys.psd2.consent.api.ais;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.adorsys.psd2.xs2a.core.profile.Xs2aAccountReference;
import de.adorsys.psd2.xs2a.core.profile.Xs2aAdditionalInformationAccess;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Value
public class AisAccountAccess {
    private List<Xs2aAccountReference> accounts;
    private List<Xs2aAccountReference> balances;
    private List<Xs2aAccountReference> transactions;

    private String availableAccounts;
    private String allPsd2;
    private String availableAccountsWithBalance;
    @Nullable
    private Xs2aAdditionalInformationAccess accountAdditionalInformationAccess;

    @JsonIgnore
    public boolean isNotEmpty() {
        return !(CollectionUtils.isEmpty(accounts)
                     && CollectionUtils.isEmpty(balances)
                     && CollectionUtils.isEmpty(transactions)
                     && allPsd2 == null
                     && availableAccounts == null
                     && availableAccountsWithBalance == null);
    }
}
