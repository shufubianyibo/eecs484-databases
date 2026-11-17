import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import org.json.*;

/** JDBC fixture test. Validates export mapping and cleanup, not Oracle SQL execution. */
public class GetDataOfflineTest {
    private static final List<String> queries = new ArrayList<>();
    private static int statementsClosed;
    private static int resultsClosed;
    private static boolean fail;

    private static Map<String, Object> user(int id, boolean hasCity) {
        Map<String, Object> row = new HashMap<>();
        row.put("USER_ID", id);
        row.put("FIRST_NAME", "A\"\\\n");
        row.put("LAST_NAME", "Example");
        row.put("GENDER", "female");
        row.put("YEAR_OF_BIRTH", 1990);
        row.put("MONTH_OF_BIRTH", 10);
        row.put("DAY_OF_BIRTH", 2);
        for (String label : new String[] { "CURRENT", "HOMETOWN" }) {
            if (hasCity) {
                row.put(label + "_ID", 0);
                row.put(label + "_CITY", label + " town");
                row.put(label + "_STATE", "State");
                row.put(label + "_COUNTRY", "Country");
            }
        }
        return row;
    }

    private static ResultSet result(List<Map<String, Object>> rows) {
        int[] position = { -1 };
        return (ResultSet) Proxy.newProxyInstance(GetDataOfflineTest.class.getClassLoader(),
            new Class<?>[] { ResultSet.class }, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "next": return ++position[0] < rows.size();
                    case "getInt": return ((Number) rows.get(position[0]).get(args[0])).intValue();
                    case "getString": case "getObject": return rows.get(position[0]).get(args[0]);
                    case "close": resultsClosed++; return null;
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
    }

    private static Connection connection(boolean empty) {
        return (Connection) Proxy.newProxyInstance(GetDataOfflineTest.class.getClassLoader(),
            new Class<?>[] { Connection.class }, (proxy, method, args) -> {
                if (!method.getName().equals("createStatement")) {
                    throw new AssertionError("Exporter must not close the supplied connection");
                }
                return Proxy.newProxyInstance(GetDataOfflineTest.class.getClassLoader(),
                    new Class<?>[] { Statement.class }, (statement, operation, parameters) -> {
                        if (operation.getName().equals("close")) { statementsClosed++; return null; }
                        if (!operation.getName().equals("executeQuery")) throw new UnsupportedOperationException();
                        String sql = (String) parameters[0];
                        queries.add(sql);
                        if (fail) throw new SQLException("fixture failure");
                        if (empty) return result(Collections.emptyList());
                        if (sql.contains("_USERS")) return result(Arrays.asList(user(0, true), user(7, false), user(9, false)));
                        return result(Collections.singletonList(Map.of("SMALL_ID", 0, "LARGE_ID", 7)));
                    });
            });
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        JSONArray output = new GetData("FIXTURE", connection(false)).toJSON();
        check(output.length() == 3, "all users retained");
        JSONObject first = output.getJSONObject(0);
        check(first.length() == 10, "exact user fields");
        check(first.getInt("user_id") == 0 && first.getInt("YOB") == 1990 &&
            first.getInt("MOB") == 10 && first.getInt("DOB") == 2, "numeric fields");
        check(first.getJSONArray("friends").getInt(0) == 7, "larger friend stored");
        check(first.getJSONObject("current").getString("city").equals("CURRENT town"), "current city");
        check(first.getJSONObject("hometown").getString("city").equals("HOMETOWN town"), "hometown city");
        check(output.getJSONObject(1).getJSONObject("current").length() == 0, "missing city is empty object");
        check(output.getJSONObject(1).getJSONObject("hometown").length() == 0, "missing hometown is empty object");
        check(output.getJSONObject(1).getJSONArray("friends").length() == 0, "incoming edge excluded");
        check(output.getJSONObject(2).getJSONArray("friends").length() == 0, "isolated user has empty array");
        check(new JSONArray(output.toString()).getJSONObject(0).getString("first_name").equals("A\"\\\n"), "JSON escaping");
        for (String table : new String[] { "USERS", "CITIES", "USER_CURRENT_CITIES", "USER_HOMETOWN_CITIES" }) {
            check(queries.get(0).contains("project3.FIXTURE_" + table), "constructor table: " + table);
        }
        check(queries.get(0).contains("LEFT JOIN"), "retain missing cities");
        check(queries.get(1).contains("project3.FIXTURE_FRIENDS") && queries.get(1).contains("LEAST") &&
            queries.get(1).contains("GREATEST") && queries.get(1).contains("DISTINCT"), "normalize and deduplicate edges");
        check(statementsClosed == 2 && resultsClosed == 2, "JDBC resources closed");
        check(new GetData("FIXTURE", connection(true)).toJSON().length() == 0, "empty database");
        fail = true;
        try {
            new GetData("FIXTURE", connection(false)).toJSON();
            throw new AssertionError("SQL exception swallowed");
        } catch (SQLException expected) {
            check(expected.getMessage().equals("fixture failure"), "original SQL failure preserved");
        }
        check(statementsClosed == 5 && resultsClosed == 4, "cleanup including SQL failure");
        System.out.println("PASS: exporter mapping, empty data, JSON escaping, JDBC cleanup, and SQL error propagation (offline mock).");
    }
}
