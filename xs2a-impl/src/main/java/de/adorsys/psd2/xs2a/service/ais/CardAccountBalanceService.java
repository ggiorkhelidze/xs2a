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

package de.adorsys.psd2.xs2a.service.ais;

import de.adorsys.psd2.consent.api.TypeAccess;
import de.adorsys.psd2.core.data.Xs2aConsentAccountAccess;
import de.adorsys.psd2.core.data.ais.AisConsent;
import de.adorsys.psd2.event.core.model.EventType;
import de.adorsys.psd2.logger.context.LoggingContextService;
import de.adorsys.psd2.xs2a.core.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.core.error.MessageError;
import de.adorsys.psd2.xs2a.core.mapper.ServiceType;
import de.adorsys.psd2.xs2a.core.profile.Xs2aAccountReference;
import de.adorsys.psd2.xs2a.core.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.account.Xs2aBalancesReport;
import de.adorsys.psd2.xs2a.service.event.Xs2aEventService;
import de.adorsys.psd2.xs2a.service.mapper.AccountMappersHolder;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.service.validator.ais.account.GetCardBalancesReportValidator;
import de.adorsys.psd2.xs2a.service.validator.ais.account.dto.GetCardAccountBalanceRequestObject;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountBalance;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountReference;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.CardAccountSpi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.adorsys.psd2.xs2a.core.error.ErrorType.AIS_401;
import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.CONSENT_INVALID;

@Slf4j
@Service
public class CardAccountBalanceService extends AbstractBalanceService {
    private final CardAccountSpi cardAccountSpi;
    private final AccountMappersHolder accountMappersHolder;
    private final AccountServicesHolder accountServicesHolder;
    private final GetCardBalancesReportValidator getCardBalancesReportValidator;
    private final SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;


    public CardAccountBalanceService(AccountServicesHolder accountServicesHolder,
                                     Xs2aEventService xs2aEventService,
                                     LoggingContextService loggingContextService,
                                     CardAccountSpi cardAccountSpi,
                                     AccountMappersHolder accountMappersHolder,
                                     GetCardBalancesReportValidator getCardBalancesReportValidator,
                                     SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory) {
        super(accountServicesHolder, xs2aEventService, loggingContextService);
        this.cardAccountSpi = cardAccountSpi;
        this.accountServicesHolder = accountServicesHolder;
        this.accountMappersHolder = accountMappersHolder;
        this.getCardBalancesReportValidator = getCardBalancesReportValidator;
        this.aspspConsentDataProviderFactory = aspspConsentDataProviderFactory;
    }

    @Override
    protected EventType getEventType() {
        return EventType.READ_CARD_BALANCE_REQUEST_RECEIVED;
    }

    @Override
    protected ValidationResult getValidationResultForCommonAccountBalanceRequest(String accountId, String requestUri, AisConsent aisConsent) {
        return getCardBalancesReportValidator.validate(
            new GetCardAccountBalanceRequestObject(aisConsent, accountId, requestUri));
    }

    @Override
    protected SpiResponse<List<SpiAccountBalance>> getSpiResponse(AisConsent aisConsent, String consentId, String accountId) {
        Xs2aConsentAccountAccess access = aisConsent.getAspspAccountAccesses();
        SpiAccountReference requestedAccountReference = accountServicesHolder.findAccountReference(access.getBalances(), accountId);

        return cardAccountSpi.requestCardBalancesForAccount(accountServicesHolder.getSpiContextData(),
                                                            requestedAccountReference,
                                                            accountMappersHolder.mapToSpiAccountConsent(aisConsent),
                                                            aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(consentId));
    }

    @Override
    protected ResponseObject<Xs2aBalancesReport> checkSpiResponse(String consentId, String accountId, SpiResponse<List<SpiAccountBalance>> spiResponse) {

        log.info("Account-ID [{}], Consent-ID: [{}]. Get card balances report failed: error on SPI level",
                 accountId, consentId);
        return ResponseObject.<Xs2aBalancesReport>builder()
                   .fail(new MessageError(accountMappersHolder.mapToErrorHolder(spiResponse, ServiceType.AIS)))
                   .build();
    }

    @Override
    protected ResponseObject<Xs2aBalancesReport> getXs2aBalancesReportResponseObject(AisConsent aisConsent,
                                                                                     String accountId,
                                                                                     String consentId,
                                                                                     String requestUri,
                                                                                     List<SpiAccountBalance> payload) {
        Xs2aConsentAccountAccess access = aisConsent.getAspspAccountAccesses();
        List<Xs2aAccountReference> balances = access.getBalances();
        if (hasNoAccessToCardSource(balances)) {
            return ResponseObject.<Xs2aBalancesReport>builder()
                       .fail(AIS_401, TppMessageInformation.of(CONSENT_INVALID))
                       .build();
        }

        Xs2aAccountReference maskedAccountReference = getMaskedAccountReference(accountId, access.getBalances());

        Xs2aBalancesReport balancesReport = accountMappersHolder.mapToXs2aBalancesReport(maskedAccountReference, payload);

        ResponseObject<Xs2aBalancesReport> response = ResponseObject.<Xs2aBalancesReport>builder()
                                                          .body(balancesReport)
                                                          .build();

        accountServicesHolder.consentActionLog(accountServicesHolder.getTppId(), consentId,
                                               accountServicesHolder.createActionStatus(false, TypeAccess.BALANCE, response),
                                               requestUri, accountServicesHolder.needsToUpdateUsage(aisConsent), accountId, null);

        return response;
    }

    private Xs2aAccountReference getMaskedAccountReference(String accountId, List<Xs2aAccountReference> balances) {
        Xs2aAccountReference filteredAccountReference = filterAccountReference(balances, accountId);

        if (filteredAccountReference != null && StringUtils.isNotBlank(filteredAccountReference.getPan())) {
            String maskedPan = accountServicesHolder.hidePanInAccountReference(filteredAccountReference.getPan());

            filteredAccountReference.setPan(null);
            filteredAccountReference.setMaskedPan(maskedPan);
        }

        return filteredAccountReference;
    }

    private Xs2aAccountReference filterAccountReference(List<Xs2aAccountReference> references, String resourceId) {
        return references.stream()
                   .filter(accountReference -> StringUtils.equals(accountReference.getResourceId(), resourceId))
                   .findFirst()
                   .orElse(null);
    }

    private boolean hasNoAccessToCardSource(List<Xs2aAccountReference> references) {
        return references.stream()
                   .allMatch(Xs2aAccountReference::isNotCardAccount);
    }
}
