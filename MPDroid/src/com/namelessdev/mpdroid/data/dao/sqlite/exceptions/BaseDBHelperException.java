
package com.namelessdev.mpdroid.data.dao.sqlite.exceptions;

public class BaseDBHelperException extends RuntimeException {

    private static final long serialVersionUID = 1899138265936016333L;
    private String cause;

    public BaseDBHelperException() {
        cause = "Unknown cause";
    }

    public BaseDBHelperException(String cause) {
        this.cause = cause;
    }

    @Override
    public String getMessage() {
        return cause;
    }
}
