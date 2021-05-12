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


package de.adorsys.psd2.xs2a.service;

import de.adorsys.psd2.aspsp.profile.service.AspspProfileService;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.sca.AuthorisationScaApproachResponse;
import de.adorsys.psd2.xs2a.service.authorization.Xs2aAuthorisationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static de.adorsys.psd2.xs2a.core.profile.ScaApproach.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScaApproachResolverTest {
    private static final String AUTHORISATION_ID = "463318a0-1e33-45d8-8209-e16444b18dda";
    private static final String INSTANCE_ID = "bank1";

    @InjectMocks
    private ScaApproachResolver scaApproachResolver;

    @Mock
    private AspspProfileService aspspProfileService;
    @Mock
    private RequestProviderService requestProviderService;
    @Mock
    private Xs2aAuthorisationService xs2aAuthorisationService;

    @Test
    void resolveScaApproach_shouldReturn_Redirect() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID))
            .thenReturn(buildScaApproaches(EMBEDDED, REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred())
            .thenReturn(Optional.of(true));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_shouldReturn_Embedded() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID))
            .thenReturn(buildScaApproaches(EMBEDDED, REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred())
            .thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(EMBEDDED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredAbsent_Redirect_shouldReturn_Redirect() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.empty());

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredTrue_Redirect_shouldReturn_Redirect() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredFalse_Redirect_shouldReturn_Redirect() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredAbsent_Embedded_shouldReturn_Embedded() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.empty());

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(EMBEDDED);
    }

    @Test
    void resolveScaApproach_noHeaders() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(REDIRECT, DECOUPLED, EMBEDDED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.empty());
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.empty());

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_bothTrue() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED, DECOUPLED, REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.of(true));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(DECOUPLED);
    }

    @Test
    void resolveScaApproach_bothTrueDecoupledFirst() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(DECOUPLED, REDIRECT, EMBEDDED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.of(true));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(DECOUPLED);
    }

    @Test
    void resolveScaApproach_bothTrueRedirectFirst() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(REDIRECT, DECOUPLED, EMBEDDED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.of(true));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_RedirectTrue() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED, DECOUPLED, REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.empty());

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_RedirectTrueDecoupledFalse() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED, DECOUPLED, REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_DecoupledTrue() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED, DECOUPLED, REDIRECT));
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.of(true));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.empty());

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(DECOUPLED);
    }

    @Test
    void resolveScaApproach_RedirectFalseDecoupledTrue() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED, DECOUPLED, REDIRECT));
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.of(true));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(DECOUPLED);
    }

    @Test
    void resolveScaApproach_RedirectFalse() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED, DECOUPLED, REDIRECT));
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.empty());
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(EMBEDDED);
    }

    @Test
    void resolveScaApproach_DecoupledFalse() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED, DECOUPLED, REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.empty());
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(EMBEDDED);
    }

    @Test
    void resolveScaApproach_RedirectFalseDecoupled() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(DECOUPLED, EMBEDDED, REDIRECT));
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.empty());
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(DECOUPLED);
    }

    @Test
    void resolveScaApproach_DecoupledFalseRedirect() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(REDIRECT, DECOUPLED, EMBEDDED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.empty());
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_RedirectFalseAndFirst() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(REDIRECT, EMBEDDED, DECOUPLED));
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.empty());
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(EMBEDDED);
    }

    @Test
    void resolveScaApproach_DecoupledFalseAndFirst() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(DECOUPLED, REDIRECT, EMBEDDED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.empty());
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_bothFalse() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(DECOUPLED, REDIRECT, EMBEDDED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(false));
        when(requestProviderService.resolveTppDecoupledPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(EMBEDDED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredTrue_Embedded_shouldReturn_Embedded() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(EMBEDDED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredFalse_Embedded_shouldReturn_Embedded() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(EMBEDDED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredTrue_Decoupled_shouldReturn_Decoupled() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(DECOUPLED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(DECOUPLED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredFalse_Decoupled_shouldReturn_Decoupled() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(DECOUPLED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(DECOUPLED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredTrue_EmbeddedDecoupledRedirect_shouldReturn_Redirect() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED, DECOUPLED, REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredTrue_RedirectEmbeddedDecoupled_shouldReturn_Redirect() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(REDIRECT, EMBEDDED, DECOUPLED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredTrue_DecoupledEmbeddedRedirect_shouldReturn_Redirect() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(DECOUPLED, EMBEDDED, REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredTrue_EmbeddedDecoupled_shouldReturn_Embedded() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED, DECOUPLED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(EMBEDDED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredTrue_DecoupledEmbedded_shouldReturn_Decoupled() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(DECOUPLED, EMBEDDED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(true));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(DECOUPLED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredAbsent_RedirectEmbeddedDecoupled_shouldReturn_Redirect() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(REDIRECT, EMBEDDED, DECOUPLED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.empty());

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(REDIRECT);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredAbsent_EmbeddedDecoupledRedirect_shouldReturn_Embedded() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED, DECOUPLED, REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.empty());

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(EMBEDDED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredAbsent_DecoupledEmbeddedRedirect_shouldReturn_Decoupled() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(DECOUPLED, EMBEDDED, REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.empty());

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(DECOUPLED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredFalse_DecoupledEmbeddedRedirect_shouldReturn_Decoupled() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(DECOUPLED, EMBEDDED, REDIRECT));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(DECOUPLED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredFalse_EmbeddedRedirectDecoupled_shouldReturn_Embedded() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(EMBEDDED, REDIRECT, DECOUPLED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(EMBEDDED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredFalse_RedirectEmbeddedDecoupled_shouldReturn_Embedded() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(REDIRECT, EMBEDDED, DECOUPLED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(EMBEDDED);
    }

    @Test
    void resolveScaApproach_TppRedirectPreferredFalse_RedirectDecoupledEmbedded_shouldReturn_Decoupled() {
        //Given
        when(requestProviderService.getInstanceId()).thenReturn(INSTANCE_ID);
        when(aspspProfileService.getScaApproaches(INSTANCE_ID)).thenReturn(buildScaApproaches(REDIRECT, DECOUPLED, EMBEDDED));
        when(requestProviderService.resolveTppRedirectPreferred()).thenReturn(Optional.of(false));

        //When
        ScaApproach actualResult = scaApproachResolver.resolveScaApproach();

        //Then
        assertThat(actualResult).isEqualTo(DECOUPLED);
    }

    @Test
    void resolveScaApproach_scaApproachResponseIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> scaApproachResolver.getScaApproach(AUTHORISATION_ID));
    }

    @Test
    void getScaApproach() {
        when(xs2aAuthorisationService.getAuthorisationScaApproach(AUTHORISATION_ID))
            .thenReturn(Optional.of(new AuthorisationScaApproachResponse(REDIRECT)));

        ScaApproach scaApproach = scaApproachResolver.getScaApproach(AUTHORISATION_ID);

        assertThat(scaApproach).isEqualTo(REDIRECT);
        verify(xs2aAuthorisationService, times(1))
            .getAuthorisationScaApproach(AUTHORISATION_ID);
    }

    private List<ScaApproach> buildScaApproaches(ScaApproach... scaApproaches) {
        return Arrays.asList(scaApproaches);
    }
}
