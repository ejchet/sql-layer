package com.akiban.ais.ddl;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.akiban.ais.model.UserTable;
import org.junit.Test;

import com.akiban.ais.io.CSVTarget;
import com.akiban.ais.io.Writer;
import com.akiban.ais.model.AkibaInformationSchema;
import com.akiban.ais.model.Table;
import com.akiban.ais.model.TableName;

public class DDLSourceTest {

    private final static String DDL_FILE_NAME = "src/test/resources/DDLSourceTest_schema.ddl";
    private final static String XXXXXXXX_DDL_FILE_NAME = "src/test/resources/xxxxxxxx_schema.ddl";
    private final static String XXXXXXXX_FK_DDL_FILE_NAME = "src/test/resources/xxxxxxxx_schema.ddl";

    private final static String SCHEMA_NAME = "DDLSourceTest_schema";

    @Test
    public void testParseEnumAndSet() throws Exception {
        DDLSource parser = new DDLSource();
        AkibaInformationSchema ais = parser.buildAIS(DDL_FILE_NAME);

        final Table enumTable = ais.getTable(new TableName(SCHEMA_NAME,
                "with_enum"));
        assertEquals(1, enumTable.getColumn("e4").getMaxStorageSize()
                .intValue());
        assertEquals(1, enumTable.getColumn("e255").getMaxStorageSize()
                .intValue());
        assertEquals(2, enumTable.getColumn("e10000").getMaxStorageSize()
                .intValue());

        final Table setTable = ais.getTable(new TableName(SCHEMA_NAME,
                "with_set"));
        assertEquals(1, setTable.getColumn("s8").getMaxStorageSize().intValue());
        assertEquals(2, setTable.getColumn("s16").getMaxStorageSize()
                .intValue());
        assertEquals(3, setTable.getColumn("s24").getMaxStorageSize()
                .intValue());
        assertEquals(4, setTable.getColumn("s32").getMaxStorageSize()
                .intValue());
        assertEquals(8, setTable.getColumn("s64").getMaxStorageSize()
                .intValue());
    }

    @Test
    public void testFKParse() throws Exception {
        final AkibaInformationSchema ais1 = new DDLSource()
                .buildAIS(XXXXXXXX_DDL_FILE_NAME);
        final AkibaInformationSchema ais2 = new DDLSource()
                .buildAIS(XXXXXXXX_FK_DDL_FILE_NAME);
        final StringWriter aisw1 = new StringWriter();
        final StringWriter aisw2 = new StringWriter();
        new Writer(new CSVTarget(new PrintWriter(aisw1))).save(ais1);
        new Writer(new CSVTarget(new PrintWriter(aisw2))).save(ais2);
        assertEquals(aisw1.getBuffer().toString(), aisw2.getBuffer().toString());
    }

    @Test
    public void testOverloadedTableColumn() throws Exception {
        String ddl = "CREATE TABLE `s1`.one (idOne int, PRIMARY KEY (idOne)) engine=akibandb;\n" +
                    "CREATE TABLE `s2`.one (idTwo int, PRIMARY KEY (idTwo)) engine=akibandb;";

        SchemaDef schemaDef = DDLSource.parseSchemaDef(ddl);
        assertEquals("user tables", 2, schemaDef.getUserTableMap().size());
        assertEquals("group tables", 2, schemaDef.getGroupMap().size());
        assertTrue("s1.one missing", schemaDef.getGroupMap().containsKey(new SchemaDef.CName("akiba_objects", "one")));
        assertTrue("s2.one missing", schemaDef.getGroupMap().containsKey(new SchemaDef.CName("akiba_objects", "one$0")));

        AkibaInformationSchema ais = new DDLSource().buildAISFromString(ddl);
        assertEquals("user tables", 2, ais.getUserTables().size());
        assertEquals("group tables", 2, ais.getGroupTables().size());
        UserTable s1 = ais.getUserTable("s1", "one");
        UserTable s2 = ais.getUserTable("s2", "one");
        assertNotNull("s1", s1);
        assertNotNull("s2", s2);
        assertSame("s1 group's root", s1, ais.getGroup("one").getGroupTable().getRoot());
        assertSame("s2 group's root", s2, ais.getGroup("one$0").getGroupTable().getRoot());
    }

