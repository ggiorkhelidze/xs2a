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

package de.adorsys.psd2.xs2a.service.authorization.pis;

import de.adorsys.psd2.consent.api.CmsResponse;
import de.adorsys.psd2.consent.api.authorisation.*;
import de.adorsys.psd2.consent.api.pis.PisCommonPaymentResponse;
import de.adorsys.psd2.consent.api.service.AuthorisationServiceEncrypted;
import de.adorsys.psd2.xs2a.core.authorisation.Authorisation;
import de.adorsys.psd2.xs2a.core.authorisation.AuthorisationType;
import de.adorsys.psd2.xs2a.core.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.core.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.core.error.ErrorType;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.mapper.ServiceType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.AuthorisationScaApproachResponse;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppRedirectUri;
import de.adorsys.psd2.xs2a.domain.authorisation.UpdateAuthorisationRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.ScaApproachResolver;
import de.adorsys.psd2.xs2a.service.authorization.AuthorisationChainResponsibilityService;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.AuthorisationProcessorResponse;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.PisAuthorisationProcessorRequest;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.PisCancellationAuthorisationProcessorRequest;
import de.adorsys.psd2.xs2a.service.consent.Xs2aPisCommonPaymentService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.cms_xs2a_mappers.Xs2aPisCommonPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPaymentMapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiStartAuthorisationResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.PaymentAuthorisationSpi;
import de.adorsys.psd2.xs2a.spi.service.SpiPayment;
import de.adorsys.psd2.xs2a.web.mapper.TppRedirectUriMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PisAuthorisationService {
    private final AuthorisationServiceEncrypted authorisationServiceEncrypted;
    private final Xs2aPisCommonPaymentMapper pisCommonPaymentMapper;
    private final ScaApproachResolver scaApproachResolver;
    private final RequestProviderService requestProviderService;
    private final TppRedirectUriMapper tppRedirectUriMapper;
    private final AuthorisationChainResponsibilityService authorisationChainResponsibilityService;
    private final PaymentAuthorisationSpi paymentAuthorisationSpi;
    private final SpiContextDataProvider spiContextDataProvider;
    private final Xs2aPisCommonPaymentService xs2aPisCommonPaymentService;
    private final Xs2aToSpiPaymentMapper xs2aToSpiPaymentMapper;
    private final SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;
    private final SpiErrorMapper spiErrorMapper;

    /**
     * Sends a POST request to CMS to store created pis authorisation
     *
     * @param paymentId String representation of identifier of stored payment
     * @param psuData   PsuIdData container of authorisation data about PSU
     * @return a response object containing authorisation id
     */
    public CreateAuthorisationResponse createPisAuthorisation(String paymentId, PsuIdData psuData) {
        TppRedirectUri redirectURIs = tppRedirectUriMapper.mapToTppRedirectUri(requestProviderService.getTppRedirectURI(), requestProviderService.getTppNokRedirectURI());

        CreateAuthorisationRequest request = new CreateAuthorisationRequest(psuData, scaApproachResolver.resolveScaApproach(), redirectURIs);
        CmsResponse<CreateAuthorisationResponse> cmsResponse = authorisationServiceEncrypted.createAuthorisation(new PisAuthorisationParentHolder(paymentId), request);

        if (cmsResponse.hasError()) {
            log.info("Payment-ID [{}]. Create PIS authorisation has failed: can't save authorisation to cms DB",
                     paymentId);
            return null;
        }

        return cmsResponse.getPayload();
    }

    public Xs2aStartAuthorisationResponse startAuthorisation (String paymentId, PsuIdData psuIdData) {

        SpiContextData contextData = spiContextDataProvider.provideWithPsuIdData(psuIdData);
        SpiPayment spiPayment = getSpiPayment(paymentId);
        SpiAspspConsentDataProvider aspspConsentDataProvider = aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(paymentId);
        SpiResponse<SpiStartAuthorisationResponse> spiResponse = paymentAuthorisationSpi.startAuthorization(contextData, scaApproachResolver.resolveScaApproach(), spiPayment, aspspConsentDataProvider);

        if (spiResponse.hasError()) {
            ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);
            return new Xs2aStartAuthorisationResponse(errorHolder);
        }
        return getResponse(spiResponse);
    }

    /**
     * Updates PIS authorisation according to psu's sca methods with embedded and decoupled SCA approach
     *
     * @param request     Provides transporting data when updating pis authorisation
     * @param scaApproach current SCA approach, preferred by the server
     * @return update pis authorisation response, which contains payment id, authorisation id, sca status, psu message and links
     */
    public Xs2aUpdatePisCommonPaymentPsuDataResponse updatePisAuthorisation(Xs2aUpdatePisCommonPaymentPsuDataRequest request, ScaApproach scaApproach) {
        String authorisationId = request.getAuthorisationId();
        CmsResponse<Authorisation> pisAuthorisationResponse = authorisationServiceEncrypted.getAuthorisationById(authorisationId);
        if (pisAuthorisationResponse.hasError()) {
            ErrorHolder errorHolder = ErrorHolder.builder(ErrorType.PIS_404)
                                          .tppMessages(TppMessageInformation.of(MessageErrorCode.RESOURCE_UNKNOWN_404_NO_AUTHORISATION))
                                          .build();
            log.info("Payment-ID [{}], Authorisation-ID [{}]. Updating PIS authorisation PSU Data has failed: authorisation is not found by id.",
                     request.getPaymentId(), request.getAuthorisationId());
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(errorHolder, request.getPaymentId(), authorisationId, request.getPsuData());

        }

        Authorisation response = pisAuthorisationResponse.getPayload();

        return (Xs2aUpdatePisCommonPaymentPsuDataResponse) authorisationChainResponsibilityService.apply(
            new PisAuthorisationProcessorRequest(scaApproach,
                                                 response.getScaStatus(),
                                                 request,
                                                 response));
    }

    /**
     * Updates PIS cancellation authorisation according to psu's sca methods with embedded and decoupled SCA approach
     *
     * @param request     Provides transporting data when updating pis cancellation authorisation
     * @param scaApproach current SCA approach, preferred by the server
     * @return update pis authorisation response, which contains payment id, authorisation id, sca status, psu message and links
     */
    public Xs2aUpdatePisCommonPaymentPsuDataResponse updatePisCancellationAuthorisation(Xs2aUpdatePisCommonPaymentPsuDataRequest request, ScaApproach scaApproach) {
        String authorisationId = request.getAuthorisationId();
        CmsResponse<Authorisation> pisCancellationAuthorisationResponse = authorisationServiceEncrypted.getAuthorisationById(request.getAuthorisationId());
        if (pisCancellationAuthorisationResponse.hasError()) {
            log.warn("Payment-ID [{}], Authorisation-ID [{}]. Updating PIS Payment Cancellation authorisation PSU Data has failed: authorisation is not found by id.",
                     request.getPaymentId(), request.getAuthorisationId());

            ErrorHolder errorHolder = ErrorHolder.builder(ErrorType.PIS_404)
                                          .tppMessages(TppMessageInformation.of(MessageErrorCode.RESOURCE_UNKNOWN_404_NO_CANC_AUTHORISATION))
                                          .build();
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(errorHolder, request.getPaymentId(), authorisationId, request.getPsuData());
        }

        Authorisation response = pisCancellationAuthorisationResponse.getPayload();

        return (Xs2aUpdatePisCommonPaymentPsuDataResponse) authorisationChainResponsibilityService.apply(
            new PisCancellationAuthorisationProcessorRequest(scaApproach,
                                                             response.getScaStatus(),
                                                             request,
                                                             response));
    }

    /**
     * Sends a POST request to CMS to store created pis authorisation cancellation
     *
     * @param paymentId String representation of identifier of payment ID
     * @param psuData   PsuIdData container of authorisation data about PSU
     * @return long representation of identifier of stored pis authorisation cancellation
     */
    public CreateAuthorisationResponse createPisAuthorisationCancellation(String paymentId, PsuIdData psuData) {
        TppRedirectUri redirectURIs = tppRedirectUriMapper.mapToTppRedirectUri(requestProviderService.getTppRedirectURI(), requestProviderService.getTppNokRedirectURI());

        CreateAuthorisationRequest request = new CreateAuthorisationRequest(psuData, scaApproachResolver.resolveScaApproach(), redirectURIs);
        CmsResponse<CreateAuthorisationResponse> cmsResponse = authorisationServiceEncrypted.createAuthorisation(new PisCancellationAuthorisationParentHolder(paymentId), request);

        if (cmsResponse.hasError()) {
            log.info("Payment-ID [{}]. Create PIS Payment Cancellation Authorisation has failed. Can't find Payment Data by id or Payment is Finalised.",
                     paymentId);
            return null;
        }

        return cmsResponse.getPayload();
    }

    /**
     * Sends a GET request to CMS to get cancellation authorisation sub resources
     *
     * @param paymentId String representation of identifier of payment ID
     * @return list of pis authorisation IDs
     */
    public Optional<List<String>> getCancellationAuthorisationSubResources(String paymentId) {
        CmsResponse<List<String>> cmsResponse = authorisationServiceEncrypted.getAuthorisationsByParentId(new PisCancellationAuthorisationParentHolder(paymentId));

        if (cmsResponse.hasError()) {
            return Optional.empty();
        }

        return Optional.ofNullable(cmsResponse.getPayload());
    }

    /**
     * Sends a GET request to CMS to get authorisation sub resources
     *
     * @param paymentId String representation of identifier of payment ID
     * @return list of pis authorisation IDs
     */
    public Optional<List<String>> getAuthorisationSubResources(String paymentId) {
        CmsResponse<List<String>> cmsResponse = authorisationServiceEncrypted.getAuthorisationsByParentId(new PisAuthorisationParentHolder(paymentId));

        if (cmsResponse.hasError()) {
            return Optional.empty();
        }

        return Optional.ofNullable(cmsResponse.getPayload());
    }

    /**
     * Gets SCA status of the authorisation
     *
     * @param paymentId       String representation of the payment identifier
     * @param authorisationId String representation of the authorisation identifier
     * @return SCA status of the authorisation
     */
    public Optional<ScaStatus> getAuthorisationScaStatus(String paymentId, String authorisationId) {
        CmsResponse<ScaStatus> cmsResponse = authorisationServiceEncrypted.getAuthorisationScaStatus(authorisationId, new PisAuthorisationParentHolder(paymentId));

        if (cmsResponse.hasError()) {
            return Optional.empty();
        }

        return Optional.ofNullable(cmsResponse.getPayload());
    }

    /**
     * Gets SCA status of the cancellation authorisation
     *
     * @param paymentId      String representation of the payment identifier
     * @param authorisationId String representation of the authorisation identifier
     * @return SCA status of the authorisation
     */
    public Optional<ScaStatus> getCancellationAuthorisationScaStatus(String paymentId, String authorisationId) {
        CmsResponse<ScaStatus> cmsResponse = authorisationServiceEncrypted.getAuthorisationScaStatus(authorisationId, new PisCancellationAuthorisationParentHolder(paymentId));

        if (cmsResponse.hasError()) {
            return Optional.empty();
        }

        return Optional.ofNullable(cmsResponse.getPayload());
    }

    /**
     * Gets SCA approach of the authorisation by its id and type
     *
     * @param authorisationId   String representation of the authorisation identifier
     * @return SCA approach of the authorisation
     */
    public Optional<AuthorisationScaApproachResponse> getAuthorisationScaApproach(String authorisationId) {
        CmsResponse<AuthorisationScaApproachResponse> cmsResponse = authorisationServiceEncrypted.getAuthorisationScaApproach(authorisationId);

        if (cmsResponse.hasError()) {
            return Optional.empty();
        }

        return Optional.ofNullable(cmsResponse.getPayload());
    }

    public void updateAuthorisation(UpdateAuthorisationRequest request,
                                    AuthorisationProcessorResponse response) {
        if (response.hasError()) {
            log.warn("Payment-ID [{}], Authorisation-ID [{}]. Updating PIS authorisation PSU Data has failed. Error msg: [{}]",
                     request.getBusinessObjectId(), request.getAuthorisationId(), response.getErrorHolder());
        } else {
            authorisationServiceEncrypted.updateAuthorisation(request.getAuthorisationId(),
                                                              pisCommonPaymentMapper.mapToUpdateAuthorisationRequest(response, AuthorisationType.PIS_CREATION));
        }
    }

    public void updateCancellationAuthorisation(UpdateAuthorisationRequest request,
                                                AuthorisationProcessorResponse response) {
        if (response.hasError()) {
            log.warn("Payment-ID [{}], Authorisation-ID [{}]. Updating PIS Payment Cancellation authorisation PSU Data has failed:. Error msg: [{}]",
                     request.getBusinessObjectId(), request.getAuthorisationId(), response.getErrorHolder());
        } else {
            authorisationServiceEncrypted.updateAuthorisation(request.getAuthorisationId(),
                                                              pisCommonPaymentMapper.mapToUpdateAuthorisationRequest(response, AuthorisationType.PIS_CANCELLATION));
        }
    }

    private SpiPayment getSpiPayment(String encryptedPaymentId) {
        Optional<PisCommonPaymentResponse> commonPaymentById = xs2aPisCommonPaymentService.getPisCommonPaymentById(encryptedPaymentId);
        return commonPaymentById
                   .map(xs2aToSpiPaymentMapper::mapToSpiPayment)
                   .orElse(null);
    }

    private Xs2aStartAuthorisationResponse getResponse(SpiResponse<SpiStartAuthorisationResponse> response) {
        Xs2aStartAuthorisationResponse resultResponse = new Xs2aStartAuthorisationResponse();
        resultResponse.setPsuMessage(response.getPayload().getPsuMessage());
        resultResponse.setScaApproach(response.getPayload().getScaApproach());
        resultResponse.setTppMessageInformation(response.getPayload().getTppMessageInformation());

        return resultResponse;
    }
}
