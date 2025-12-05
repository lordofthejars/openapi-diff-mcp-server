package com.lordofthejars.openapidiff.mcp;


import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
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

public class OpenApiDiffMcpServer {

    @Inject
    Logger logger;

    @Tool(description = "find differences between two OpenAPI specifications and reports on differences ")
    public ToolResponse verifyCompatibility(
            @ToolArg(description = "The old specification content") String oldSpec,
            @ToolArg(description = "The new specification content") String newSpec,
            @ToolArg(description = "The format of the explanation of diff output", defaultValue = "md") String format
    ) {

        try {
            final ChangedOpenApi diff = OpenApiCompare.fromContents(oldSpec, newSpec);

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
        } catch (Exception e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    private String writeToString(Render render, ChangedOpenApi diff) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(baos);
        render.render(diff, osw);
        osw.flush();
        return baos.toString();
    }

}