    @Test
    public void columnMarkedAsPrimaryKey() throws Exception {
        SchemaDef.UserTableDef tableDef = createTableFromInner("id int PRIMARY KEY, whatever int");
        assertEquals("columns", Arrays.asList("id", "whatever"), tableDef.getColumnNames());
        assertEquals("PK columns", Arrays.asList("id"), tableDef.getPrimaryKey());
        assertEquals("other indexes", 0, tableDef.indexes.size());
    }

    @Test
    public void columnMarkedAsKey() throws Exception {
        SchemaDef.UserTableDef tableDef = createTableFromInner("id int KEY, whatever int");
        assertEquals("columns", Arrays.asList("id", "whatever"), tableDef.getColumnNames());
        assertEquals("PK columns", Arrays.asList("id"), tableDef.getPrimaryKey());
        assertEquals("other indexes", 0, tableDef.indexes.size());
    }

    @Test
    public void tableHasTwoPKsIndexes() throws Exception {
        createTableWithError(DDLSource.ParseException.class,
                "too many primary keys",
                "id int, sid int, PRIMARY KEY (id), PRIMARY KEY (sid)");
    }

    @Test
    public void tableHasTwoPrimaryColumns() throws Exception {
        createTableWithError(DDLSource.ParseException.class,
                "too many primary keys",
                "id int primary key, sid int primary key");
    }

    @Test
    public void tableMixesPrimaryColumnAndPkIndex() {
        createTableWithError(DDLSource.ParseException.class,
                "too many primary keys",
                "id int, sid int primary key, primary key(id)");
    }

    @Test
    public void testAkibanFKUnnamedIndex() throws Exception{
        SchemaDef.UserTableDef tableDef = new DDLSource().parseCreateTable(
                "two (id int, oid int, PRIMARY KEY (id), " +
                "CONSTRAINT `__akiban_fk` FOREIGN KEY (`oid`) REFERENCES zebra (id) ) engine=akibandb;");

        assertEquals("schema", null, tableDef.getCName().getSchema());
        assertEquals("table", "two", tableDef.getCName().getName());

        assertEquals("columns", 2, tableDef.getColumns().size());
        assertEquals("column[0]", "id", tableDef.getColumns().get(0).getName());
        assertEquals("column[1]", "oid", tableDef.getColumns().get(1).getName());

        assertEquals("PK columns", 1, tableDef.primaryKey.size());
        assertEquals("PK[0]", "id", tableDef.primaryKey.get(0));

        assertEquals("indexes", 1, tableDef.indexes.size());
        assertEquals("index[0] name", "_auto_generated_index_0", tableDef.indexes.get(0).name);
        assertEquals("index[0] constraints", 1, tableDef.indexes.get(0).constraints.size());
        assertEquals("index[0] constraint[0]", "__akiban_fk", tableDef.indexes.get(0).constraints.get(0));
        assertEquals("index[0] child columns", 1, tableDef.indexes.get(0).getChildColumns().size());
        assertEquals("index[0] child column[0]", "oid", tableDef.indexes.get(0).getChildColumns().get(0));
        assertEquals("index[0] parent schema", null, tableDef.indexes.get(0).getParentSchema());
        assertEquals("index[0] parent table", "zebra", tableDef.indexes.get(0).getParentTable());
        assertEquals("index[0] parent columns", 1, tableDef.indexes.get(0).getParentColumns().size());
        assertEquals("index[0] parent column[0]", "id", tableDef.indexes.get(0).getParentColumns().get(0));
    }

