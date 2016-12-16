package org.apache.lucene.util;

/**
 * Created by me on 12/15/16.
 */
public final class WeakUnsupportedOperationException extends UnsupportedOperationException {

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }

}
