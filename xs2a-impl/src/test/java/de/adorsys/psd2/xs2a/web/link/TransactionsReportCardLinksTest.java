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

package de.adorsys.psd2.xs2a.web.link;

import de.adorsys.psd2.xs2a.domain.Xs2aHrefType;
import de.adorsys.psd2.xs2a.domain.Links;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionsReportCardLinksTest {

    private static final String HTTP_URL = "http://url";
    private static final String ACCOUNT_ID = "33333-999999999";
    private Links expectedLinks;

    @BeforeEach
    void setUp() {
        expectedLinks = new Links();
    }

    @Test
    void success_noBalance() {
        boolean withOutBalance = false;

        TransactionsReportCardLinks links = new TransactionsReportCardLinks(HTTP_URL, ACCOUNT_ID, withOutBalance);

        expectedLinks.setAccount(new Xs2aHrefType("http://url/v1/card-accounts/33333-999999999"));
        assertEquals(expectedLinks, links);
    }

    @Test
    void success_with_balance() {
        boolean withBalance = true;

        TransactionsReportCardLinks links = new TransactionsReportCardLinks(HTTP_URL, ACCOUNT_ID, withBalance);

        expectedLinks.setAccount(new Xs2aHrefType("http://url/v1/card-accounts/33333-999999999"));
        expectedLinks.setBalances(new Xs2aHrefType("http://url/v1/card-accounts/33333-999999999/balances"));
        assertEquals(expectedLinks, links);
    }
}
