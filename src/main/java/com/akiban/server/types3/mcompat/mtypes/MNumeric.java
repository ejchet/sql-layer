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

package com.akiban.server.types3.mcompat.mtypes;

import com.akiban.server.types3.TClass;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.TFactory;
import com.akiban.server.types3.TInstance;
import com.akiban.server.types3.TParser;
import com.akiban.server.types3.TParsers;
import com.akiban.server.types3.TypeDeclarationException;
import com.akiban.server.types3.*;
import com.akiban.server.types3.aksql.AkCategory;
import com.akiban.server.types3.common.types.NumericAttribute;
import com.akiban.server.types3.common.NumericFormatter;
import com.akiban.server.types3.common.types.SimpleDtdTClass;
import com.akiban.server.types3.mcompat.MBundle;
import com.akiban.server.types3.pvalue.PUnderlying;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;
import com.akiban.sql.types.TypeId;

public class MNumeric extends SimpleDtdTClass {

    protected MNumeric(String name, TClassFormatter formatter, int serializationSize, PUnderlying pUnderlying,
                       int defaultWidth, TParser parser)
    {
        super(MBundle.INSTANCE.id(), name, AkCategory.INTEGER,
                formatter,
                NumericAttribute.class,
                1, 1, serializationSize, 
                pUnderlying, parser, inferTypeid(name));
        this.defaultWidth = defaultWidth;
    }

    private static TypeId inferTypeid(String name) {
        if (name.contains("unsigned"))
            return TypeId.INTEGER_UNSIGNED_ID;
        else
            return TypeId.INTEGER_ID;
    }

    @Override
    public TInstance instance() {
        return instance(defaultWidth);
    }

    @Override
    public void putSafety(TExecutionContext context, 
                          TInstance sourceInstance,
                          PValueSource sourceValue,
                          TInstance targetInstance,
                          PValueTarget targetValue)
    {
       assert sourceInstance.typeClass() instanceof MNumeric
                    && targetInstance.typeClass() instanceof MNumeric
               : "expected instances of mcompat.types.MNumeric";
       targetValue.putValueSource(sourceValue);
    }
        
    @Override
    protected void validate(TInstance instance) {
        int m = instance.attribute(NumericAttribute.WIDTH);
        if (m < 0 || m > 255)
            throw new TypeDeclarationException("width must be 0 < M < 256");
    }

    @Override
    public TFactory factory() {
        return new MNumericFactory(this);
    }

    @Override
    protected TInstance doPickInstance(TInstance instance0, TInstance instance1) {
        // Determine precision of TInstance
        /*switch (mode) {
            case COMBINE:
            case CHOOSE:
        }*/
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private final int defaultWidth;
    
    // numeric types
    // TODO verify default widths
    public static final MNumeric TINYINT 
            = new MNumeric("tinyint", NumericFormatter.FORMAT.INT_8, 1, PUnderlying.INT_8, 5, TParsers.TINYINT); 

    public static final MNumeric TINYINT_UNSIGNED 
            = new MNumeric("tinyint unsigned", NumericFormatter.FORMAT.INT_16, 4, PUnderlying.INT_16, 4, TParsers.UNSIGNED_TINYINT);

    public static final MNumeric SMALLINT 
            = new MNumeric("smallint", NumericFormatter.FORMAT.INT_16, 2, PUnderlying.INT_16, 7, TParsers.SMALLINT);

    public static final MNumeric SMALLINT_UNSIGNED 
            = new MNumeric("smallint unsigned", NumericFormatter.FORMAT.INT_32, 4, PUnderlying.INT_32, 6, TParsers.UNSIGNED_SMALLINT);

    public static final MNumeric MEDIUMINT 
            = new MNumeric("mediumint", NumericFormatter.FORMAT.INT_32, 3, PUnderlying.INT_32, 9, TParsers.MEDIUMINT);

    public static final MNumeric MEDIUMINT_UNSIGNED 
            = new MNumeric("mediumint unsigned", NumericFormatter.FORMAT.INT_64, 8, PUnderlying.INT_64, 8, TParsers.UNSIGNED_MEDIUMINT);

    public static final MNumeric INT 
            = new MNumeric("int", NumericFormatter.FORMAT.INT_32, 4, PUnderlying.INT_32, 11, TParsers.INT);

    public static final MNumeric INT_UNSIGNED 
            = new MNumeric("int unsigned", NumericFormatter.FORMAT.INT_64, 8, PUnderlying.INT_64, 10, TParsers.UNSIGNED_INT);

    public static final MNumeric BIGINT 
            = new MNumeric("bigint", NumericFormatter.FORMAT.INT_64, 8, PUnderlying.INT_64, 21, TParsers.BIGINT);
    public static final MNumeric BIGINT_UNSIGNED
            = new MNumeric("bigintunsigned", NumericFormatter.FORMAT.INT_64, 8, PUnderlying.INT_64, 20, TParsers.UNSIGNED_BIGINT);

    public static final TClass DECIMAL = new MBigDecimal();

    public static final TClass DECIMAL_UNSIGNED = new MBigDecimal();
}