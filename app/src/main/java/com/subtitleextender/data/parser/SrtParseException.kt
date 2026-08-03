package com.subtitleextender.data.parser

/**
 * Thrown when the content being parsed does not conform to the SRT subtitle
 * format. Callers should present a generic, user-friendly "invalid file"
 * message rather than the raw [message], which is intended for logs and
 * debugging rather than for display.
 */
class SrtParseException(message: String) : Exception(message)
