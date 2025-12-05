package com.lordofthejars.openapidiff.mcp;


import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@QuarkusTest
class OpenApiDiffMcpServerTest {

    @Test
    void testAnswer() throws IOException {

        String oldSpec = Files.readString(Paths.get("src/test/resources", "oldSpec.yaml"));
        String newSpec = Files.readString(Paths.get("src/test/resources", "newSpec.yaml"));

        McpAssured.McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();

        client.when()
                .toolsCall("verifyCompatibility", Map.of(
                    "oldSpec", oldSpec, "newSpec", newSpec
                ), r-> {
                    System.out.println(r);
                })
                .thenAssertResults();
    }

}
