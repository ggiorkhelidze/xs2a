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

package de.adorsys.psd2.xs2a.service.authorization.processor.service;

import de.adorsys.psd2.xs2a.core.authorisation.AuthenticationObject;
import de.adorsys.psd2.xs2a.core.authorisation.Authorisation;
import de.adorsys.psd2.xs2a.core.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.core.mapper.ServiceType;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.domain.authorisation.UpdateAuthorisationRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.PaymentServicesHolder;
import de.adorsys.psd2.xs2a.service.PisMappersHolder;
import de.adorsys.psd2.xs2a.service.SpiCommonService;
import de.adorsys.psd2.xs2a.service.authorization.Xs2aAuthorisationService;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisScaAuthorisationService;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.AuthorisationProcessorRequest;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.AuthorisationProcessorResponse;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.*;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiPaymentExecutionResponse;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.PaymentAuthorisationSpi;
import de.adorsys.psd2.xs2a.spi.service.SpiPayment;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.adorsys.psd2.xs2a.core.sca.ScaStatus.PSUAUTHENTICATED;

@Service
public class PisAuthorisationProcessorServiceImpl extends PaymentBaseAuthorisationProcessorService {

    private final SpiCommonService spiService;
    private final PaymentAuthorisationSpi paymentAuthorisationSpi;
    private final PisMappersHolder pisMappersHolder;
    private final Xs2aAuthorisationService xs2aAuthorisationService;
    private final PaymentServicesHolder paymentServicesHolder;

    public PisAuthorisationProcessorServiceImpl(List<PisScaAuthorisationService> services,
                                                Xs2aAuthorisationService xs2aAuthorisationService,
                                                PaymentServicesHolder paymentServicesHolder,
                                                PisMappersHolder pisMappersHolder, SpiCommonService spiService,
                                                PaymentAuthorisationSpi paymentAuthorisationSpi) {
        super(services, xs2aAuthorisationService, paymentServicesHolder, pisMappersHolder, spiService);
        this.spiService = spiService;
        this.paymentAuthorisationSpi = paymentAuthorisationSpi;
        this.pisMappersHolder = pisMappersHolder;
        this.xs2aAuthorisationService = xs2aAuthorisationService;
        this.paymentServicesHolder = paymentServicesHolder;
    }

    @Override
    public void updateAuthorisation(AuthorisationProcessorRequest request, AuthorisationProcessorResponse response) {
        PisScaAuthorisationService authorizationService = getService(request.getScaApproach());
        authorizationService.updateAuthorisation(request.getUpdateAuthorisationRequest(), response);
    }

    @Override
    public AuthorisationProcessorResponse doScaReceived(AuthorisationProcessorRequest authorisationProcessorRequest) {
        Xs2aUpdatePisCommonPaymentPsuDataRequest request = (Xs2aUpdatePisCommonPaymentPsuDataRequest) authorisationProcessorRequest.getUpdateAuthorisationRequest();
        return request.isUpdatePsuIdentification()
                   ? applyIdentification(authorisationProcessorRequest)
                   : applyAuthorisation(authorisationProcessorRequest);
    }

    @Override
    SpiResponse<SpiPaymentExecutionResponse> verifyScaAuthorisationAndExecutePayment(Authorisation authorisation,
                                                                                     SpiPayment payment, SpiScaConfirmation spiScaConfirmation,
                                                                                     SpiContextData contextData, SpiAspspConsentDataProvider spiAspspConsentDataProvider) {
        return paymentServicesHolder.verifyScaAuthorisationAndExecutePaymentWithPaymentResponse(contextData,
                                                                                                spiScaConfirmation,
                                                                                                payment,
                                                                                                spiAspspConsentDataProvider);
    }

    @Override
    void updatePaymentDataByPaymentResponse(String paymentId, SpiResponse<SpiPaymentExecutionResponse> spiResponse) {
        SpiPaymentExecutionResponse payload = spiResponse.getPayload();
        TransactionStatus paymentStatus = payload.getTransactionStatus();

        if (paymentStatus == TransactionStatus.PATC) {
            paymentServicesHolder.updateMultilevelSca(paymentId, true);
        }

        paymentServicesHolder.updatePaymentStatus(paymentId, paymentStatus);
    }

