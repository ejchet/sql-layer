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

import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.common.BigDecimalWrapper;
import com.akiban.server.types3.mcompat.mtypes.MBigDecimal;
import com.akiban.server.types3.mcompat.mtypes.MBigDecimal.Attrs;
import com.akiban.server.types3.mcompat.mtypes.MBigDecimalWrapper;
import com.akiban.server.types3.pvalue.PValueTarget;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CastUtils
{
    public static long round (long max, long min, double val, TExecutionContext context)
    {
        long rounded = Math.round(val);
        
        if (Double.compare(rounded, val) != 0)
            context.reportTruncate(Double.toString(val), Long.toString(rounded));
        return getInRange(max, min, rounded, context);
    }

    public static long getInRange (long max, long min, long val, TExecutionContext context)
    {
        if (val > max)
        {
            context.reportTruncate(Long.toString(val), Long.toString(max));
            return max;
        }
        else if (val < min)
        {
            context.reportTruncate(Long.toString(val), Long.toString(min));
            return min;
        }
        else
            return val;
    }

    public static long getInRange (long max, long min, long val, long defaultVal, TExecutionContext context)
    {
        if (val > max)
        {
            context.reportTruncate(Long.toString(val), Long.toString(defaultVal));
            return defaultVal;
        }
        else if (val < min)
        {
            context.reportTruncate(Long.toString(val), Long.toString(defaultVal));
            return defaultVal;
        }
        else
            return val;
    }
     
     public static String getNum(int scale, int precision)
    {
        assert precision >= scale : "precision has to be >= scale";
        
        char val[] = new char[precision + 1];
        Arrays.fill(val, '9');
        val[precision - scale] = '.';
        
        return new String(val);
    }
  
    public static void doCastDecimal(TExecutionContext context,
                            BigDecimalWrapper num,
                            PValueTarget out)
    {
        int pre = num.getPrecision();
        int scale = num.getScale();

        int expectedPre = context.outputTInstance().attribute(Attrs.PRECISION);
        int expectedScale = context.outputTInstance().attribute(Attrs.SCALE);

        BigDecimalWrapper meta[] = (BigDecimalWrapper[]) context.outputTInstance().getMetaData();

        if (meta == null)
        {
            // compute the max value:
            meta = new BigDecimalWrapper[2];
            meta[MBigDecimal.MAX_INDEX] = new MBigDecimalWrapper(getNum(expectedScale, expectedPre));
            meta[MBigDecimal.MIN_INDEX] = meta[MBigDecimal.MAX_INDEX].negate();

            context.outputTInstance().setMetaData(meta);
        }

        if (num.compareTo(meta[MBigDecimal.MAX_INDEX]) >= 0)
            out.putObject(meta[MBigDecimal.MAX_INDEX]);
        else if (num.compareTo(meta[MBigDecimal.MIN_INDEX]) <= 0)
            out.putObject(meta[MBigDecimal.MIN_INDEX]);
        else if (scale > expectedScale) // check the sacle
            out.putObject(num.round(expectedPre, expectedScale));
        else // else put the original value
            out.putObject(num);
    }

    public static MBigDecimalWrapper parseDecimalString (String st, TExecutionContext context)
    {
        Matcher m = DOUBLE_PATTERN.matcher(st);
        
        m.lookingAt();
        String truncated = st.substring(0, m.end());
        
        if (!truncated.equals(st))
            context.reportTruncate(st, truncated);
        
        MBigDecimalWrapper ret = MBigDecimalWrapper.ZERO;
        try
        {
            ret = new MBigDecimalWrapper(truncated);
        }
        catch (NumberFormatException e)
        {
            context.reportBadValue(e.getMessage());
        }
        return ret;
    }
    /**
     * Parse the st for a double value
     * MySQL compat in that illegal digits will be truncated and won't cause
     * NumberFormatException
     * 
     * @param st
     * @param context
     * @return 
     */
    public static double parseDoubleString(String st, TExecutionContext context)
    {      
        Matcher m = DOUBLE_PATTERN.matcher(st);

        m.lookingAt();
        String truncated = st.substring(0, m.end());

        if (!truncated.equals(st))
        {
            context.reportTruncate(st, truncated);
        }

        double ret = 0;
        try
        {
            ret = Double.parseDouble(truncated);
        }
        catch (NumberFormatException e)
        {
            context.reportBadValue(e.getMessage());
        }

       return ret;
    }
    
    public static long parseInRange(String st, long max, long min, TExecutionContext context)
    {
        String truncated;

        // first attempt
        try
        {
            return CastUtils.getInRange(Long.parseLong(st), max, min, context);
        }
        catch (NumberFormatException e)
        {
            truncated = CastUtils.truncateNonDigits(st, context);
        }

        // second attempt
        return CastUtils.getInRange(Long.parseLong(truncated), max, min, context);
    }
    
    /**
     * Truncate non-digits part
     * @param st
     * @return 
     */
    public static String truncateNonDigits(String st, TExecutionContext context)
    {
        int n = 0;
        st = st.trim();
        
        // if the string starts with non-digits, return 0
        if (st.isEmpty() || !Character.isDigit(st.charAt(n++)) 
                && (n == st.length() || n < st.length() && !Character.isDigit(st.charAt(n++))))
        {
            context.reportTruncate(st, "0");
            return "0";
        }
        
        char ch;
        boolean sawDot = false;
        for (; n < st.length(); ++n)
            if ((!Character.isDigit(ch = st.charAt(n)))
                    && (sawDot || !(sawDot = ch == '.')))
            {
                context.reportTruncate(st ,(st = st.substring(0, n)));
                return st;
            }
        
        return st;
    }
    
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("([-+]?\\d*)(\\.?\\d+)?([eE][-+]?\\d+)?");
}