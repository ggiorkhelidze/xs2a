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

package de.adorsys.psd2.consent.domain.account;

import de.adorsys.psd2.consent.domain.consent.ConsentEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Entity(name = "consent_usage")
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"consent_id", "request_uri", "usage_date"})
})
@NoArgsConstructor
public class AisConsentUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "consent_usage_generator")
    @SequenceGenerator(name = "consent_usage_generator", sequenceName = "consent_usage_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "consent_id", nullable = false)
    private ConsentEntity consent;

    @Column(name = "request_uri", nullable = false)
    private String requestUri;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "usage_amount", nullable = false)
    private int usage;

    @Version
    @Column(name = "version")
    private long version;

    public AisConsentUsage(ConsentEntity consent, String requestUri) {
        this.usageDate = LocalDate.now();
        this.consent = consent;
        this.requestUri = requestUri;
    }

    @Override
    public String toString() {
        return "AisConsentUsage{" +
                   "id=" + id +
                   ", consentId=" + consent.getId() +
                   ", requestUri='" + requestUri + '\'' +
                   ", resourceId='" + resourceId + '\'' +
                   ", transactionId='" + transactionId + '\'' +
                   ", usageDate=" + usageDate +
                   ", usage=" + usage +
                   ", version=" + version +
                   '}';
    }
}
