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

package com.akiban.qp.operator;

import com.akiban.ais.model.GroupTable;
import com.akiban.ais.model.Index;
import com.akiban.ais.model.UserTable;
import com.akiban.qp.expression.IndexKeyRange;
import com.akiban.qp.row.HKey;
import com.akiban.qp.row.Row;
import com.akiban.qp.rowtype.AggregatedRowType;
import com.akiban.qp.rowtype.RowType;
import com.akiban.server.aggregation.Aggregator;
import com.akiban.server.aggregation.AggregatorFactory;
import com.akiban.server.error.InconvertibleTypesException;
import com.akiban.server.types.AkType;
import com.akiban.server.types.ValueSource;
import com.akiban.server.types.ValueTarget;
import com.akiban.server.types.conversion.Converters;
import com.akiban.server.types.extract.Extractors;
import com.akiban.server.types.util.ValueHolder;
import com.akiban.util.Strings;
import com.akiban.util.WrappingByteSource;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class Aggregate_PartialTest {

    @Test
    public void simpleNoGroupBy() {
        TestOperator input = new TestOperator(new RowsBuilder(AkType.LONG)
                .row(1L)
                .row(2L)
                .row(3L)
        );
        AggregatedRowType rowType = new AggregatedRowType(null, 1, input.rowType());
        Operator plan = new Aggregate_Partial(input, 0, Collections.singletonList(TEST_AGGREGATOR), rowType);
        Deque<Row> expected = new RowsBuilder(AkType.VARCHAR)
                .row("1, 2, 3")
                .rows();
        check(plan, expected);
    }

    @Test
    public void simpleWithGroupBy() {
        TestOperator input = new TestOperator(new RowsBuilder(AkType.LONG, AkType.LONG)
                .row(1L, 10L)
                .row(1L, 11L)
                .row(2L, 12L)
                .row(3L, 13L)
        );
        AggregatedRowType rowType = new AggregatedRowType(null, 1, input.rowType());
        Operator plan = new Aggregate_Partial(input, 1, Collections.singletonList(TEST_AGGREGATOR), rowType);
        Deque<Row> expected = new RowsBuilder(AkType.LONG, AkType.VARCHAR)
                .row(1L, "10, 11")
                .row(2L, "12")
                .row(3L, "13")
                .rows();
        check(plan, expected);
    }

    /**
     * Situation like <tt>SELECT name FROM customers GROUP BY name</tt>. In this case, all of the columns are
     * GROUP BY, and there's no aggregator.
     */
    @Test
    public void groupByWithoutAggregators() {
        TestOperator input = new TestOperator(new RowsBuilder(AkType.LONG)
                .row(1L)
                .row(2L)
                .row(3L)
        );
        AggregatedRowType rowType = new AggregatedRowType(null, 1, input.rowType());
        Operator plan = new Aggregate_Partial(input, 1, Collections.<AggregatorFactory>emptyList(), rowType);
        Deque<Row> expected = new RowsBuilder(AkType.LONG)
                .row(1L)
                .row(2L)
                .row(3L)
                .rows();
        check(plan, expected);
    }

    @Test
    public void partiallyOrderedGroupBy() {
        TestOperator input = new TestOperator(new RowsBuilder(AkType.LONG, AkType.LONG)
                .row(null, 8L)
                .row(null, 9L)
                .row(1L, 10L)
                .row(2L, 11L)
                .row(3L, 12L)
                .row(null, 13L)
                .row(null, 14L)
                .row(1L, 15L)
                .row(1L, 16L)
        );
        AggregatedRowType rowType = new AggregatedRowType(null, 1, input.rowType());
        Operator plan = new Aggregate_Partial(input, 1, Collections.singletonList(TEST_AGGREGATOR), rowType);
        Deque<Row> expected = new RowsBuilder(AkType.LONG, AkType.VARCHAR)
                .row(null, "8, 9")
                .row(1L, "10")
                .row(2L, "11")
                .row(3L, "12")
                .row(null, "13, 14")
                .row(1L, "15, 16")
                .rows();
        check(plan, expected);
    }

    @Test
    public void twoColumnGroupBy() {
        TestOperator input = new TestOperator(new RowsBuilder(AkType.LONG, AkType.VARCHAR, AkType.LONG)
                .row(1L, "alpha", 1)
                .row(1L, "alpha", 2)
                .row(1L, "bravo", 3)
                .row(2L, "bravo", 4)
                .row(2L, "charlie", 5)
        );
        AggregatedRowType rowType = new AggregatedRowType(null, 1, input.rowType());
        Operator plan = new Aggregate_Partial(input, 2, Collections.singletonList(TEST_AGGREGATOR), rowType);
        Deque<Row> expected = new RowsBuilder(AkType.LONG, AkType.VARCHAR, AkType.VARCHAR)
                .row(1L, "alpha", "1, 2")
                .row(1L, "bravo", "3")
                .row(2L, "bravo", "4")
                .row(2L, "charlie", "5")
                .rows();
        check(plan, expected);
    }

    @Test
    public void noInputRowsNoGroupBy() {
        TestOperator input = new TestOperator(new RowsBuilder(AkType.LONG));
        AggregatedRowType rowType = new AggregatedRowType(null, 1, input.rowType());
        Operator plan = new Aggregate_Partial(input, 0, Collections.singletonList(TEST_AGGREGATOR), rowType);
        Deque<Row> expected = new RowsBuilder(AkType.VARCHAR)
                .row(new ValueHolder(AkType.VARCHAR, EMPTY))
                .rows();
        check(plan, expected);
    }

    @Test
    public void noInputRowsWithGroupBy() {
        TestOperator input = new TestOperator(new RowsBuilder(AkType.LONG, AkType.LONG));
        AggregatedRowType rowType = new AggregatedRowType(null, 1, input.rowType());
        Operator plan = new Aggregate_Partial(input, 1, Collections.singletonList(TEST_AGGREGATOR), rowType);
        Deque<Row> expected = new RowsBuilder(AkType.LONG, AkType.VARCHAR).rows();
        check(plan, expected);
    }

    @Test
    public void uninterestingRowsPassThrough() {
        RowsBuilder boringRows = new RowsBuilder(AkType.VARCHAR, AkType.LONG)
                .row("A", 1L)
                .row("B", 2L)
                .row("C", 3L);
        RowsBuilder interestingRows = new RowsBuilder(AkType.LONG, AkType.LONG)
                .row(10L, 100L)
                .row(10L, 101L)
                .row(20L, 200L);
        // Shuffle rows into this order
        // (10, 100)
        // ("A", 1)
        // (10, 101)
        // ("B", 2)
        // (20, 200)
        // ("C", 3)
        // We'll expect the output in this order:
        // ("A", 1)
        // ("B", 2)
        // (10, "100, 101")
        // ("C", 3)
        // (20, "200")
        Deque<Row> shuffled = shuffle(interestingRows.rows(), boringRows.rows(), new ArrayDeque<Row>());
        // now in this order:

        TestOperator input = new TestOperator(shuffled, interestingRows.rowType());
        AggregatedRowType rowType = new AggregatedRowType(null, 1, input.rowType());
        Operator plan = new Aggregate_Partial(input, 1, Collections.singletonList(TEST_AGGREGATOR), rowType);

        // Create the expected output, including rows that have passed through
        Deque<Row> expected = new RowsBuilder(AkType.LONG, AkType.VARCHAR)
                .row(10L, "100, 101")
                .row(20L, "200")
                .rows();
        List<Row> expectedFull = new ArrayList<Row>();
        expectedFull.addAll(boringRows.rows());
        expectedFull.addAll(expected);
        Collections.swap(expectedFull, 2, 3);

        // Finally, check
        check(plan, expectedFull);
    }

    @Test(expected = InconvertibleTypesException.class)
    public void invalidRowTypes() {
        Operator plan;
        try {
            TestOperator input = new TestOperator(new RowsBuilder(AkType.LONG, AkType.VARBINARY)
                    .row(wrapLong(1L), wrapBytes(0x01, 0x02))
            );
            AggregatedRowType rowType = new AggregatedRowType(null, 1, input.rowType());
            plan = new Aggregate_Partial(input, 1, Collections.singletonList(TEST_AGGREGATOR), rowType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        execute(plan);
    }

    @Test(expected = NullPointerException.class)
    public void inputIsNull() {
        AggregatedRowType rowType;
        try {
            TestOperator input = new TestOperator(new RowsBuilder(AkType.LONG, AkType.VARBINARY)
                    .row(wrapLong(1L), wrapBytes(0x01, 0x02))
            );
            rowType = new AggregatedRowType(null, 1, input.rowType());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        new Aggregate_Partial(null, 1, Collections.singletonList(TEST_AGGREGATOR), rowType);
    }

    @Test(expected = NullPointerException.class)
    public void factoriesListIsNull() {
        TestOperator input;
        AggregatedRowType rowType;
        try {
            input = new TestOperator(new RowsBuilder(AkType.LONG, AkType.VARBINARY)
                    .row(wrapLong(1L), wrapBytes(0x01, 0x02))
            );
            rowType = new AggregatedRowType(null, 1, input.rowType());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        new Aggregate_Partial(input, 1, null, rowType);
    }

    @Test(expected = NullPointerException.class)
    public void rowTypeIsNull() {
        TestOperator input;
        try {
            input = new TestOperator(new RowsBuilder(AkType.LONG, AkType.VARBINARY)
                    .row(wrapLong(1L), wrapBytes(0x01, 0x02))
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        new Aggregate_Partial(input, 1, Collections.singletonList(TEST_AGGREGATOR), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void inputsIndexTooLow() {
        TestOperator input;
        AggregatedRowType rowType;
        try {
            input = new TestOperator(new RowsBuilder(AkType.LONG, AkType.VARBINARY)
                    .row(wrapLong(1L), wrapBytes(0x01, 0x02))
            );
            rowType = new AggregatedRowType(null, 1, input.rowType());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        new Aggregate_Partial(input, -1, Collections.singletonList(TEST_AGGREGATOR), rowType);
    }

    @Test(expected = IllegalArgumentException.class)
    public void inputsIndexTooHigh() {
        TestOperator input;
        AggregatedRowType rowType;
        try {
            input = new TestOperator(new RowsBuilder(AkType.LONG, AkType.VARBINARY)
                    .row(wrapLong(1L), wrapBytes(0x01, 0x02))
            );
            rowType = new AggregatedRowType(null, 1, input.rowType());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        new Aggregate_Partial(input, 2, Collections.singletonList(TEST_AGGREGATOR), rowType);
    }

    private static void check(Operator plan, Collection<Row> expecteds) {
        List<Row> actuals = execute(plan);
        if (expecteds.size() != actuals.size()) {
            assertEquals("output", Strings.join(expecteds), Strings.join(actuals));
            assertEquals("size (expecteds=" + expecteds+", actuals=" + actuals + ')', expecteds.size(), actuals.size());
        }
        int rowCount = 0;
        try {
            Iterator<Row> expectedsIter = expecteds.iterator();
            for (Row actual : actuals) {
                Row expected = expectedsIter.next();
                for (int i = 0; i < plan.rowType().nFields(); ++i) {
                    ValueHolder actualHolder = new ValueHolder(actual.eval(i));
                    ValueHolder expectedHolder = new ValueHolder(expected.eval(i));

                    if (!expectedHolder.equals(actualHolder)) {
                        assertEquals(
                                String.format("row[%d] field[%d]", rowCount, i),
                                str(expecteds),
                                str(actuals)
                        );
                        assertEquals(String.format("row[%d] field[%d]", rowCount, i), expectedHolder, actualHolder);
                        throw new AssertionError("should have failed by now!");
                    }
                }
                ++rowCount;
            }
        }
        finally {
            for (Row actual : actuals) {
                actual.release();
            }
        }
    }

    private static <R extends Row,C extends Collection<? super Row>>
    C shuffle(Collection<? extends R> first, Collection<? extends R> second, C output)
    {
        Iterator<? extends Row> secondIter = second.iterator();
        for (Row fromFirst : first) {
            output.add(fromFirst);
            if (secondIter.hasNext()) {
                output.add(secondIter.next());
            }
        }
        while (secondIter.hasNext()) {
            output.add(secondIter.next());
        }
        assert output.size() == first.size() + second.size()
                : String.format("%d != %d + %d: %s from %s %s",
                    output.size(), first.size(), second.size(), output, first, second
        );
        return output;
    }

    private static String str(Collection<? extends Row> rows) {
        return Strings.join(rows);
    }

    private static List<Row> execute(Operator plan) {
        List<Row> rows = new ArrayList<Row>();
        Cursor cursor = plan.cursor(ADAPTER);
        cursor.open(UndefBindings.only());
        try {
            for(Row row = cursor.next(); row != null; row = cursor.next()) {
                row.acquire();
                rows.add(row);
            }
            return rows;
        } finally {
            cursor.close();
        }
    }

    private ValueHolder wrapBytes(int... bytes) {
        byte[] asBytes = new byte[bytes.length];
        for (int i=0; i < bytes.length; ++i) {
            byte asByte = (byte)bytes[i];
            asBytes[i] = asByte;
        }
        return new ValueHolder(AkType.VARBINARY, new WrappingByteSource(asBytes));
    }

    private ValueHolder wrapLong(long value) {
        return new ValueHolder(AkType.LONG, value);
    }

    // const

    private static final String EMPTY = "empty";
    private static final TestAdapter ADAPTER = new TestAdapter();
    private static final AggregatorFactory TEST_AGGREGATOR = new AggregatorFactory() {
        @Override
        public Aggregator get() {
            return new TestAggregator();
        }
    };

    // nested classes

    private static class TestAggregator implements Aggregator {

        @Override
        public AkType outputType() {
            return AkType.VARCHAR;
        }

        @Override
        public void input(ValueSource input) {
            long value = Extractors.getLongExtractor(AkType.LONG).getLong(input);
            result.append(value).append(", ");
        }

        @Override
        public void output(ValueTarget output) {
            final String asString;
            if (result.length() == 0) {
                asString = "";
            }
            else {
                result.setLength(result.length() - 2);
                asString = result.toString();
            }
            result.setLength(0);
            Converters.convert(new ValueHolder(AkType.VARCHAR, asString), output);
        }

        @Override
        public ValueSource emptyValue() {
            return new ValueHolder(AkType.VARCHAR, EMPTY);
        }

        private StringBuilder result = new StringBuilder();
    }

    private static class TestAdapter extends StoreAdapter
    {
        @Override
        public GroupCursor newGroupCursor(GroupTable groupTable)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cursor newIndexCursor(Index index, boolean reverse, IndexKeyRange keyRange, UserTable innerJoinUntil)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public HKey newHKey(RowType rowType)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateRow(Row oldRow, Row newRow, Bindings bindings)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeRow(Row newRow, Bindings bindings)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteRow(Row oldRow, Bindings bindings)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cursor sort(Cursor input, RowType rowType, API.Ordering ordering, Bindings bindings)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkQueryCancelation()
        {
        }

        public TestAdapter()
        {
            super(null);
        }
    }
}