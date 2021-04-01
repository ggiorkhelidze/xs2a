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

package de.adorsys.psd2.consent.service.mapper;

import de.adorsys.psd2.consent.domain.CmsPsuData;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.xs2a.reader.JsonReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static de.adorsys.psd2.consent.aspsp.api.config.CmsPsuApiDefaultValue.DEFAULT_SERVICE_INSTANCE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PsuDataMapperTest {
    private final static JsonReader jsonReader = new JsonReader();
    @Spy
    private PsuDataMapper psuDataMapper;

    @Test
    void mapToPsuDataList() {
        PsuIdData psuIdData = jsonReader.getObjectFromFile("json/service/mapper/psu-id-data.json", PsuIdData.class);
        CmsPsuData expected = jsonReader.getObjectFromFile("json/service/mapper/psu-data.json", CmsPsuData.class);
        List<CmsPsuData> expectedList = Collections.singletonList(expected);

        List<CmsPsuData> psuDataList = psuDataMapper.mapToPsuDataList(Collections.singletonList(psuIdData), DEFAULT_SERVICE_INSTANCE_ID);

        verify(psuDataMapper, times(1)).mapToPsuData(psuIdData, DEFAULT_SERVICE_INSTANCE_ID);
        assertEquals(expectedList, psuDataList);
    }

    @Test
    void mapToPsuIdDataList() {
        CmsPsuData psuData = jsonReader.getObjectFromFile("json/service/mapper/psu-data.json", CmsPsuData.class);
        PsuIdData expected = jsonReader.getObjectFromFile("json/service/mapper/psu-id-data.json", PsuIdData.class);
        List<PsuIdData> expectedList = Collections.singletonList(expected);

        List<PsuIdData> psuDataList = psuDataMapper.mapToPsuIdDataList(Collections.singletonList(psuData));

        verify(psuDataMapper, times(1)).mapToPsuIdData(psuData);
        assertEquals(expectedList, psuDataList);
    }

    @Test
    void mapToPsuData_WithAdditionalPsuIdData() {
        PsuIdData psuIdData = jsonReader.getObjectFromFile("json/service/mapper/psu-id-data.json", PsuIdData.class);
        CmsPsuData expected = jsonReader.getObjectFromFile("json/service/mapper/psu-data.json", CmsPsuData.class);

        CmsPsuData psuData = psuDataMapper.mapToPsuData(psuIdData, DEFAULT_SERVICE_INSTANCE_ID);

        assertEquals(expected, psuData);
    }

    @Test
    void mapToPsuIdData_WithAdditionalPsuData() {
        CmsPsuData psuData = jsonReader.getObjectFromFile("json/service/mapper/psu-data.json", CmsPsuData.class);
        PsuIdData expected = jsonReader.getObjectFromFile("json/service/mapper/psu-id-data.json", PsuIdData.class);

        PsuIdData psuIdData = psuDataMapper.mapToPsuIdData(psuData);

        assertEquals(expected, psuIdData);
    }

    @Test
    void mapToPsuData_WithoutAdditionalPsuIdData() {
        PsuIdData psuIdData = jsonReader.getObjectFromFile("json/service/mapper/psu-id-data-with-additional-psu-id-data.json", PsuIdData.class);
        CmsPsuData expected = jsonReader.getObjectFromFile("json/service/mapper/psu-data-with-additional-psu-data.json", CmsPsuData.class);

        CmsPsuData psuData = psuDataMapper.mapToPsuData(psuIdData, DEFAULT_SERVICE_INSTANCE_ID);

        assertEquals(expected, psuData);
    }

    @Test
    void mapToPsuIdData_WithoutAdditionalPsuData() {
        CmsPsuData psuData = jsonReader.getObjectFromFile("json/service/mapper/psu-data-with-additional-psu-data.json", CmsPsuData.class);
        PsuIdData expected = jsonReader.getObjectFromFile("json/service/mapper/psu-id-data-with-additional-psu-id-data.json", PsuIdData.class);

        PsuIdData psuIdData = psuDataMapper.mapToPsuIdData(psuData);

        assertEquals(expected, psuIdData);
    }
}
