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

package de.adorsys.psd2.xs2a.web.validator.body.consent;

import de.adorsys.psd2.core.data.Xs2aConsentAccountAccess;
import de.adorsys.psd2.mapper.Xs2aObjectMapper;
import de.adorsys.psd2.model.AccountReference;
import de.adorsys.psd2.model.Consents;
import de.adorsys.psd2.xs2a.core.ais.AccountAccessType;
import de.adorsys.psd2.xs2a.core.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.core.error.MessageError;
import de.adorsys.psd2.xs2a.core.profile.Xs2aAdditionalInformationAccess;
import de.adorsys.psd2.xs2a.core.profile.Xs2aAccountReference;
import de.adorsys.psd2.xs2a.domain.consent.CreateConsentReq;
import de.adorsys.psd2.xs2a.web.validator.ErrorBuildingService;
import de.adorsys.psd2.xs2a.web.validator.body.AbstractBodyValidatorImpl;
import de.adorsys.psd2.xs2a.web.validator.body.AccountReferenceValidator;
import de.adorsys.psd2.xs2a.web.validator.body.DateFieldValidator;
import de.adorsys.psd2.xs2a.web.validator.body.FieldLengthValidator;
import de.adorsys.psd2.xs2a.web.validator.body.raw.FieldExtractor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.*;
import static de.adorsys.psd2.xs2a.web.validator.constants.Xs2aRequestBodyDateFields.AIS_CONSENT_DATE_FIELDS;

@Component
public class AccountAccessValidatorImpl extends AbstractBodyValidatorImpl implements ConsentBodyValidator {

    private AccountReferenceValidator accountReferenceValidator;
    private DateFieldValidator dateFieldValidator;
    private FieldExtractor fieldExtractor;

    @Autowired
    public AccountAccessValidatorImpl(ErrorBuildingService errorBuildingService, Xs2aObjectMapper xs2aObjectMapper,
                                      AccountReferenceValidator accountReferenceValidator, DateFieldValidator dateFieldValidator,
                                      FieldExtractor fieldExtractor, FieldLengthValidator fieldLengthValidator) {
        super(errorBuildingService, xs2aObjectMapper, fieldLengthValidator);
        this.accountReferenceValidator = accountReferenceValidator;
        this.dateFieldValidator = dateFieldValidator;
        this.fieldExtractor = fieldExtractor;
    }

    @Override
    public MessageError validateBodyFields(HttpServletRequest request, MessageError messageError) {

        Optional<Consents> consentsOptional = fieldExtractor.mapBodyToInstance(request, messageError, Consents.class);

        // In case of wrong JSON - we don't proceed to the inner fields validation.
        if (consentsOptional.isEmpty()) {
            return messageError;
        }

        Consents consents = consentsOptional.get();

        if (consents.getAccess() == null) { //NOSONAR
            errorBuildingService.enrichMessageError(messageError, TppMessageInformation.of(FORMAT_ERROR_NULL_VALUE, "access"));
        } else {
            validateAccountAccess(consents, messageError);
        }

        return messageError;
    }

    @Override
    public MessageError validateRawData(HttpServletRequest request, MessageError messageError) {
        return dateFieldValidator.validateDateFormat(request, AIS_CONSENT_DATE_FIELDS.getDateFields(), messageError);
    }

    private void validateAccountAccess(Consents consents, MessageError messageError) {
        de.adorsys.psd2.model.AccountAccess accountAccess = consents.getAccess();

        if (accountAccess.getAccounts() != null) {

            Stream<AccountReference> allReferences = Stream.of(accountAccess.getAccounts(), accountAccess.getBalances(), accountAccess.getTransactions())
                                                         .filter(Objects::nonNull)
                                                         .flatMap(Collection::stream);

            Stream<AccountReference> additionalReferences = Optional.ofNullable(accountAccess.getAdditionalInformation())
                                                                .map(additionalInformationAccess ->
                                                                         Stream.of(additionalInformationAccess.getOwnerName(),
                                                                                   additionalInformationAccess.getTrustedBeneficiaries())
                                                                             .filter(Objects::nonNull)
                                                                             .flatMap(Collection::stream))
                                                                .orElseGet(Stream::empty);

            List<AccountReference> allReferencesList = allReferences.collect(Collectors.toList());
            List<AccountReference> additionalReferencesList = additionalReferences.collect(Collectors.toList());

            Stream.of(allReferencesList, additionalReferencesList)
                .flatMap(Collection::stream)
                .distinct()
                .forEach(ar -> accountReferenceValidator.validate(ar, messageError));

            // checks for correspondence between additionalReferences and addressed by at least one of the attributes "accounts", "transactions" or "balances"
            if (areAdditionalReferencesIncorrect(additionalReferencesList, allReferencesList)) { //NOSONAR
                errorBuildingService.enrichMessageError(messageError, TppMessageInformation.of(SERVICE_INVALID_400));
            }

            CreateConsentReq createConsent = mapToCreateConsentReq(consents, messageError);

            // This object is checked for null on the level above
            if (areFlagsAndAccountsInvalid(createConsent)) { //NOSONAR
                errorBuildingService.enrichMessageError(messageError, TppMessageInformation.of(FORMAT_ERROR_CONSENT_INCORRECT));
            }
        }
    }

