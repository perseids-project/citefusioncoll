package edu.holycross.shot.fusiontables

import static org.junit.Assert.*;
import org.junit.Test;

public class TestProps extends GroovyTestCase {

    @Test public void testPropertyPassing() {
        /* Test that configuration of your installation includes
        a project apiKey property that is passed in to the test
        environment as a system property. */
        assert System.properties['apiKey']
    }
}

