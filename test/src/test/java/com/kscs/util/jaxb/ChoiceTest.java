/*
 * MIT License
 *
 * Copyright (c) 2014 Klemm Software Consulting, Mirko Klemm
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.kscs.util.jaxb;
/*
import com.kscs.jaxb2.contract.test.Bike;
import com.kscs.jaxb2.contract.test.ChoiceOfElementsFive;
import com.kscs.jaxb2.contract.test.ChoiceOfElementsFour;
import com.kscs.jaxb2.contract.test.ChoiceOfElementsTwo;
import com.kscs.jaxb2.contract.test.City;
import com.kscs.jaxb2.contract.test.Transport;
import com.kscs.jaxb2.contract.test.Worker;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ChoiceTest {

    @Test
    public void testChoiceWithBaseComplexType1() {
        ChoiceOfElementsTwo choiceOfElementsTwo = ChoiceOfElementsTwo.builder()
                .withTransport(buildBike())
                .build();

        assertChoiceOfElementsTwo(choiceOfElementsTwo);
    }

    @Test
    public void testChoiceWithBaseComplexType2() {
        ChoiceOfElementsTwo choiceOfElementsTwo = ChoiceOfElementsTwo.builder()
                .withBike()
                .withName("my bike")
                .withFrameSize(58)
                .end()
                .build();

        assertChoiceOfElementsTwo(choiceOfElementsTwo);
    }

    @Test
    public void testChoiceWithBaseComplexType3() {
        ChoiceOfElementsTwo choiceOfElementsTwo = ChoiceOfElementsTwo.builder()
                .withBike(buildBike())
                .build();

        assertChoiceOfElementsTwo(choiceOfElementsTwo);
    }

    @Test
    public void testChoiceWithCommonSuperType1() {
        ChoiceOfElementsFour choiceOfElementsFour = ChoiceOfElementsFour.builder()
                .withObject(buildWorker())
                .build();

        assertChoiceOfElementsFour(choiceOfElementsFour);
    }

    @Test
    public void testChoiceWithCommonSuperType2() {
        ChoiceOfElementsFour choiceOfElementsFour = ChoiceOfElementsFour.builder()
                .withWorker()
                .withCompany("Acme inc.")
                .withName("John Doe")
                .end()
                .build();

        assertChoiceOfElementsFour(choiceOfElementsFour);
    }

    @Test
    public void testChoiceWithCommonSuperType3() {
        ChoiceOfElementsFour choiceOfElementsFour = ChoiceOfElementsFour.builder()
                .withWorker(buildWorker())
                .build();

        assertChoiceOfElementsFour(choiceOfElementsFour);
    }

    @Test
    public void testMultiChoiceWithCommonSuperType1() {
        ChoiceOfElementsFive choiceOfElementsFive = ChoiceOfElementsFive.builder()
                .addObject(buildWorker())
                .addCity(City.builder()
                        .withTown("Springfield")
                        .build())
                .addWorker(Worker.builder()
                        .withName("Jane Doe")
                        .withPhoneNumber(1234567)
                        .build())
                .build();

        assertThat(choiceOfElementsFive.getObject())
                .hasSize(3);
        assertThat(choiceOfElementsFive.getObject().get(0)).isInstanceOf(Worker.class);
        assertThat(choiceOfElementsFive.getObject().get(1)).isInstanceOf(City.class);
        assertThat(choiceOfElementsFive.getObject().get(2)).isInstanceOf(Worker.class);
        assertThat(((Worker)choiceOfElementsFive.getObject().get(2)).getName()).startsWith("Jane");
        assertThat(((Worker)choiceOfElementsFive.getObject().get(0)).getName()).startsWith("John");
    }

    private Worker buildWorker() {
        return Worker.builder()
        .withCompany("Acme inc.")
        .withName("John Doe")
        .build();
    }

    private void assertChoiceOfElementsFour(final ChoiceOfElementsFour choiceOfElementsFour) {
        assertThat(choiceOfElementsFour.getObject()).isNotNull();
        Object object = choiceOfElementsFour.getObject();
        assertThat(object).isInstanceOf(Worker.class);
        assertThat(((Worker) object).getCompany()).isEqualTo("Acme inc.");
        assertThat(((Worker) object).getName()).isEqualTo("John Doe");
    }

    private void assertChoiceOfElementsTwo(final ChoiceOfElementsTwo choiceOfElementsTwo) {
        assertThat(choiceOfElementsTwo.getTransport()).isNotNull();
        Transport transport = choiceOfElementsTwo.getTransport();
        assertThat(transport.getName()).isEqualTo("my bike");
        assertThat(transport).isInstanceOf(Bike.class);
        assertThat(((Bike) transport).getFrameSize()).isEqualTo(58);
    }

    private Bike buildBike() {
        return Bike.builder()
                .withName("my bike")
                .withFrameSize(58)
                .build();
    }
}

*/
