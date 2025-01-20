/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.net.http.HttpClient.Version;
import java.time.Duration;
import org.testng.annotations.Test;

/*
 * @test
 * @summary Tests connection timeouts during SSL handshake
 * @bug 8208391
 * @library /test/lib
 * @build AbstractConnectTimeoutHandshake
 * @run testng/othervm ConnectTimeoutHandshakeSync
 */

public class ConnectTimeoutHandshakeSync
    extends AbstractConnectTimeoutHandshake
{
    @Test(dataProvider = "variants")
    @Override
    public void timeoutSync(Version requestVersion,
                            String method,
                            Duration connectTimeout,
                            Duration requestTimeout)
        throws Exception
    {
        super.timeoutSync(requestVersion, method, connectTimeout, requestTimeout);
    }
}
