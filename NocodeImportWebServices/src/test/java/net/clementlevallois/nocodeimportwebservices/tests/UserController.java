/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.nocodeimportwebservices.tests;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

/**
 *
 * @author LEVALLOIS
 */
public class UserController {

    public static void create(Context ctx) {
        String username = ctx.queryParam("username");
        if (username == null || username.length() < 5) {
            throw new BadRequestResponse();
        } else {
            ctx.status(201);
        }
    }
}