    @Override
    public AuthorisationProcessorResponse doScaExempted(AuthorisationProcessorRequest authorisationProcessorRequest) {
        UpdateAuthorisationRequest request = authorisationProcessorRequest.getUpdateAuthorisationRequest();

        Authorisation authorisation = authorisationProcessorRequest.getAuthorisation();
        PsuIdData psuData = extractPsuIdData(request, authorisation);
        String authorisationId = request.getAuthorisationId();
        SpiPayment payment = getSpiPayment(request.getBusinessObjectId());
        SpiContextData contextData = spiService.provideWithPsuIdData(psuData);
        SpiAspspConsentDataProvider aspspConsentDataProvider = spiService.getSpiAspspDataProviderFor(request.getBusinessObjectId());

        SpiResponse<SpiCurrencyConversionInfo> conversionInfoSpiResponse =
            paymentServicesHolder.getCurrencyConversionInfo(contextData, payment, authorisationId, aspspConsentDataProvider);
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = conversionInfoSpiResponse.getPayload();

        return Xs2aUpdatePisCommonPaymentPsuDataResponse
                   .buildWithCurrencyConversionInfo(ScaStatus.EXEMPTED,
                                                    request.getBusinessObjectId(),
                                                    request.getAuthorisationId(),
                                                    request.getPsuData(),
                                                    pisMappersHolder
                                                        .toXs2aCurrencyConversionInfo(spiCurrencyConversionInfo));
    }

    @Override
    boolean needProcessExemptedSca(PaymentType paymentType, boolean isScaExempted) {
        return isScaExempted && paymentType != PaymentType.PERIODIC;
    }

    @Override
    SpiResponse<SpiAvailableScaMethodsResponse> requestAvailableScaMethods(SpiPayment payment, SpiAspspConsentDataProvider aspspConsentDataProvider, SpiContextData contextData) {
        return paymentAuthorisationSpi.requestAvailableScaMethods(contextData, payment, aspspConsentDataProvider);
    }

    @Override
    SpiResponse<SpiPsuAuthorisationResponse> authorisePsu(Xs2aUpdatePisCommonPaymentPsuDataRequest request, SpiPayment payment,
                                                          SpiAspspConsentDataProvider aspspConsentDataProvider, SpiPsuData spiPsuData,
                                                          SpiContextData contextData, String authorisationId) {
        return paymentAuthorisationSpi.authorisePsu(contextData, authorisationId, spiPsuData, request.getPassword(), payment, aspspConsentDataProvider);
    }

    @Override
    SpiResponse<SpiAuthorizationCodeResult> requestAuthorisationCode(SpiPayment payment, String authenticationMethodId,
                                                                     SpiContextData spiContextData, SpiAspspConsentDataProvider spiAspspConsentDataProvider) {
        return paymentAuthorisationSpi.requestAuthorisationCode(spiContextData, authenticationMethodId, payment, spiAspspConsentDataProvider);
    }

    @Override
    Xs2aUpdatePisCommonPaymentPsuDataResponse proceedDecoupledApproach(Xs2aUpdatePisCommonPaymentPsuDataRequest request, SpiPayment payment, String authenticationMethodId) {
        return paymentServicesHolder.proceedDecoupledInitiation(request, payment, authenticationMethodId);
    }

