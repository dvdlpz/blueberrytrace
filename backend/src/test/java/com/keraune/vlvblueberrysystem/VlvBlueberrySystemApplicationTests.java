package com.keraune.vlvblueberrysystem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class VlvBlueberrySystemApplicationTests {
    @Test
    void mainClassIsLoadable() {
        assertDoesNotThrow(() -> Class.forName("com.keraune.vlvblueberrysystem.VlvBlueberrySystemApplication"));
    }
}
