package com.lordofthejars.openapidiff.mcp;


import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class OpenApiDiffMcpServerTest {

    @Test
    void testDiffUrlContent() throws IOException {
        String url = "https://gist.githubusercontent.com/lordofthejars/7177c09ce09ea6ddfc6359232fcb52de/raw/660b3850e03f492bc018464143e9c94f9d126c34/oldSpec.yaml";
        String newSpec = Files.readString(Paths.get("src/test/resources", "newSpec.yaml"));

        McpAssured.McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();
        client.when()
                .toolsCall("verifyCompatibility", Map.of(
                        "oldSpec", url, "newSpec", newSpec
                ), r-> {
                    JsonObject structuredContent = (JsonObject) r.structuredContent();
                    assertThat(structuredContent.getBoolean("changed")).isTrue();
                    assertThat(structuredContent.getBoolean("incompatible")).isFalse();
                    try {
                        Files.writeString(Paths.get("target", "exp.md"), structuredContent.getString("explanation"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenAssertResults();
    }

    @Test
    void testDiffContent() throws IOException {

        String oldSpec = Files.readString(Paths.get("src/test/resources", "oldSpec.yaml"));
        String newSpec = Files.readString(Paths.get("src/test/resources", "newSpec.yaml"));

        McpAssured.McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();

        client.when()
                .toolsCall("verifyCompatibility", Map.of(
                    "oldSpec", oldSpec, "newSpec", newSpec
                ), r-> {
                    JsonObject structuredContent = (JsonObject) r.structuredContent();
                    assertThat(structuredContent.getBoolean("changed")).isTrue();
                    assertThat(structuredContent.getBoolean("incompatible")).isFalse();
                    try {
                        Files.writeString(Paths.get("target", "exp.md"), structuredContent.getString("explanation"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenAssertResults();
    }

}