    @Override
    Xs2aUpdatePisCommonPaymentPsuDataResponse executePaymentWithoutSca(AuthorisationProcessorRequest authorisationProcessorRequest, PsuIdData psuData, PaymentType paymentType, SpiPayment payment, SpiContextData contextData, ScaStatus resultScaStatus) {
        Xs2aUpdatePisCommonPaymentPsuDataRequest request = (Xs2aUpdatePisCommonPaymentPsuDataRequest) authorisationProcessorRequest.getUpdateAuthorisationRequest();
        String authorisationId = request.getAuthorisationId();
        String paymentId = request.getPaymentId();

        final SpiAspspConsentDataProvider aspspConsentDataProvider = spiService.getSpiAspspDataProviderFor(paymentId);

        SpiResponse<SpiPaymentExecutionResponse> spiResponse = paymentServicesHolder.executePaymentWithoutSca(contextData, payment, aspspConsentDataProvider);

        if (spiResponse.hasError()) {
            ErrorHolder errorHolder = spiService.mapToErrorHolder(spiResponse, ServiceType.PIS);
            writeErrorLog(authorisationProcessorRequest, psuData, errorHolder, "Execute payment without SCA has failed.");
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(errorHolder, paymentId, authorisationId, psuData);
        }

        TransactionStatus paymentStatus = spiResponse.getPayload().getTransactionStatus();

        if (paymentStatus == TransactionStatus.PATC) {
            paymentServicesHolder.updateMultilevelSca(paymentId, true);
        }

        paymentServicesHolder.updatePaymentStatus(paymentId, paymentStatus);

        SpiResponse<SpiCurrencyConversionInfo> conversionInfoSpiResponse =
            paymentServicesHolder.getCurrencyConversionInfo(contextData, payment, authorisationId, aspspConsentDataProvider);
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = conversionInfoSpiResponse.getPayload();

        return Xs2aUpdatePisCommonPaymentPsuDataResponse
                   .buildWithCurrencyConversionInfo(resultScaStatus,
                                                    paymentId,
                                                    authorisationId,
                                                    psuData,
                                                    pisMappersHolder
                                                        .toXs2aCurrencyConversionInfo(spiCurrencyConversionInfo));
    }

    @Override
    Xs2aUpdatePisCommonPaymentPsuDataResponse getXs2aUpdatePisCommonPaymentPsuDataResponse(ScaStatus scaStatus, SpiPayment payment, SpiContextData contextData, SpiAspspConsentDataProvider spiAspspConsentDataProvider, PsuIdData psuData, String authorisationId) {
        SpiResponse<SpiCurrencyConversionInfo> conversionInfoSpiResponse =
            paymentServicesHolder.getCurrencyConversionInfo(contextData, payment, authorisationId, spiAspspConsentDataProvider);
        SpiCurrencyConversionInfo spiCurrencyConversionInfo = conversionInfoSpiResponse.getPayload();

        return Xs2aUpdatePisCommonPaymentPsuDataResponse
                   .buildWithCurrencyConversionInfo(scaStatus, payment.getPaymentId(), authorisationId, psuData,
                                                    pisMappersHolder
                                                        .toXs2aCurrencyConversionInfo(spiCurrencyConversionInfo)
                   );
    }

    @Override
    Xs2aUpdatePisCommonPaymentPsuDataResponse buildUpdateResponseWhenScaMethodsAreMultiple(Xs2aUpdatePisCommonPaymentPsuDataRequest request,
                                                                                           PsuIdData psuData,
                                                                                           List<AuthenticationObject> spiScaMethods,
                                                                                           SpiPayment payment,
                                                                                           SpiAspspConsentDataProvider aspspConsentDataProvider,
                                                                                           SpiContextData contextData) {
        xs2aAuthorisationService.saveAuthenticationMethods(request.getAuthorisationId(), spiScaMethods);

        SpiResponse<SpiCurrencyConversionInfo> conversionInfoSpiResponse =
            paymentServicesHolder
                .getCurrencyConversionInfo(contextData, payment, request.getAuthorisationId(), aspspConsentDataProvider);

        SpiCurrencyConversionInfo spiCurrencyConversionInfo = conversionInfoSpiResponse.getPayload();

        Xs2aUpdatePisCommonPaymentPsuDataResponse response =
            Xs2aUpdatePisCommonPaymentPsuDataResponse
                .buildWithCurrencyConversionInfo(PSUAUTHENTICATED,
                                                 request.getPaymentId(),
                                                 request.getAuthorisationId(),
                                                 psuData,
                                                 pisMappersHolder
                                                     .toXs2aCurrencyConversionInfo(spiCurrencyConversionInfo)
                );
        response.setAvailableScaMethods(spiScaMethods);
        return response;
    }
}
