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

package de.adorsys.psd2.xs2a.core.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public enum AccountReferenceType {
    IBAN(1, "iban", Xs2aAccountReference::getIban, Xs2aAccountReference::setIban),
    BBAN(2, "bban", Xs2aAccountReference::getBban, Xs2aAccountReference::setBban),
    PAN(3, "pan", Xs2aAccountReference::getPan, Xs2aAccountReference::setPan),
    MSISDN(4, "msisdn", Xs2aAccountReference::getMsisdn, Xs2aAccountReference::setMsisdn),
    MASKED_PAN(5, "maskedPan", Xs2aAccountReference::getMaskedPan, Xs2aAccountReference::setMaskedPan);

    private String value;
    private int order;
    private Function<Xs2aAccountReference, String> getter;
    private BiConsumer<Xs2aAccountReference, String> setter;

    @JsonCreator
    AccountReferenceType(int order, String value, Function<Xs2aAccountReference, String> getter,
                         BiConsumer<Xs2aAccountReference, String> setter) {
        this.order = order;
        this.value = value;
        this.getter = getter;
        this.setter = setter;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public int getOrder() {
        return order;
    }

    public static Optional<AccountReferenceType> getByValue(String name) {
        return Arrays.stream(values())
                   .filter(type -> type.getValue().equals(name))
                   .findFirst();
    }

    public String getFieldValue(Xs2aAccountReference accountReference) {
        return getter.apply(accountReference);
    }

    public void setFieldValue(Xs2aAccountReference accountReference, String fieldValue) {
        setter.accept(accountReference, fieldValue);
    }
}

