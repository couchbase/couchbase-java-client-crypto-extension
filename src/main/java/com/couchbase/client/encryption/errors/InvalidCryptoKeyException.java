/*
 * Copyright (c) 2020 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement v7
 * which may be found at https://www.couchbase.com/ESLA01162020.
 */

package com.couchbase.client.encryption.errors;

import com.couchbase.client.core.error.context.ErrorContext;

/**
 * Thrown when the supplied key does not match a crypto providers expectations
 * (for example, if the key is the wrong size).
 */
public class InvalidCryptoKeyException extends CryptoException {
  public InvalidCryptoKeyException(String message) {
    super(message);
  }

  public InvalidCryptoKeyException(String message, ErrorContext ctx) {
    super(message, ctx);
  }

  public InvalidCryptoKeyException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidCryptoKeyException(String message, Throwable cause, ErrorContext ctx) {
    super(message, cause, ctx);
  }
}
