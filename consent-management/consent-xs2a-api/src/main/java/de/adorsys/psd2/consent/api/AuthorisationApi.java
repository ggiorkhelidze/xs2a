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

package de.adorsys.psd2.consent.api;

import de.adorsys.psd2.consent.api.authorisation.CreateAuthorisationRequest;
import de.adorsys.psd2.consent.api.authorisation.CreateAuthorisationResponse;
import de.adorsys.psd2.consent.api.authorisation.UpdateAuthorisationRequest;
import de.adorsys.psd2.consent.api.config.InternalCmsXs2aApiTagName;
import de.adorsys.psd2.xs2a.core.authorisation.Authorisation;
import de.adorsys.psd2.xs2a.core.authorisation.AuthorisationType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.sca.AuthorisationScaApproachResponse;
import de.adorsys.psd2.xs2a.core.sca.Xs2aScaStatus;
import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping(path = "api/v1/")
@Api(value = "api/v1/", tags = InternalCmsXs2aApiTagName.AUTHORISATIONS)
public interface AuthorisationApi {

    @PostMapping(path = "/{authorisation-type}/{parent-id}/authorisations")
    @ApiOperation(value = "Create consent authorization for given consent id.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Created"),
        @ApiResponse(code = 404, message = "Not Found")})
    ResponseEntity<CreateAuthorisationResponse> createConsentAuthorisation(
        @PathVariable("authorisation-type") AuthorisationType authorisationType,
        @PathVariable("parent-id") String parentId,
        @RequestBody CreateAuthorisationRequest authorisationRequest);

    @GetMapping(path = "/authorisations/{authorisation-id}")
    @ApiOperation(value = "Getting consent authorization.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    ResponseEntity<Authorisation> getAuthorisation(
        @ApiParam(name = "authorisation-id",
            value = "The consent authorization identification assigned to the created authorization.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId);

    @PutMapping(path = "/authorisations/{authorisation-id}")
    @ApiOperation(value = "Update consent authorization.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    ResponseEntity<Void> updateAuthorisation(
        @ApiParam(name = "authorization-id",
            value = "The consent authorization identification assigned to the created authorization.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId,
        @RequestBody UpdateAuthorisationRequest authorisationRequest);

    @PutMapping(path = "/authorisations/{authorisation-id}/status/{status}")
    @ApiOperation(value = "Update consent authorisation status.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    ResponseEntity<Void> updateAuthorisationStatus(
        @ApiParam(name = "authorisation-id",
            value = "The consent authorisation identification assigned to the created authorisation.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId,
        @ApiParam(value = "The following code values are permitted 'VALID', 'REJECTED', 'REVOKED_BY_PSU', 'TERMINATED_BY_TPP'. These values might be extended by ASPSP by more values.",
            example = "VALID",
            required = true)
        @PathVariable("status") String scaStatus);

    @GetMapping(path = "/{authorisation-type}/{parent-id}/authorisations/{authorisation-id}/status")
    @ApiOperation(value = "Gets SCA status of consent authorisation.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    ResponseEntity<Xs2aScaStatus> getAuthorisationScaStatus(
        @PathVariable("authorisation-type") AuthorisationType authorisationType,
        @PathVariable("parent-id") String parentId,
        @PathVariable("authorisation-id") String authorisationId);

    @GetMapping(path = "/{authorisation-type}/{parent-id}/authorisations")
    @ApiOperation(value = "Gets list of consent authorisation IDs by consent ID")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    ResponseEntity<List<String>> getAuthorisationsByParentId(
        @PathVariable("authorisation-type") AuthorisationType authorisationType,
        @PathVariable("parent-id") String parentId);

    @GetMapping(path = "/authorisations/{authorisation-id}/authentication-methods/{authentication-method-id}")
    @ApiOperation(value = "Checks if requested authentication method is decoupled")
    @ApiResponse(code = 200, message = "OK")
    ResponseEntity<Boolean> isAuthenticationMethodDecoupled(
        @ApiParam(name = "authorisation-id",
            value = "Consent authorisation identification",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId,
        @ApiParam(name = "authentication-method-id",
            value = "Authentication method identification",
            example = "sms",
            required = true)
        @PathVariable("authentication-method-id") String authenticationMethodId);

    @PostMapping(path = "/authorisations/{authorisation-id}/authentication-methods")
    @ApiOperation(value = "Saves authentication methods in authorisation")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 404, message = "Not Found")})
    ResponseEntity<Void> saveAuthenticationMethods(
        @ApiParam(name = "authorisation-id",
            value = "The consent authorisation identification.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId,
        @RequestBody List<CmsScaMethod> methods);

    @PutMapping(path = "/authorisations/{authorisation-id}/sca-approach/{sca-approach}")
    @ApiOperation(value = "Updates AIS SCA approach in authorisation")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    ResponseEntity<Boolean> updateScaApproach(
        @ApiParam(name = "authorisation-id",
            value = "The consent authorisation identification.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId,
        @ApiParam(name = "sca-approach",
            value = "Chosen SCA approach.",
            example = "REDIRECT",
            required = true)
        @PathVariable("sca-approach") ScaApproach scaApproach);

    @GetMapping(path = "/authorisations/{authorisation-id}/sca-approach")
    @ApiOperation(value = "Gets SCA approach of the consent authorisation by its ID")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    ResponseEntity<AuthorisationScaApproachResponse> getAuthorisationScaApproach(
        @ApiParam(name = "authorisation-id",
            value = "The consent authorisation identification.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId);
}