    private boolean areAdditionalReferencesIncorrect(List<AccountReference> additionalReferencesList, List<AccountReference> allReferencesList) {
        if (CollectionUtils.isEmpty(additionalReferencesList)) {
            return false;
        }

        return !additionalReferencesList.stream()
                    .allMatch(allReferencesList::contains);
    }

    private boolean areFlagsAndAccountsInvalid(CreateConsentReq request) {
        Xs2aConsentAccountAccess access = request.getAccess();
        if (access.isNotEmpty(request.getAisConsentData())) {
            return !(CollectionUtils.isEmpty(request.getAccountReferences()) || areFlagsEmpty(request));
        }
        return false;
    }

    private boolean areFlagsEmpty(CreateConsentReq createConsentReq) {
        return Objects.isNull(createConsentReq.getAvailableAccounts()) && Objects.isNull(createConsentReq.getAllPsd2());
    }

    private CreateConsentReq mapToCreateConsentReq(Consents consent, MessageError messageError) {
        return Optional.ofNullable(consent)
                   .map(cnst -> {
                       CreateConsentReq createAisConsentRequest = new CreateConsentReq();
                       createAisConsentRequest.setAccess(mapToAccountAccessInner(cnst.getAccess(), messageError));
                       createAisConsentRequest.setAvailableAccounts(mapToAccountAccessTypeFromAvailableAccounts(cnst.getAccess().getAvailableAccounts()));
                       createAisConsentRequest.setAvailableAccountsWithBalance(mapToAccountAccessTypeFromAvailableAccountsWithBalance(cnst.getAccess().getAvailableAccountsWithBalance()));
                       createAisConsentRequest.setAllPsd2(mapToAccountAccessTypeFromAllPsd2Enum(cnst.getAccess().getAllPsd2()));
                       return createAisConsentRequest;
                   })
                   .orElse(null);
    }

    private Xs2aConsentAccountAccess mapToAccountAccessInner(de.adorsys.psd2.model.AccountAccess accountAccess, MessageError messageError) {
        return Optional.ofNullable(accountAccess)
                   .map(acs ->
                            new Xs2aConsentAccountAccess(
                                mapToXs2aAccountReferences(acs.getAccounts(), messageError),
                                mapToXs2aAccountReferences(acs.getBalances(), messageError),
                                mapToXs2aAccountReferences(acs.getTransactions(), messageError),
                                mapToAdditionalInformationAccess(acs.getAdditionalInformation(), messageError)
                            ))
                   .orElse(null);
    }

    private Xs2aAdditionalInformationAccess mapToAdditionalInformationAccess(de.adorsys.psd2.model.AdditionalInformationAccess additionalInformationAccess, MessageError messageError) {
        if (additionalInformationAccess == null) {
            return null;
        }

        return new Xs2aAdditionalInformationAccess(mapToXs2aAccountReferences(additionalInformationAccess.getOwnerName(), messageError),
                                               mapToXs2aAccountReferences(additionalInformationAccess.getTrustedBeneficiaries(), messageError));
    }

    private List<Xs2aAccountReference> mapToXs2aAccountReferences(List<de.adorsys.psd2.model.AccountReference> references, MessageError messageError) { // NOPMD
        return Optional.ofNullable(references)
                   .map(ref -> ref.stream()
                                   .map((AccountReference reference) -> mapToAccountReference(reference, messageError))
                                   .collect(Collectors.toList()))
                   .orElseGet(Collections::emptyList);
    }

    private AccountAccessType mapToAccountAccessTypeFromAvailableAccounts(de.adorsys.psd2.model.AccountAccess.AvailableAccountsEnum accountsEnum) {
        return Optional.ofNullable(accountsEnum)
                   .flatMap(en -> AccountAccessType.getByDescription(en.toString()))
                   .orElse(null);
    }

    private AccountAccessType mapToAccountAccessTypeFromAllPsd2Enum(de.adorsys.psd2.model.AccountAccess.AllPsd2Enum allPsd2Enum) {
        return Optional.ofNullable(allPsd2Enum)
                   .flatMap(en -> AccountAccessType.getByDescription(en.toString()))
                   .orElse(null);
    }

    private AccountAccessType mapToAccountAccessTypeFromAvailableAccountsWithBalance(de.adorsys.psd2.model.AccountAccess.AvailableAccountsWithBalanceEnum accountsEnum) {
        return Optional.ofNullable(accountsEnum)
                   .flatMap(en -> AccountAccessType.getByDescription(en.toString()))
                   .orElse(null);
    }

    private Xs2aAccountReference mapToAccountReference(Object reference, MessageError messageError) {
        try {
            return xs2aObjectMapper.convertValue(reference, Xs2aAccountReference.class);
        } catch (IllegalArgumentException e) {
            // Happens only during Currency field processing, as other fields are of String type.
            errorBuildingService.enrichMessageError(messageError, TppMessageInformation.of(FORMAT_ERROR_WRONG_FORMAT_VALUE, "currency"));
            return null;
        }
    }
}
