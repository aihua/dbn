package com.dci.intellij.dbn.error;

import com.dci.intellij.dbn.common.util.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

class JiraTicketResponse implements TicketResponse{
    private static final GsonBuilder GSON_BUILDER = new GsonBuilder();

    private JsonObject response;
    private String errorMessage;


    JiraTicketResponse(@Nullable String responseString, @Nullable String errorMessage) {
        if (Strings.isNotEmpty(responseString)) {
            Gson gson = GSON_BUILDER.create();
            this.response = gson.fromJson(responseString, JsonObject.class);
        }
        this.errorMessage = errorMessage;
    }

    @Override
    @Nullable
    public String getTicketId() {
        if (response == null) {
            return null;
        }
        JsonElement key = response.get("key");
        return key == null ? null : key.getAsString();
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}