    private static void createTableWithError(Class<? extends Throwable> errClass, String errMessage, String innerDDL) {
        Throwable thrown = null;
        try {
            createTableFromInner(innerDDL);
        } catch (Throwable t) {
            thrown = t;
        }
        assertNotNull("expected " + errClass, thrown);
        assertEquals("exception class", errClass, thrown.getClass());
        assertEquals("exception message", errMessage, thrown.getMessage());
    }

    private static SchemaDef.UserTableDef createTableFromInner(String ddl) throws Exception {
        SchemaDef.UserTableDef ret = new DDLSource().parseCreateTable(
                "s.t (" + ddl + ") engine=akibandb;"
        );
        assertEquals("schema", "s", ret.getCName().getSchema());
        assertEquals("table", "t", ret.getCName().getName());
        assertEquals("engine", "akibandb", ret.engine);
        return ret;
    }

    @Test
    public void testParseCreateTable() throws Exception{
        SchemaDef.UserTableDef tableDef = new DDLSource().parseCreateTable(
                "two (id int, oid int, PRIMARY KEY (id), " +
                "CONSTRAINT `__akiban_fk` FOREIGN KEY `__akiban_index` (`oid`) REFERENCES zebra (id) ) engine=akibandb;");

        assertEquals("schema", null, tableDef.getCName().getSchema());
        assertEquals("table", "two", tableDef.getCName().getName());

        assertEquals("columns", 2, tableDef.getColumns().size());
        assertEquals("column[0]", "id", tableDef.getColumns().get(0).getName());
        assertEquals("column[1]", "oid", tableDef.getColumns().get(1).getName());

        assertEquals("PK columns", 1, tableDef.primaryKey.size());
        assertEquals("PK[0]", "id", tableDef.primaryKey.get(0));

        assertEquals("indexes", 1, tableDef.indexes.size());
        assertEquals("index[0] name", "__akiban_index", tableDef.indexes.get(0).name);
        assertEquals("index[0] constraints", 1, tableDef.indexes.get(0).constraints.size());
        assertEquals("index[0] constraint[0]", "__akiban_fk", tableDef.indexes.get(0).constraints.get(0));
        assertEquals("index[0] child columns", 1, tableDef.indexes.get(0).getChildColumns().size());
        assertEquals("index[0] child column[0]", "oid", tableDef.indexes.get(0).getChildColumns().get(0));
        assertEquals("index[0] parent schema", null, tableDef.indexes.get(0).getParentSchema());
        assertEquals("index[0] parent table", "zebra", tableDef.indexes.get(0).getParentTable());
        assertEquals("index[0] parent columns", 1, tableDef.indexes.get(0).getParentColumns().size());
        assertEquals("index[0] parent column[0]", "id", tableDef.indexes.get(0).getParentColumns().get(0));
    }

