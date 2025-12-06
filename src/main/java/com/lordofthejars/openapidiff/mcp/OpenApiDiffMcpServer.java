package com.lordofthejars.openapidiff.mcp;


import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkiverse.mcp.server.WrapBusinessError;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.output.AsciidocRender;
import org.openapitools.openapidiff.core.output.ConsoleRender;
import org.openapitools.openapidiff.core.output.JsonRender;
import org.openapitools.openapidiff.core.output.MarkdownRender;
import org.openapitools.openapidiff.core.output.Render;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenApiDiffMcpServer {

    @Inject
    Logger logger;


    @Tool(description = "find differences between two OpenAPI specifications and reports on differences.")
    @WrapBusinessError
    public ToolResponse verifyCompatibility(
            @ToolArg(description = "The old specification content or the URL location of the spec.") String oldSpec,
            @ToolArg(description = "The new specification content or the URL location of the spec.") String newSpec,
            @ToolArg(description = "The format of the explanation of diff output", defaultValue = "md") String format
    ) throws IOException {

        String oldContent = isUri(oldSpec) ? downloadToString(oldSpec) : oldSpec;
        String newContent = isUri(newSpec) ? downloadToString(newSpec) : newSpec;

        final ChangedOpenApi diff = OpenApiCompare.fromContents(oldContent, newContent);

        String explanation = switch (format) {
            case "md" -> writeToString(new MarkdownRender(), diff);
            case "asciidoc" -> writeToString(new AsciidocRender(), diff);
            case "text" -> writeToString(new ConsoleRender(), diff);
            case "json" -> writeToString(new JsonRender(), diff);
            default -> {
                logger.warnf("Output format %s is not supported and we'll switch to Markdown.", format);
                yield writeToString(new MarkdownRender(), diff);
            }
        };

        DiffResponse diffResponse = new DiffResponse(diff.isDifferent(), diff.isIncompatible(), explanation);
        return ToolResponse.structuredSuccess(diffResponse);
    }

    private String writeToString(Render render, ChangedOpenApi diff) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(baos);
        render.render(diff, osw);
        return baos.toString();
    }

    private boolean isUri(String param) {
        try {
            URI uri = new URI(param);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private String downloadToString(String url) throws IOException {
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Failed to fetch URL. HTTP status: " + response.statusCode());
            }

            return response.body();

        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
}
