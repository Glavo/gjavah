package org.glavo.javah;


import org.glavo.javah.util.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RegexTests {

    @Test
    void testSimpleNamePattern() {
        String[] names = {
                "A", "a", "ABC", "AbC", "类名称", "_(*)", "a b c $ d ,", "<a"
        };

        String[] wrongNames = {
                "", "A.B", "[A", "A;B", "A/B"
        };

        for (String name : names) {
            assertTrue(Utils.SIMPLE_NAME_PATTERN.matcher(name).matches()
                    , String.format("'%s' match simple name pattern failed", name));
        }

        for (String name : wrongNames) {
            assertFalse(Utils.SIMPLE_NAME_PATTERN.matcher(name).matches(),
                    String.format("'%s' match simple name pattern failed", name));
        }
    }

    @Test
    void testFullNamePattern() {
        String[] names = {
                "A", "a", "ABC", "AbC", "类名称", "_(*)", "a b c $ d ,",
                "A.B.C", "A.bcd.E", "包1.包2.类名称", "_().B"
        };

        String[] wrongNames = {
                "", "A..B", "A.", ".A", "[A", "A;B", "A/B"
        };

        for (String name : names) {
            assertTrue(Utils.FULL_NAME_PATTERN.matcher(name).matches(),
                    String.format("'%s' match full name pattern failed", name));
        }

        for (String name : wrongNames) {
            assertFalse(Utils.FULL_NAME_PATTERN.matcher(name).matches(),
                    String.format("'%s' match full name pattern failed", name));
        }
    }

    @Test
    void testMethodNamePattern() {
        String[] names = {
                "A", "a", "ABC", "AbC", "类名称", "_(*)", "a b c $ d ,", "<init>", "<cinit>"
        };

        String[] wrongNames = {
                "", "A.B", "[A", "A;B", "A/B", "<", "b<a"
        };

        for (String name : names) {
            assertTrue(Utils.METHOD_NAME_PATTERN.matcher(name).matches()
                    , String.format("'%s' match simple name pattern failed", name));
        }

        for (String name : wrongNames) {
            assertFalse(Utils.METHOD_NAME_PATTERN.matcher(name).matches(),
                    String.format("'%s' match simple name pattern failed", name));
        }
    }
}
