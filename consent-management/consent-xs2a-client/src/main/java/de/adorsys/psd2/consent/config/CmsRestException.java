/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
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

package de.adorsys.psd2.consent.config;

import de.adorsys.psd2.consent.api.CmsError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CmsRestException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String message;
    private final CmsError cmsError;

    CmsRestException(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
        this.message = null;
        this.cmsError = null;
    }

    CmsRestException(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.cmsError = CmsError.getByName(message).orElse(CmsError.TECHNICAL_ERROR);
    }
}
