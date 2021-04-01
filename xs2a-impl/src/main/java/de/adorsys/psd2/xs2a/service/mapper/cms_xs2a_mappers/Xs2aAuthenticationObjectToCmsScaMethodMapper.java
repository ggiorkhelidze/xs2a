/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
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

package de.adorsys.psd2.xs2a.service.mapper.cms_xs2a_mappers;

import de.adorsys.psd2.consent.api.CmsScaMethod;
import de.adorsys.psd2.xs2a.core.authorisation.Xs2aAuthenticationObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class Xs2aAuthenticationObjectToCmsScaMethodMapper {
    @NotNull
    public List<CmsScaMethod> mapToCmsScaMethods(@NotNull List<Xs2aAuthenticationObject> authenticationObjects) {
        return authenticationObjects.stream()
                   .map(this::mapToCmsScaMethod)
                   .collect(Collectors.toList());
    }

    @NotNull
    public CmsScaMethod mapToCmsScaMethod(@NotNull Xs2aAuthenticationObject authenticationObject) {
        return new CmsScaMethod(authenticationObject.getAuthenticationMethodId(),
                                authenticationObject.isDecoupled());
    }
}
