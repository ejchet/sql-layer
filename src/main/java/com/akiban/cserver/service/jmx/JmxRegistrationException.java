/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.cserver.service.jmx;

public final class JmxRegistrationException extends RuntimeException {
    public JmxRegistrationException(String message) {
        super(message);
    }

    public JmxRegistrationException(Class<?> registeringClass, String name) {
        super(String.format("Illegal name from %s: %s", registeringClass, name));
    }

    public JmxRegistrationException(String message, Exception cause) {
        super(message, cause);
    }

    public JmxRegistrationException(Exception cause) {
        super(cause);
    }
}
