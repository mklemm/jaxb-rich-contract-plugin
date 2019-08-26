package com.kscs.util.jaxb;

import com.kscs.jaxb2.contract.test.Class;
import com.kscs.jaxb2.contract.test.Import;
import com.kscs.jaxb2.contract.test.ReservedWordsMultiChoice;
import com.kscs.jaxb2.contract.test.ReservedWordsSequence;
import com.kscs.jaxb2.contract.test.ReservedWordsSingleChoice;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReservedWordTest {

    @Test
    public void testReservedWordsMultiChoice1() {

        ReservedWordsMultiChoice reservedWordsMultiChoice = ReservedWordsMultiChoice.builder()
                .addImportOrClazz(
                        Import.builder()
                                .withImport("import")
                                .build(),
                        Class.builder()
                                .withClazz("class")
                                .build())
                .build();

        assertThat(reservedWordsMultiChoice.getImportOrClazz())
                .hasSize(2);
        assertThat(reservedWordsMultiChoice.getImportOrClazz().get(0))
                .isInstanceOf(Import.class);
        assertThat(((Import)reservedWordsMultiChoice.getImportOrClazz().get(0)).getImport())
                .isEqualTo("import");
        assertThat(reservedWordsMultiChoice.getImportOrClazz().get(1))
                .isInstanceOf(Class.class);
        assertThat(((Class)reservedWordsMultiChoice.getImportOrClazz().get(1)).getClazz())
                .isEqualTo("class");
    }

    @Test
    public void testReservedWordsMultiChoice2() {

        ReservedWordsMultiChoice reservedWordsMultiChoice = ReservedWordsMultiChoice.builder()
                .addImport()
                .withImport("import")
                .end()
                .addClazz()
                .withClazz("class")
                .end()
                .build();

        assertThat(reservedWordsMultiChoice.getImportOrClazz())
                .hasSize(2);
        assertThat(reservedWordsMultiChoice.getImportOrClazz().get(0))
                .isInstanceOf(Import.class);
        assertThat(((Import)reservedWordsMultiChoice.getImportOrClazz().get(0)).getImport())
                .isEqualTo("import");
        assertThat(reservedWordsMultiChoice.getImportOrClazz().get(1))
                .isInstanceOf(Class.class);
        assertThat(((Class)reservedWordsMultiChoice.getImportOrClazz().get(1)).getClazz())
                .isEqualTo("class");
    }

    @Test
    public void testReservedWordsMultiChoice3() {

        ReservedWordsMultiChoice reservedWordsMultiChoice = ReservedWordsMultiChoice.builder()
                .addImport(Import.builder()
                        .withImport("import").build())
                .addClazz(Class.builder()
                        .withClazz("class").build())
                .build();

        assertThat(reservedWordsMultiChoice.getImportOrClazz())
                .hasSize(2);
        assertThat(reservedWordsMultiChoice.getImportOrClazz().get(0))
                .isInstanceOf(Import.class);
        assertThat(((Import)reservedWordsMultiChoice.getImportOrClazz().get(0)).getImport())
                .isEqualTo("import");
        assertThat(reservedWordsMultiChoice.getImportOrClazz().get(1))
                .isInstanceOf(Class.class);
        assertThat(((Class)reservedWordsMultiChoice.getImportOrClazz().get(1)).getClazz())
                .isEqualTo("class");
    }

    @Test
    public void testReservedWordsSequence1() {

        ReservedWordsSequence reservedWordsSequence = ReservedWordsSequence.builder()
                .withImport()
                .withImport("import")
                .end()
                .withClazz()
                .withClazz("class")
                .end()
                .build();

        assertThat((reservedWordsSequence.getImport()).getImport())
                .isEqualTo("import");
        assertThat((reservedWordsSequence.getClazz()).getClazz())
                .isEqualTo("class");
    }

    @Test
    public void testReservedWordsSequence2() {

        ReservedWordsSequence reservedWordsSequence = ReservedWordsSequence.builder()
                .withImport(Import.builder()
                        .withImport("import")
                        .build())
                .withClazz(Class.builder()
                        .withClazz("class")
                        .build())
                .build();

        assertThat((reservedWordsSequence.getImport()).getImport())
                .isEqualTo("import");
        assertThat((reservedWordsSequence.getClazz()).getClazz())
                .isEqualTo("class");
    }

    @Test
    public void testReservedWordsSingleChoice1() {

        ReservedWordsSingleChoice reservedWordsSingleChoice = ReservedWordsSingleChoice.builder()
                .withImportOrClazz(
                        Import.builder()
                                .withImport("import")
                                .build())
                .build();

        assertThat(reservedWordsSingleChoice.getImportOrClazz())
                .isInstanceOf(Import.class);
        assertThat(((Import)reservedWordsSingleChoice.getImportOrClazz()).getImport())
                .isEqualTo("import");
    }

    @Test
    public void testReservedWordsSingleChoice2() {

        ReservedWordsSingleChoice reservedWordsSingleChoice = ReservedWordsSingleChoice.builder()
                .withImportOrClazz(
                        Import.builder()
                                .withImport("import")
                                .build())
                .withImportOrClazz(
                        Class.builder()
                                .withClazz("class")
                                .build())
                .build();

        assertThat(reservedWordsSingleChoice.getImportOrClazz())
                .isInstanceOf(Class.class);
        assertThat(((Class)reservedWordsSingleChoice.getImportOrClazz()).getClazz())
                .isEqualTo("class");
    }

    @Test
    public void testReservedWordsSingleChoice3() {

        ReservedWordsSingleChoice reservedWordsSingleChoice = ReservedWordsSingleChoice.builder()
                .withImport()
                        .withImport("import")
                        .end()
                .build();

        assertThat(reservedWordsSingleChoice.getImportOrClazz())
                .isInstanceOf(Import.class);
        assertThat(((Import)reservedWordsSingleChoice.getImportOrClazz()).getImport())
                .isEqualTo("import");
    }

    @Test
    public void testReservedWordsSingleChoice4() {

        ReservedWordsSingleChoice reservedWordsSingleChoice = ReservedWordsSingleChoice.builder()
                .withImport()
                    .withImport("import")
                    .end()
                .withClazz()
                    .withClazz("class")
                    .end()
                .build();

        assertThat(reservedWordsSingleChoice.getImportOrClazz())
                .isInstanceOf(Class.class);
        assertThat(((Class)reservedWordsSingleChoice.getImportOrClazz()).getClazz())
                .isEqualTo("class");
    }

    @Test
    public void testReservedWordsSingleChoice5() {

        ReservedWordsSingleChoice reservedWordsSingleChoice = ReservedWordsSingleChoice.builder()
                .withImport(Import.builder()
                        .withImport("import")
                        .build())
                .build();

        assertThat(reservedWordsSingleChoice.getImportOrClazz())
                .isInstanceOf(Import.class);
        assertThat(((Import)reservedWordsSingleChoice.getImportOrClazz()).getImport())
                .isEqualTo("import");
    }

    @Test
    public void testReservedWordsSingleChoice6() {

        ReservedWordsSingleChoice reservedWordsSingleChoice = ReservedWordsSingleChoice.builder()
                .withImport(Import.builder()
                        .withImport("import")
                        .build())
                .withClazz(Class.builder()
                        .withClazz("class")
                        .build())
                .build();

        assertThat(reservedWordsSingleChoice.getImportOrClazz())
                .isInstanceOf(Class.class);
        assertThat(((Class)reservedWordsSingleChoice.getImportOrClazz()).getClazz())
                .isEqualTo("class");
    }

}
