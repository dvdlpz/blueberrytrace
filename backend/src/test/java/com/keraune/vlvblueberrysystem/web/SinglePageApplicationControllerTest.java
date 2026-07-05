package com.keraune.vlvblueberrysystem.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SinglePageApplicationControllerTest {
    @Test
    void forwardsBrowserRoutesToTheBundledFrontend() {
        SinglePageApplicationController controller = new SinglePageApplicationController();

        assertEquals("forward:/index.html", controller.forwardToFrontend());
    }
}
