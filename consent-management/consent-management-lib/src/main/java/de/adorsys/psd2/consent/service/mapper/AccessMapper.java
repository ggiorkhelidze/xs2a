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

package de.adorsys.psd2.consent.service.mapper;

import de.adorsys.psd2.consent.api.TypeAccess;
import de.adorsys.psd2.consent.api.ais.AdditionalAccountInformationType;
import de.adorsys.psd2.consent.domain.account.AspspAccountAccess;
import de.adorsys.psd2.consent.domain.account.TppAccountAccess;
import de.adorsys.psd2.core.data.Xs2aConsentAccountAccess;
import de.adorsys.psd2.xs2a.core.profile.Xs2aAccountReference;
import de.adorsys.psd2.xs2a.core.profile.Xs2aAdditionalInformationAccess;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AccessMapper {

    public Xs2aConsentAccountAccess mapTppAccessesToAccountAccess(List<TppAccountAccess> tppAccountAccesses,
                                                                  AdditionalAccountInformationType ownerNameType,
                                                                  AdditionalAccountInformationType trustedBeneficiariesType) {
        AccountAccessListHolder holder = new AccountAccessListHolder();
        tppAccountAccesses.forEach(a -> {
            Xs2aAccountReference accountReference = new Xs2aAccountReference(a.getAccountReferenceType(),
                                                                     a.getAccountIdentifier(),
                                                                     a.getCurrency());
            holder.addAccountReference(accountReference, a.getTypeAccess());
        });
        return buildAccountAccess(holder, ownerNameType, trustedBeneficiariesType);
    }

    public Xs2aConsentAccountAccess mapAspspAccessesToAccountAccess(List<AspspAccountAccess> aspspAccountAccesses,
                                                                    AdditionalAccountInformationType ownerNameType,
                                                                    AdditionalAccountInformationType trustedBeneficiariesType) {
        AccountAccessListHolder holder = new AccountAccessListHolder();
        aspspAccountAccesses.forEach(a -> {
            Xs2aAccountReference accountReference = new Xs2aAccountReference(a.getAccountReferenceType(),
                                                                     a.getAccountIdentifier(),
                                                                     a.getCurrency(),
                                                                     a.getResourceId(),
                                                                     a.getAspspAccountId());
            holder.addAccountReference(accountReference, a.getTypeAccess());
        });
        return buildAccountAccess(holder, ownerNameType, trustedBeneficiariesType);
    }

    public List<TppAccountAccess> mapToTppAccountAccess(Xs2aConsentAccountAccess accountAccess) {
        List<TppAccountAccess> tppAccountAccesses = new ArrayList<>();
        tppAccountAccesses.addAll(accountAccess.getAccounts().stream().map(a -> new TppAccountAccess(a.getUsedAccountReferenceSelector().getAccountValue(),
                                                                                                     TypeAccess.ACCOUNT,
                                                                                                     a.getUsedAccountReferenceSelector().getAccountReferenceType(),
                                                                                                     a.getCurrency())).collect(Collectors.toList()));
        tppAccountAccesses.addAll(accountAccess.getBalances().stream().map(a -> new TppAccountAccess(a.getUsedAccountReferenceSelector().getAccountValue(),
                                                                                                     TypeAccess.BALANCE,
                                                                                                     a.getUsedAccountReferenceSelector().getAccountReferenceType(),
                                                                                                     a.getCurrency())).collect(Collectors.toList()));
        tppAccountAccesses.addAll(accountAccess.getTransactions().stream().map(a -> new TppAccountAccess(a.getUsedAccountReferenceSelector().getAccountValue(),
                                                                                                         TypeAccess.TRANSACTION,
                                                                                                         a.getUsedAccountReferenceSelector().getAccountReferenceType(),
                                                                                                         a.getCurrency())).collect(Collectors.toList()));
        Xs2aAdditionalInformationAccess additionalInformationAccess = accountAccess.getAdditionalInformationAccess();
        if (additionalInformationAccess != null) {
            if (CollectionUtils.isNotEmpty(additionalInformationAccess.getOwnerName())) {
                tppAccountAccesses.addAll(additionalInformationAccess.getOwnerName().stream().map(a -> new TppAccountAccess(a.getUsedAccountReferenceSelector().getAccountValue(),
                                                                                                                            TypeAccess.OWNER_NAME,
                                                                                                                            a.getUsedAccountReferenceSelector().getAccountReferenceType(),
                                                                                                                            a.getCurrency())).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(additionalInformationAccess.getTrustedBeneficiaries())) {
                tppAccountAccesses.addAll(additionalInformationAccess.getTrustedBeneficiaries().stream().map(a -> new TppAccountAccess(a.getUsedAccountReferenceSelector().getAccountValue(),
                                                                                                                                       TypeAccess.BENEFICIARIES,
                                                                                                                                       a.getUsedAccountReferenceSelector().getAccountReferenceType(),
                                                                                                                                       a.getCurrency())).collect(Collectors.toList()));
            }
        }

        return tppAccountAccesses;
    }

    public List<AspspAccountAccess> mapToAspspAccountAccess(Xs2aConsentAccountAccess accountAccess) {
        List<AspspAccountAccess> aspspAccountAccesses = new ArrayList<>();
        aspspAccountAccesses.addAll(accountAccess.getAccounts().stream().map(a -> new AspspAccountAccess(a.getUsedAccountReferenceSelector().getAccountValue(),
                                                                                                         TypeAccess.ACCOUNT,
                                                                                                         a.getUsedAccountReferenceSelector().getAccountReferenceType(),
                                                                                                         a.getCurrency(),
                                                                                                         a.getResourceId(),
                                                                                                         a.getAspspAccountId())).collect(Collectors.toList()));
        aspspAccountAccesses.addAll(accountAccess.getBalances().stream().map(a -> new AspspAccountAccess(a.getUsedAccountReferenceSelector().getAccountValue(),
                                                                                                         TypeAccess.BALANCE,
                                                                                                         a.getUsedAccountReferenceSelector().getAccountReferenceType(),
                                                                                                         a.getCurrency(),
                                                                                                         a.getResourceId(),
                                                                                                         a.getAspspAccountId())).collect(Collectors.toList()));
        aspspAccountAccesses.addAll(accountAccess.getTransactions().stream().map(a -> new AspspAccountAccess(a.getUsedAccountReferenceSelector().getAccountValue(),
                                                                                                             TypeAccess.TRANSACTION,
                                                                                                             a.getUsedAccountReferenceSelector().getAccountReferenceType(),
                                                                                                             a.getCurrency(),
                                                                                                             a.getResourceId(),
                                                                                                             a.getAspspAccountId())).collect(Collectors.toList()));
        Xs2aAdditionalInformationAccess additionalInformationAccess = accountAccess.getAdditionalInformationAccess();
        if (additionalInformationAccess != null) {
            if (CollectionUtils.isNotEmpty(additionalInformationAccess.getOwnerName())) {
                aspspAccountAccesses.addAll(additionalInformationAccess.getOwnerName().stream().map(a -> new AspspAccountAccess(a.getUsedAccountReferenceSelector().getAccountValue(),
                                                                                                                                TypeAccess.OWNER_NAME,
                                                                                                                                a.getUsedAccountReferenceSelector().getAccountReferenceType(),
                                                                                                                                a.getCurrency(),
                                                                                                                                a.getResourceId(),
                                                                                                                                a.getAspspAccountId())).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(additionalInformationAccess.getTrustedBeneficiaries())) {
                aspspAccountAccesses.addAll(additionalInformationAccess.getTrustedBeneficiaries().stream().map(a -> new AspspAccountAccess(a.getUsedAccountReferenceSelector().getAccountValue(),
                                                                                                                                           TypeAccess.BENEFICIARIES,
                                                                                                                                           a.getUsedAccountReferenceSelector().getAccountReferenceType(),
                                                                                                                                           a.getCurrency(),
                                                                                                                                           a.getResourceId(),
                                                                                                                                           a.getAspspAccountId())).collect(Collectors.toList()));
            }
        }

        return aspspAccountAccesses;
    }

    public AspspAccountAccess mapToAspspAccountAccess(Xs2aAccountReference accountReference) {
        return new AspspAccountAccess(accountReference.getUsedAccountReferenceSelector().getAccountValue(),
                                      TypeAccess.ACCOUNT,
                                      accountReference.getUsedAccountReferenceSelector().getAccountReferenceType(),
                                      accountReference.getCurrency(),
                                      accountReference.getResourceId(),
                                      accountReference.getAspspAccountId());
    }

    public Xs2aAccountReference mapToAccountReference(AspspAccountAccess aspspAccountAccess) {
        return new Xs2aAccountReference(aspspAccountAccess.getAccountReferenceType(),
                                    aspspAccountAccess.getAccountIdentifier(),
                                    aspspAccountAccess.getCurrency(),
                                    aspspAccountAccess.getResourceId(),
                                    aspspAccountAccess.getAspspAccountId());
    }

    @Getter
    private static class AccountAccessListHolder {
        List<Xs2aAccountReference> accounts = new ArrayList<>();
        List<Xs2aAccountReference> balances = new ArrayList<>();
        List<Xs2aAccountReference> transactions = new ArrayList<>();
        List<Xs2aAccountReference> ownerNames = new ArrayList<>();
        List<Xs2aAccountReference> trustedBeneficiaries = new ArrayList<>();

        void addAccountReference(Xs2aAccountReference accountReference, TypeAccess typeAccess) {
            if (TypeAccess.ACCOUNT == typeAccess) {
                accounts.add(accountReference);
            } else if (TypeAccess.BALANCE == typeAccess) {
                balances.add(accountReference);
            } else if (TypeAccess.TRANSACTION == typeAccess) {
                transactions.add(accountReference);
            } else if (TypeAccess.OWNER_NAME == typeAccess) {
                ownerNames.add(accountReference);
            } else if (TypeAccess.BENEFICIARIES == typeAccess) {
                trustedBeneficiaries.add(accountReference);
            }
        }
    }

    private Xs2aConsentAccountAccess buildAccountAccess(AccountAccessListHolder holder,
                                                        AdditionalAccountInformationType ownerNameType,
                                                        AdditionalAccountInformationType trustedBeneficiariesType) {

        return new Xs2aConsentAccountAccess(holder.getAccounts(), holder.getBalances(), holder.getTransactions(),
                                 new Xs2aAdditionalInformationAccess(resolveAdditionalAccountInformationType(ownerNameType).getReferencesByType(holder.getOwnerNames()),
                                                                 resolveAdditionalAccountInformationType(trustedBeneficiariesType).getReferencesByType(holder.getTrustedBeneficiaries())));
    }

    private AdditionalAccountInformationType resolveAdditionalAccountInformationType(AdditionalAccountInformationType additionalAccountInformationType) {
        return additionalAccountInformationType == null ? AdditionalAccountInformationType.NONE : additionalAccountInformationType;
    }
}
