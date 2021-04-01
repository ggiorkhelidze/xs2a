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

import de.adorsys.psd2.consent.api.CmsAddress;
import de.adorsys.psd2.consent.domain.payment.PisAddress;
import de.adorsys.psd2.core.payment.model.Xs2aPisAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CmsAddressMapper {

    Xs2aPisAddress mapToAddress(CmsAddress cmsAddress);

    @Mapping(target = "postCode", source = "postalCode")
    @Mapping(target = "streetName", source = "street")
    @Mapping(target = "townName", source = "city")
    CmsAddress mapToCmsAddress(PisAddress pisAddress);
}
