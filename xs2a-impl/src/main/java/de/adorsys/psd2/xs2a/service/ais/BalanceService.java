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
import de.adorsys.psd2.xs2a.service.validator.ais.account.GetBalancesReportValidator;
import de.adorsys.psd2.xs2a.service.validator.ais.account.dto.GetAccountBalanceRequestObject;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountBalance;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountReference;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.AccountSpi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.adorsys.psd2.xs2a.core.error.ErrorType.AIS_401;
import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.CONSENT_INVALID;

@Slf4j
@Service
public class BalanceService extends AbstractBalanceService {
    private final AccountSpi accountSpi;
    private final AccountMappersHolder accountMappersHolder;
    private final AccountServicesHolder accountServicesHolder;

    private final GetBalancesReportValidator getBalancesReportValidator;
    private final SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;

    public BalanceService(AccountServicesHolder accountServicesHolder,
                          Xs2aEventService xs2aEventService,
                          LoggingContextService loggingContextService,
                          AccountSpi accountSpi,
                          AccountMappersHolder accountMappersHolder,
                          GetBalancesReportValidator getBalancesReportValidator,
                          SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory) {
        super(accountServicesHolder, xs2aEventService, loggingContextService);
        this.accountServicesHolder = accountServicesHolder;
        this.accountSpi = accountSpi;
        this.accountMappersHolder = accountMappersHolder;
        this.getBalancesReportValidator = getBalancesReportValidator;
        this.aspspConsentDataProviderFactory = aspspConsentDataProviderFactory;
    }

    @Override
    protected EventType getEventType() {
        return EventType.READ_BALANCE_REQUEST_RECEIVED;
    }

    @Override
    protected ValidationResult getValidationResultForCommonAccountBalanceRequest(String accountId, String requestUri, AisConsent accountConsent) {
        return getBalancesReportValidator.validate(
            new GetAccountBalanceRequestObject(accountConsent, accountId, requestUri));
    }

    @Override
    protected SpiResponse<List<SpiAccountBalance>> getSpiResponse(AisConsent aisConsent, String consentId, String accountId) {
        Xs2aConsentAccountAccess access = aisConsent.getAspspAccountAccesses();
        SpiAccountReference requestedAccountReference = accountServicesHolder.findAccountReference(access.getBalances(), accountId);

        return accountSpi.requestBalancesForAccount(accountServicesHolder.getSpiContextData(),
                                                    requestedAccountReference,
                                                    accountMappersHolder.mapToSpiAccountConsent(aisConsent),
                                                    aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(consentId));
    }

    @Override
    protected ResponseObject<Xs2aBalancesReport> checkSpiResponse(String consentId, String accountId, SpiResponse<List<SpiAccountBalance>> spiResponse) {

        log.info("Account-ID [{}], Consent-ID: [{}]. Get balances report failed: error on SPI level",
                 accountId, consentId);
        return ResponseObject.<Xs2aBalancesReport>builder()
                   .fail(new MessageError(accountMappersHolder.mapToErrorHolder(spiResponse, ServiceType.AIS)))
                   .build();
    }

    @Override
    protected ResponseObject<Xs2aBalancesReport> getXs2aBalancesReportResponseObject(AisConsent accountConsent,
                                                                                     String accountId,
                                                                                     String consentId,
                                                                                     String requestUri,
                                                                                     List<SpiAccountBalance> payload) {
        Xs2aConsentAccountAccess access = accountConsent.getAspspAccountAccesses();
        List<Xs2aAccountReference> balances = access.getBalances();
        if (hasNoAccessToSource(balances)) {
            return ResponseObject.<Xs2aBalancesReport>builder()
                       .fail(AIS_401, TppMessageInformation.of(CONSENT_INVALID))
                       .build();
        }

        SpiAccountReference requestedAccountReference = accountServicesHolder.findAccountReference(balances, accountId);

        Xs2aBalancesReport balancesReport = accountMappersHolder.mapToXs2aBalancesReportSpi(requestedAccountReference, payload);

        ResponseObject<Xs2aBalancesReport> response = ResponseObject.<Xs2aBalancesReport>builder()
                                                          .body(balancesReport)
                                                          .build();

        accountServicesHolder.consentActionLog(accountServicesHolder.getTppId(), consentId,
                                               accountServicesHolder.createActionStatus(false, TypeAccess.BALANCE, response),
                                               requestUri, accountServicesHolder.needsToUpdateUsage(accountConsent), accountId, null);

        return response;
    }

    private boolean hasNoAccessToSource(List<Xs2aAccountReference> references) {
        return references.stream()
                   .allMatch(Xs2aAccountReference::isNotIbanAccount);
    }
}
