package com.maizeyield.exception

/**
 * Exception thrown when a requested resource is not found
 */
class ResourceNotFoundException(message: String) : RuntimeException(message)

/**
 * Exception thrown when input validation fails
 */
class ValidationException(
    message: String,
    val errors: Map<String, String> = emptyMap()
) : RuntimeException(message)

/**
 * Exception thrown when an operation fails due to data integrity issues
 */
class DataIntegrityException(message: String) : RuntimeException(message)

/**
 * Exception thrown when a user attempts an operation they're not authorized for
 */
class UnauthorizedAccessException(message: String) : RuntimeException(message)

/**
 * Exception thrown when a machine learning prediction operation fails
 */
class PredictionException(message: String, val details: String? = null) : RuntimeException(message)

/**
 * Exception thrown when an external service integration fails
 */
class ExternalServiceException(message: String, val serviceName: String) : RuntimeException(message)