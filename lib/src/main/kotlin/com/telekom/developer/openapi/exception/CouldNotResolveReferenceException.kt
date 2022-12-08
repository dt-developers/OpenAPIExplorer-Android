package com.telekom.developer.openapi.exception

class CouldNotResolveReferenceException(val reference: String) :
    Throwable(message = "Reference '$reference' not found")
