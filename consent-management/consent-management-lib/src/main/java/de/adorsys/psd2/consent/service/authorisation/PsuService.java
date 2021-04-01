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

package de.adorsys.psd2.consent.service.authorisation;

import de.adorsys.psd2.consent.domain.CmsPsuData;
import de.adorsys.psd2.consent.service.mapper.PsuDataMapper;
import de.adorsys.psd2.consent.service.psu.CmsPsuService;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PsuService {
    protected final CmsPsuService cmsPsuService;
    protected final PsuDataMapper psuDataMapper;

    public CmsPsuData mapToPsuData(PsuIdData psuData, String instanceId) {
        return psuDataMapper.mapToPsuData(psuData, instanceId);
    }

    public Optional<CmsPsuData> definePsuDataForAuthorisation(CmsPsuData psuData, List<CmsPsuData> psuDataList) {
        return cmsPsuService.definePsuDataForAuthorisation(psuData, psuDataList);
    }

    public List<CmsPsuData> enrichPsuData(CmsPsuData psuData, List<CmsPsuData> psuDataList) {
        return cmsPsuService.enrichPsuData(psuData, psuDataList);
    }

    public boolean isPsuDataRequestCorrect(CmsPsuData psuRequest, CmsPsuData psuData) {
        return cmsPsuService.isPsuDataRequestCorrect(psuRequest, psuData);
    }
}
