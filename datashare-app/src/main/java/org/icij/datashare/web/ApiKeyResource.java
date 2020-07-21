package org.icij.datashare.web;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.codestory.http.Context;
import net.codestory.http.annotations.*;
import net.codestory.http.payload.Payload;
import org.icij.datashare.tasks.TaskFactory;
import org.icij.datashare.user.User;

import java.util.HashMap;

import static net.codestory.http.payload.Payload.ok;

@Singleton
@Prefix("/api/key")
public class ApiKeyResource {
    private final TaskFactory taskFactory;

    @Inject
    public ApiKeyResource(TaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    /**
     * Preflight for key creation.
     *
     * @return 200 with PUT
     */
    @Options("/:userId")
    public Payload createKey(String userId) {
        return ok().withAllowMethods("OPTIONS", "PUT");
    }

    /**
     * Creates a new private key and saves its 384 hash into database for current user.
     *
     * "/api/key" resource is available only in SERVER mode.
     *
     * Returns JSON like :
     * ```
     * {"apiKey":"SrcasvUmaAD6NsZ3+VmUkFFWVfRggIRNmWR5aHx7Kfc="}
     * ```
     *
     * @param userId
     * @return 201 (created) or error
     *
     * @throws Exception
     */
    @Put("/:userId")
    public Payload createKey(String userId, Context context) throws Exception {
        return new Payload("application/json", new HashMap<String, String>() {{
            put("apiKey", taskFactory.createGenApiKey(new User(userId)).call());
        }},201);
    }

    /**
     * Get the private key for an existing user.
     *
     * "/api/key" resource is available only in SERVER mode.
     *
     * @param userId
     * @return 200 or error
     *
     * @throws Exception
     */
    @Get("/:userId")
    public Payload getKey(String userId) throws Exception{
        return new Payload("application/json", new HashMap<String, String>() {{
            put("hashedKey", taskFactory.createGetApiKey(new User(userId)).call());
        }},200);
    }

    /**
     * Preflight for delete key.
     *
     * @return 200 DELETE
     */
    @Options("/:userId")
    public Payload deleteKey(String userId) {
        return ok().withAllowMethods("OPTIONS", "DELETE");
    }

    /**
     * Deletes an apikey for current user.
     *
     * "/api/key" resource is available only in SERVER mode.
     *
     * @param userId
     * @return 204 or 404 (if no key found)
     * @throws Exception
     */
    @Delete("/:userId")
    public Payload deleteKey(String userId,Context context) throws Exception {
        return taskFactory.createDelApiKey(new User(userId)).call() ? new Payload(204) : new Payload(404);
    }
}
