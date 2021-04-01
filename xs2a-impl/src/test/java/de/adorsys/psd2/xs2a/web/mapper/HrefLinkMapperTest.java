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

package de.adorsys.psd2.xs2a.web.mapper;

import de.adorsys.psd2.mapper.Xs2aObjectMapper;
import de.adorsys.psd2.xs2a.domain.Xs2aHrefType;
import de.adorsys.psd2.xs2a.domain.Links;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HrefLinkMapperTest {
    private static final String LINK_NAME = "scaStatus";
    private static final String LINK_PATH = "http://localhost/v1/payments/";

    private HrefLinkMapper hrefMapper;

    @BeforeEach
    void setUp() {
        Xs2aObjectMapper xs2aObjectMapper = new Xs2aObjectMapper();
        hrefMapper = new HrefLinkMapper(xs2aObjectMapper);
    }

    @Test
    void mapToLinksMap_withValidMap_shouldReturnLinks() {
        // Given
        Links links = new Links();
        links.setScaStatus(new Xs2aHrefType(LINK_PATH));

        //When:
        Map<String, Xs2aHrefType> linkMap = hrefMapper.mapToLinksMap(links);

        //Then:
        assertNotNull(linkMap);

        Xs2aHrefType wrappedLink = linkMap.get(LINK_NAME);

        assertNotNull(wrappedLink);
        assertEquals(LINK_PATH, wrappedLink.getHref());
    }

    @Test
    void mapToLinksMap_withNullMap_shouldReturnNull() {
        //When:
        Map<String, Xs2aHrefType> linkMap = hrefMapper.mapToLinksMap(null);

        //Then:
        assertNull(linkMap);
    }
}