    public static void main(final String[] args) throws Exception {
        final BufferedReader console = new BufferedReader(
                new InputStreamReader(System.in));
        for (;;) {
            System.out.println();
            System.out.print("Enter a line to parse: ");
            final String line = console.readLine();
            if (line == null) {
                break;
            }
            final DDLSource ddlSource = new DDLSource();
            try {
                final SchemaDef.UserTableDef tableDef = ddlSource
                        .parseCreateTable(line);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            System.out.println("OK");
        }
    }

    private void testCOCommon(SchemaDef schemaDef) throws Exception {
        assertEquals("tables length", 2, schemaDef.getUserTableMap().size());

        SchemaDef.UserTableDef customer = schemaDef.getUserTableMap().get(new SchemaDef.CName("schema", "customer"));

        assertEquals("indexes size", 0, customer.indexes.size());

        assertEquals("columns size", 1, customer.columns.size());
        testColumn(0, customer, "id", "INT", null, null, true, null);

        assertEquals("engine", "akibandb", customer.engine);
    }

    private static void testColumn(int which, SchemaDef.UserTableDef table,
                                   final String name, String typeName, String param1, String param2, boolean nullable,
                                   String autoIncrement, String... constraints) {

        SchemaDef.ColumnDef actual = table.columns.get(which);
        SchemaDef.ColumnDef expected = new SchemaDef.ColumnDef(name, typeName, param1, param2, nullable, autoIncrement,
                constraints);
        assertEquals("column " + which, expected, actual);
    }


    @Test
    public void fKFullDescription() throws Exception {
        SchemaDef def = parseCO(true, "my_constraint0", "my_index0");
        testCOCommon(def);
        testOrder(def, "my_constraint0", "my_index0");
    }

    @Test
    public void fKHasConstraintNoIndex() throws Exception {
        SchemaDef def = parseCO(true, "my_constraint0", null);
        testCOCommon(def);
        testOrder(def, "my_constraint0", null);
    }

    @Test
    public void fKNoConstraintHasIndex() throws Exception {
        SchemaDef def = parseCO(true, null, "my_index1");
        testCOCommon(def);
        testOrder(def, null, "my_index1");
    }

    @Test
    public void fKNoConstraintOrIndex() throws Exception {
        SchemaDef def = parseCO(true, null, null);
        testCOCommon(def);
        testOrder(def, null, null);
    }

    private void testOrder(SchemaDef def, String constraintName, String indexName) {
        SchemaDef.UserTableDef order = def.getUserTableMap().get(new SchemaDef.CName("schema", "order"));

        assertEquals("columns len", 2, order.columns.size());
        testColumn(0, order, "id", "INT", null, null, true, null);
        testColumn(1, order, "cid", "INT", null, null, true, null);

        List<SchemaDef.IndexColumnDef> columns = Arrays.asList(new SchemaDef.IndexColumnDef("cid"));
        SchemaDef.CName customerName = new SchemaDef.CName("schema", "customer");
        assertEquals("indexes size", indexName == null ? 2 : 3, order.indexes.size());

        List<String> index0Constraints = new ArrayList<String>();
        Set<SchemaDef.IndexQualifier> index0qualifiers = EnumSet.noneOf(SchemaDef.IndexQualifier.class);
        SchemaDef.CName index0ReferenceTable = null;
        List<String> index0ReferenceColumns = new ArrayList<String>();
        if (indexName == null) {
            if (constraintName != null) {
                index0Constraints.add(constraintName);
            }
            index0qualifiers.add(SchemaDef.IndexQualifier.FOREIGN_KEY);
            index0ReferenceTable = customerName;
            index0ReferenceColumns.add("id");
        }
        SchemaDef.IndexDef plainKey = new SchemaDef.IndexDef("given_key",
                index0qualifiers,
                columns, index0ReferenceTable, index0ReferenceColumns, index0Constraints);
        assertEquals("index 0", plainKey, order.indexes.get(0));

        SchemaDef.IndexDef givenFKIndex = new SchemaDef.IndexDef("givenFk",
                EnumSet.of(SchemaDef.IndexQualifier.FOREIGN_KEY),
                columns, customerName, Arrays.asList("id"), Arrays.asList("givenConstraint"));
        assertEquals("index 1", givenFKIndex, order.indexes.get(1));

        if (indexName != null) {
            List<String> index2Constraints = new ArrayList<String>();
            if (constraintName != null) {
                index2Constraints.add(constraintName);
            }
            SchemaDef.IndexDef definedFKIndex = new SchemaDef.IndexDef(indexName,
                    EnumSet.of(SchemaDef.IndexQualifier.FOREIGN_KEY),
                    columns, customerName, Arrays.asList("id"), index2Constraints);
            assertEquals("index 2", definedFKIndex, order.indexes.get(2));
        }
    }

    /**
     * Tests this class's private {@linkplain #fk(String, boolean, String, String, String...)} method.
     */
    @Test
    public void fkCreation() {
        assertEquals("full",
                "CONSTRAINT `fk0` FOREIGN KEY `index0` (`id`) REFERENCES `parent` (`pid`)",
                fk("parent", true, "fk0", "index0", "parent:pid", "child:id"));
        assertEquals("no constraint name",
                "CONSTRAINT FOREIGN KEY `index0` (`id`) REFERENCES `parent` (`pid`)",
                fk("parent", true, null, "index0", "parent:pid", "child:id"));
        assertEquals("no constraint",
                "FOREIGN KEY `index0` (`id`) REFERENCES `parent` (`pid`)",
                fk("parent", false, "ignored", "index0", "parent:pid", "child:id"));
        assertEquals("no index",
                "FOREIGN KEY (`id`) REFERENCES `parent` (`pid`)",
                fk("parent", false, "ignored", null, "parent:pid", "child:id"));
        assertEquals("same columns",
                "FOREIGN KEY (`pid`) REFERENCES `parent` (`pid`)",
                fk("parent", false, "ignored", null, "pid"));
    }

    /**
     * Creates the customer-order tables and parses them into a SchemaDef.
     * @param includeConstraint see {@linkplain #fk(String, boolean, String, String, String...)}
     * @param constraintName see {@linkplain #fk(String, boolean, String, String, String...)}
     * @param indexName see {@linkplain #fk(String, boolean, String, String, String...)}
     * @return a SchemaDef with two tables, customer and order. The order's FK will use the given params.
     */
    private static SchemaDef parseCO(boolean includeConstraint, String constraintName, String indexName) throws Exception {
        StringBuilder ret = new StringBuilder();
        ret.append("CREATE TABLE `schema`.`customer` (`id` INT, PRIMARY KEY (`id`)) engine=akibandb;\n");

        ret.append("CREATE TABLE `schema`.`order` (`id` INT, `cid` INT, PRIMARY KEY (`id`), ");
        ret.append("KEY `given_key` (`cid`), ");
        ret.append("CONSTRAINT `givenConstraint` FOREIGN KEY `givenFk` (`cid`) REFERENCES `customer` (`id`), ");
        ret.append(fk("customer", includeConstraint, constraintName, indexName, "parent:id", "child:cid"));
        ret.append(") engine=akibandb;");

        return DDLSource.parseSchemaDef(ret.toString());
    }

    /**
     * Generates a FK statement
     * @param parent the parent table name
     * @param includeConstraint whether to include the "CONSTRAINT [symbol]" part of the string
     * @param constraintName (optional) if non-null and includesConstraint is true, the constraint symbol
     * @param indexName (optional) the index name
     * @param columns the columns. Any column that starts with "parent:" will apply only to the parent; any that
     * starts with "child:" will apply only to the child. All others will apply to both
     * @return the FK
     */
    private static String fk(String parent, boolean includeConstraint, String constraintName, String indexName, String... columns) {
        List<String> parentCols = new ArrayList<String>(columns.length);
        List<String> childCols = new ArrayList<String>(columns.length);
        for (String column : columns) {
            if (column.startsWith("parent")) {
                parentCols.add(column.substring("parent:".length()));
            }
            else if (column.startsWith("child:")) {
                childCols.add(column.substring("child:".length()));
            }
            else {
                parentCols.add(column);
                childCols.add(column);
            }
        }
        assertEquals("column mismatch between parent " + parentCols +" and child " + childCols,
                parentCols.size(), childCols.size());

        StringBuilder builder = new StringBuilder();
        if (includeConstraint) {
            builder.append("CONSTRAINT ");
            if (constraintName != null) {
                TableName.escape(constraintName, builder);
                builder.append(' ');
            }
        }
        builder.append("FOREIGN KEY ");
        if (indexName != null) {
            TableName.escape(indexName, builder);
            builder.append(' ');
        }
        builder.append('(');
        for (String column : childCols) {
            TableName.escape(column, builder);
            builder.append(',');
        }
        builder.setLength(builder.length() - 1);
        builder.append(')');

        builder.append(" REFERENCES ");
        TableName.escape(parent, builder);
        builder.append(' ');

        builder.append('(');
        for (String column : parentCols) {
            TableName.escape(column, builder);
            builder.append(',');
        }
        builder.setLength(builder.length() - 1);
        builder.append(')');

        return builder.toString();
    }
}
