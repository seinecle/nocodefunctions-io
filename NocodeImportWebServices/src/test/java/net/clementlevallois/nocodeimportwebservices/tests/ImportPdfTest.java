/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.nocodeimportwebservices.tests;

import io.javalin.http.Context;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 *
 * @author LEVALLOIS
 */
public class ImportPdfTest {

    private final Context ctx = mock(Context.class);

    @Test
    public void POST_to_create_users_gives_201_for_valid_username() {
        when(ctx.queryParam("username")).thenReturn("Roland");
        UserController.create(ctx); // the handler we're testing
        verify(ctx).status(201);
    }
}
