/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */
package com.akiban.server.types3.mcompat.mcasts;

import com.akiban.server.error.AkibanInternalException;
import com.akiban.server.types3.TCast;
import com.akiban.server.types3.TCastBase;
import com.akiban.server.types3.TClass;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.common.types.StringAttribute;
import com.akiban.server.types3.common.types.StringFactory;
import com.akiban.server.types3.mcompat.mtypes.MBinary;
import com.akiban.server.types3.mcompat.mtypes.MString;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

public final class Cast_From_Varbinary {

    private Cast_From_Varbinary() {}

    public static final TCast VARBINARY_TO_BLOB = new BinaryToBinary(MBinary.VARBINARY, MBinary.BLOB);
    public static final TCast VARBINARY_TO_LONGBLOB = new BinaryToBinary(MBinary.VARBINARY, MBinary.LONGBLOB);
    public static final TCast VARBINARY_TO_MEDIUMBLOB = new BinaryToBinary(MBinary.VARBINARY, MBinary.MEDIUMBLOB);
    public static final TCast VARBINARY_TO_TINYBLOB = new BinaryToBinary(MBinary.VARBINARY, MBinary.TINYBLOB);

    public static final TCast BLOB_TO_VARBINARY = new BinaryToBinary(MBinary.BLOB, MBinary.VARBINARY);
    public static final TCast BLOB_TO_LONGBLOB = new BinaryToBinary(MBinary.BLOB, MBinary.LONGBLOB);
    public static final TCast BLOB_TO_MEDIUMBLOB = new BinaryToBinary(MBinary.BLOB, MBinary.MEDIUMBLOB);
    public static final TCast BLOB_TO_TINYBLOB = new BinaryToBinary(MBinary.BLOB, MBinary.TINYBLOB);

    public static final TCast LONGBLOB_TO_VARBINARY = new BinaryToBinary(MBinary.LONGBLOB, MBinary.VARBINARY);
    public static final TCast LONGBLOB_TO_BLOB = new BinaryToBinary(MBinary.LONGBLOB, MBinary.BLOB);
    public static final TCast LONGBLOB_TO_MEDIUMBLOB = new BinaryToBinary(MBinary.LONGBLOB, MBinary.MEDIUMBLOB);
    public static final TCast LONGBLOB_TO_TINYBLOB = new BinaryToBinary(MBinary.LONGBLOB, MBinary.TINYBLOB);

    public static final TCast MEDIUMBLOB_TO_VARBINARY = new BinaryToBinary(MBinary.MEDIUMBLOB, MBinary.VARBINARY);
    public static final TCast MEDIUMBLOB_TO_BLOB = new BinaryToBinary(MBinary.MEDIUMBLOB, MBinary.BLOB);
    public static final TCast MEDIUMBLOB_TO_LONGBLOB = new BinaryToBinary(MBinary.MEDIUMBLOB, MBinary.LONGBLOB);
    public static final TCast MEDIUMBLOB_TO_TINYBLOB = new BinaryToBinary(MBinary.MEDIUMBLOB, MBinary.TINYBLOB);

    public static final TCast TINYBLOB_TO_VARBINARY = new BinaryToBinary(MBinary.TINYBLOB, MBinary.VARBINARY);
    public static final TCast TINYBLOB_TO_BLOB = new BinaryToBinary(MBinary.TINYBLOB, MBinary.BLOB);
    public static final TCast TINYBLOB_TO_LONGBLOB = new BinaryToBinary(MBinary.TINYBLOB, MBinary.LONGBLOB);
    public static final TCast TINYBLOB_TO_MEDIUMBLOB = new BinaryToBinary(MBinary.TINYBLOB, MBinary.MEDIUMBLOB);

    private static class BinaryToBinary extends TCastBase {
        private BinaryToBinary(TClass sourceClass, TClass targetClass) {
            super(sourceClass, targetClass);
        }

        @Override
        public void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            MBinary.putBytes(context, target, source.getBytes());
        }
    }

    private static final String DEFAULT_CHARSET = Charset.defaultCharset().name();
}
