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

package de.adorsys.psd2.core.data.ais;

import de.adorsys.psd2.core.data.Xs2aConsentAccountAccess;
import de.adorsys.psd2.xs2a.core.consent.AisConsentRequestType;
import de.adorsys.psd2.xs2a.core.consent.ConsentType;
import de.adorsys.psd2.xs2a.core.profile.Xs2aAccountReference;
import de.adorsys.psd2.xs2a.core.profile.AccountReferenceType;
import de.adorsys.xs2a.reader.JsonReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AisConsentDataTest {
    private static final JsonReader jsonReader = new JsonReader();

    @Test
    void getConsentType() {
        assertEquals(ConsentType.AIS, new AisConsent().getConsentType());
    }

    @Test
    void getConsentRequestType_bankOffered() {
        AisConsent aisConsent = jsonReader.getObjectFromFile("json/data/ais/ais-consent-bank-offered.json", AisConsent.class);

        assertEquals(AisConsentRequestType.BANK_OFFERED, aisConsent.getConsentRequestType());
    }

    @Test
    void getConsentRequestType_bankOfferedEmptyArray() {
        AisConsent aisConsent = jsonReader.getObjectFromFile("json/data/ais/ais-consent-bank-offered-empty-array.json", AisConsent.class);

        assertEquals(AisConsentRequestType.BANK_OFFERED, aisConsent.getConsentRequestType());
    }

    @ParameterizedTest
    @MethodSource("consents")
    void getConsentRequestType(AisConsent aisConsent) {
        assertEquals(AisConsentRequestType.ALL_AVAILABLE_ACCOUNTS, aisConsent.getConsentRequestType());
    }

    private static Stream<Arguments> consents() {
        AisConsent aisConsent = jsonReader.getObjectFromFile("json/data/ais/ais-consent-all-available.json",
                                                             AisConsent.class);
        AisConsent aisConsentWithOwnerName = jsonReader.getObjectFromFile("json/data/ais/ais-consent-all-available-owner-name.json",
                                                                          AisConsent.class);
        AisConsent aisConsentWithBalance = jsonReader.getObjectFromFile("json/data/ais/ais-consent-all-available-with-balance.json",
                                                                        AisConsent.class);
        AisConsent aisConsentWithBalanceAndOwnerName = jsonReader.getObjectFromFile("json/data/ais/ais-consent-all-available-with-balance-owner-name.json",
                                                                                    AisConsent.class);

        return Stream.of(
            Arguments.arguments(aisConsent),
            Arguments.arguments(aisConsentWithOwnerName),
            Arguments.arguments(aisConsentWithBalance),
            Arguments.arguments(aisConsentWithBalanceAndOwnerName)
        );
    }

    @Test
    void getConsentRequestType_tpp_dedicated() {
        AisConsent aisConsent = jsonReader.getObjectFromFile("json/data/ais/ais-consent-tpp-dedicated.json",
                                                             AisConsent.class);

        assertEquals(AisConsentRequestType.DEDICATED_ACCOUNTS, aisConsent.getConsentRequestType());
    }

    @Test
    void getConsentRequestType_tpp_global() {
        AisConsent aisConsent = jsonReader.getObjectFromFile("json/data/ais/ais-consent-global.json",
                                                             AisConsent.class);

        assertEquals(AisConsentRequestType.GLOBAL, aisConsent.getConsentRequestType());
        assertTrue(aisConsent.isWithBalance());
    }

    @Test
    void getConsentRequestType_tpp_globalWithOwnerName() {
        AisConsent aisConsent = jsonReader.getObjectFromFile("json/data/ais/ais-consent-global-owner-name.json",
                                                             AisConsent.class);

        assertEquals(AisConsentRequestType.GLOBAL, aisConsent.getConsentRequestType());
    }

    @Test
    void getConsentRequestType_aspsp_dedicated() {
        AisConsent aisConsent = jsonReader.getObjectFromFile("json/data/ais/ais-consent-aspsp-dedicated.json",
                                                             AisConsent.class);

        assertEquals(AisConsentRequestType.DEDICATED_ACCOUNTS, aisConsent.getConsentRequestType());
    }

    @Test
    void getUsedAccess_emptyAccesses() {
        AisConsent bankOfferedConsent = jsonReader.getObjectFromFile("json/data/ais/ais-consent-bank-offered.json", AisConsent.class);
        Xs2aConsentAccountAccess emptyAccess = new Xs2aConsentAccountAccess(null, null, null, null);

        assertEquals(emptyAccess, bankOfferedConsent.getAccess());
    }

    @Test
    void getUsedAccess_tppAccess() {
        AisConsent consentWithTppAccess = jsonReader.getObjectFromFile("json/data/ais/ais-consent-tpp-dedicated.json", AisConsent.class);
        List<Xs2aAccountReference> accountReferences = Collections.singletonList(new Xs2aAccountReference(AccountReferenceType.IBAN, "DE98500105171757213183", null));
        Xs2aConsentAccountAccess dedicatedAccess = new Xs2aConsentAccountAccess(accountReferences, accountReferences, accountReferences, null);

        assertEquals(dedicatedAccess, consentWithTppAccess.getAccess());
    }

    @Test
    void getUsedAccess_aspspAccess() {
        AisConsent consentWithAspspAccess = jsonReader.getObjectFromFile("json/data/ais/ais-consent-aspsp-dedicated.json", AisConsent.class);
        List<Xs2aAccountReference> accountReferences = Collections.singletonList(new Xs2aAccountReference(AccountReferenceType.IBAN, "DE98500105171757213183", null));
        Xs2aConsentAccountAccess dedicatedAccess = new Xs2aConsentAccountAccess(accountReferences, accountReferences, accountReferences, null);

        assertEquals(dedicatedAccess, consentWithAspspAccess.getAccess());
    }

    @Test
    void getUsedAccess_globalConsentWithAspspReferences_shouldReturnTppAccess() {
        AisConsent globalConsentDataWithAccountReferences = jsonReader.getObjectFromFile("json/data/ais/ais-consent-global-aspsp-accounts.json", AisConsent.class);
        Xs2aConsentAccountAccess globalAccess = new Xs2aConsentAccountAccess(null, null, null, null);

        assertEquals(globalAccess, globalConsentDataWithAccountReferences.getAccess());
    }
}
