package de.adorsys.aspsp.xs2a.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.aspsp.xs2a.domain.*;
import de.adorsys.aspsp.xs2a.exception.MessageCategory;
import de.adorsys.aspsp.xs2a.exception.MessageError;
import de.adorsys.aspsp.xs2a.service.mapper.AccountMapper;
import de.adorsys.aspsp.xs2a.service.validator.ValidationGroup;
import de.adorsys.aspsp.xs2a.service.validator.ValueValidatorService;
import de.adorsys.aspsp.xs2a.spi.service.AccountSpi;
import de.adorsys.aspsp.xs2a.web.AccountController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.validation.ValidationException;
import java.util.*;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@Service
@Validated
public class AccountService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);

    private int maxNumberOfCharInTransactionJson;
    private AccountSpi accountSpi;
    private AccountMapper accountMapper;
    private ValueValidatorService validatorService;

    @Autowired
    public AccountService(AccountSpi accountSpi, int maxNumberOfCharInTransactionJson, AccountMapper accountMapper, ValueValidatorService validatorService) {
        this.accountSpi = accountSpi;
        this.maxNumberOfCharInTransactionJson = maxNumberOfCharInTransactionJson;
        this.accountMapper = accountMapper;
        this.validatorService = validatorService;
    }

    public ResponseObject<Map<String, List<AccountDetails>>> getAccountDetailsList(boolean withBalance, boolean psuInvolved) {
        List<AccountDetails> accountDetailsList = accountMapper.mapFromSpiAccountDetailsList(accountSpi.readAccounts(withBalance, psuInvolved));
        Map<String, List<AccountDetails>> accountDetailsMap = new HashMap<>();
        accountDetailsMap.put("accountList", accountDetailsList);

        return new ResponseObject<>(accountDetailsMap);
    }

    public ResponseObject<List<Balances>> getBalancesList(String accountId, boolean psuInvolved) {
        List<Balances> result = accountMapper.mapFromSpiBalancesList(accountSpi.readBalances(accountId, psuInvolved));
        return new ResponseObject<>(result);
    }

    public ResponseObject<AccountReport> getAccountReport(String accountId, Date dateFrom,
                                                          Date dateTo, String transactionId,
                                                          boolean psuInvolved, String bookingStatus, boolean withBalance, boolean deltaList) {
        if (accountSpi.readAccountDetails(accountId, false, false) == null) {
            return new ResponseObject<>(new MessageError(new TppMessageInformation(MessageCategory.ERROR, MessageCode.RESOURCE_UNKNOWN_404)));
        } else {

            try {
                AccountReport accountReport = getAccountReport(accountId, dateFrom, dateTo, transactionId, psuInvolved, withBalance);

                return new ResponseObject<>(getReportAccordingMaxSize(accountReport, accountId));
            } catch (ValidationException ex) {

                TppMessageInformation tppMessageInformation = new TppMessageInformation(MessageCategory.ERROR, MessageCode.FORMAT_ERROR);
                tppMessageInformation.setText(ex.getMessage());

                return new ResponseObject<>(new MessageError(tppMessageInformation));
            }
        }
    }

    private AccountReport getAccountReport(String accountId, Date dateFrom, Date dateTo, String transactionId, boolean psuInvolved, boolean withBalance) {
        return StringUtils.isEmpty(transactionId)
                                      ? getAccountReportByPeriod(accountId, dateFrom, dateTo, psuInvolved, withBalance)
                                      : getAccountReportByTransaction(accountId, transactionId, psuInvolved, withBalance);
    }

    private AccountReport getAccountReportByPeriod(String accountId, Date dateFrom, Date dateTo, boolean psuInvolved, boolean withBalance) {
        validate_accountId_period(accountId, dateFrom, dateTo);
        return readTransactionsByPeriod(accountId, dateFrom, dateTo, psuInvolved, withBalance);
    }

    private AccountReport getAccountReportByTransaction(String accountId, String transactionId, boolean psuInvolved, boolean withBalance) {
        validate_accountId_transactionId(accountId, transactionId);
        return readTransactionsById(accountId, transactionId, psuInvolved, withBalance);
    }

    private AccountReport getReportAccordingMaxSize(AccountReport accountReport, String accountId) {

        String jsonReport = getJsonStringFromObject(accountReport);

        if (jsonReport.length() > maxNumberOfCharInTransactionJson) {
            return getAccountReportWithDownloadLink(accountId);
        }

        String urlToAccount = linkTo(AccountController.class).slash(accountId).toString();
        accountReport.get_links().setViewAccount(urlToAccount);
        return accountReport;
    }

    private String getJsonStringFromObject(Object obj) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting object {} to json", obj);
            return "";
        }
    }

    private AccountReport readTransactionsByPeriod(String accountId, Date dateFrom,
                                                   Date dateTo, boolean psuInvolved, boolean withBalance) { //NOPMD TODO review and check PMD assertion
        Optional<AccountReport> result = accountMapper.mapFromSpiAccountReport(accountSpi.readTransactionsByPeriod(accountId, dateFrom, dateTo, psuInvolved));

        return result.orElseGet(() -> new AccountReport(new Transactions[]{}, new Transactions[]{}, new Links()));
    }

    private AccountReport readTransactionsById(String accountId, String transactionId,
                                               boolean psuInvolved, boolean withBalance) { //NOPMD TODO review and check PMD assertion
        Optional<AccountReport> result = accountMapper.mapFromSpiAccountReport(accountSpi.readTransactionsById(accountId, transactionId, psuInvolved));

        return result.orElseGet(() -> new AccountReport(new Transactions[]{},
                                                        new Transactions[]{},
                                                        new Links()
        ));

    }

    public AccountReport getAccountReportWithDownloadLink(String accountId) {
        // todo further we should implement real flow for downloading file
        String urlToDownload = linkTo(AccountController.class).slash(accountId).slash("transactions/download").toString();
        Links downloadLink = new Links();
        downloadLink.setDownload(urlToDownload);
        return new AccountReport(null, null, downloadLink);
    }

    public ResponseObject<AccountDetails> getAccountDetails(String accountId, boolean withBalance, boolean psuInvolved) {
        AccountDetails accountDetails = accountMapper.mapFromSpiAccountDetails(accountSpi.readAccountDetails(accountId, withBalance, psuInvolved));

        return new ResponseObject<>(accountDetails);
    }

    // Validation
    private void validate_accountId_period(String accountId, Date dateFrom, Date dateTo) {
        ValidationGroup fieldValidator = new ValidationGroup();
        fieldValidator.setAccountId(accountId);
        fieldValidator.setDateFrom(dateFrom);
        fieldValidator.setDateTo(dateTo);

        validatorService.validate(fieldValidator, ValidationGroup.AccountIdAndPeriodIsValid.class);
    }

    private void validate_accountId_transactionId(String accountId, String transactionId) {
        ValidationGroup fieldValidator = new ValidationGroup();
        fieldValidator.setAccountId(accountId);
        fieldValidator.setTransactionId(transactionId);

        validatorService.validate(fieldValidator, ValidationGroup.AccountIdAndTransactionIdIsValid.class);
    }
}
