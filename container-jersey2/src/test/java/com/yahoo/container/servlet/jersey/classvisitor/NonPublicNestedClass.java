// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.container.servlet.jersey.classvisitor;

import javax.ws.rs.Path;

/**
 * @author tonytv
 */
public class NonPublicNestedClass {
    @Path("ignored")
    static class Nested {}
}
