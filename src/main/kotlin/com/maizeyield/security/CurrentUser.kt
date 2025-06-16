package com.maizeyield.security

import org.springframework.security.core.annotation.AuthenticationPrincipal

/**
 * Custom annotation to inject the current authenticated user
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@AuthenticationPrincipal
annotation class CurrentUser