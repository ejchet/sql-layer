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

package com.akiban.server.types.util;

import com.akiban.server.types.AkType;
import com.akiban.server.types.ValueSource;
import com.akiban.server.types.extract.Extractors;
import com.akiban.server.types.extract.LongExtractor;
import com.akiban.util.ByteSource;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.io.IOException;
import java.util.Formatter;

public class SqlLiteralValueFormatter
{
    private Appendable buffer;
    private Formatter formatter;

    public SqlLiteralValueFormatter(Appendable buffer) {
        this.buffer = buffer;
    }
    
    public void append(ValueSource source) throws IOException {
        append(source, source.getConversionType());
    }

    public void append(ValueSource source, AkType type) throws IOException {
        if (source.isNull()) {
            buffer.append("NULL");
            return;
        }
        switch (type) {
        case DATE:
            appendDate(source.getDate());
            break;
        case DATETIME:
            appendDateTime(source.getDateTime());
            break;
        case DECIMAL:
            appendDecimal(source.getDecimal());
            break;
        case DOUBLE:
            appendDouble(source.getDouble());
            break;
        case FLOAT:
            appendDouble(source.getFloat());
            break;
        case INT:
            appendLong(source.getInt());
            break;
        case LONG:
            appendLong(source.getLong());
            break;
        case VARCHAR:
            appendVarchar(source.getString());
            break;
        case TEXT:
            appendVarchar(source.getText());
            break;
        case TIME:
            appendTime(source.getTime());
            break;
        case TIMESTAMP:
            appendTimestamp(source.getTimestamp());
            break;
        case U_BIGINT:
            appendUBigInt(source.getUBigInt());
            break;
        case U_DOUBLE:
            appendDouble(source.getUDouble());
            break;
        case U_FLOAT:
            appendDouble(source.getUFloat());
            break;
        case U_INT:
            appendLong(source.getUInt());
            break;
        case VARBINARY:
            appendVarBinary(source.getVarBinary());
            break;
        case YEAR:
            appendLong(source.getYear());
            break;
        case BOOL:
            appendBool(source.getBool());
            break;
        }
    }

    protected void appendDecimal(BigDecimal value) throws IOException {
        buffer.append(value.toString());
    }

    protected void appendDouble(double value) throws IOException {
        if (formatter == null)
            formatter = new Formatter(buffer);
        formatter.format("%e", value);
    }

    protected void appendLong(long value) throws IOException {
        buffer.append(Long.toString(value));
    }

    protected void appendVarchar(String value) throws IOException {
        buffer.append('\'');
        if (value.indexOf('\'') < 0)
            buffer.append(value);
        else {
            for (int i = 0; i < value.length(); i++) {
                int ch = value.charAt(i);
                if (ch == '\'')
                    buffer.append('\'');
                buffer.append((char)ch);
            }
        }
        buffer.append('\'');
    }

    protected void appendUBigInt(BigInteger value) throws IOException {
        buffer.append(value.toString());
    }

    private char hexDigit(int n) {
        if (n < 10)
            return (char)('0' + n);
        else
            return (char)('A' + n - 10);
    }

    protected void appendVarBinary(ByteSource value) throws IOException {
        buffer.append("X'");
        byte[] byteArray = value.byteArray();
        int byteArrayOffset = value.byteArrayOffset();
        int byteArrayLength = value.byteArrayLength();
        for (int i = 0; i < byteArrayLength; i++) {
            int b = byteArray[byteArrayOffset + i] & 0xFF;
            buffer.append(hexDigit(b >> 8));
            buffer.append(hexDigit(b & 0xF));
        }
        buffer.append('\'');
    }

    protected void appendBool(boolean value) throws IOException {
        buffer.append((value) ? "TRUE" : "FALSE");
    }

    LongExtractor dateExtractor, dateTimeExtractor, timeExtractor, timestampExtractor;

    protected void appendDate(long value) throws IOException {
        if (dateExtractor == null)
            dateExtractor = Extractors.getLongExtractor(AkType.DATE);
        buffer.append("DATE '");
        buffer.append(dateExtractor.asString(value));
        buffer.append('\'');
    }

    protected void appendDateTime(long value) throws IOException {
        if (dateExtractor == null)
            dateExtractor = Extractors.getLongExtractor(AkType.DATETIME);
        buffer.append("TIMESTAMP '");
        buffer.append(dateExtractor.asString(value));
        buffer.append('\'');
    }

    protected void appendTime(long value) throws IOException {
        if (dateExtractor == null)
            dateExtractor = Extractors.getLongExtractor(AkType.TIME);
        buffer.append("TIME '");
        buffer.append(dateExtractor.asString(value));
        buffer.append('\'');
    }

    protected void appendTimestamp(long value) throws IOException {
        if (dateExtractor == null)
            dateExtractor = Extractors.getLongExtractor(AkType.TIMESTAMP);
        buffer.append("TIMESTAMP '");
        buffer.append(dateExtractor.asString(value));
        buffer.append('\'');
    }

}