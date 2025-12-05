package com.lordofthejars.openapidiff.mcp;





public record DiffResponse(boolean changed, boolean incompatible, String explanation) {
}
