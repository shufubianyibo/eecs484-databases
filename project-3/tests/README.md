# Offline verification

Run from the project root with Node.js and JDK 17 or later. No database, credentials, network, or package installation is required.

```sh
node tests/offline.js
build_dir=$(mktemp -d /tmp/484p3-test.XXXXXX)
javac --release 17 -d "$build_dir" -cp 'ojdbc6.jar:json-20151123.jar:json_simple-1.1.jar:.' Main.java GetData.java tests/GetDataOfflineTest.java
java -cp "$build_dir:json-20151123.jar" GetDataOfflineTest
```

The JavaScript suite loads the actual query files into an isolated shell mock. It compares complete query results against independently calculated results for `sample.json` and synthetic fixtures. It checks the supplied starter expectations, strict age boundaries, both friendship directions, age ties, user ID zero, missing cities, isolated users, empty input, repeated collection creation, and preservation of `users`.

The Java suite supplies JDBC proxies to the actual exporter. It checks field mapping, numeric values, city ID zero, empty city objects and friend arrays, JSON escaping, resource cleanup, empty input, and propagation of SQL failures.

These mocks do not execute SQL or MongoDB commands on a server. Live Oracle schema/query validation, full Oracle export comparison with `sample.json`, and execution against MongoDB 3.6 remain unverified. `Main.java` emits a pre-existing deprecated API warning during compilation.

# Plan, implementation, and audit

1. **Plan:** Extract the PDF requirements and preserve starter function signatures, table variables, the constructor, and `writeJSON()`.
2. **Implement:** Read users and optional cities with left joins; attach distinct friendships to the smaller user ID. Implement queries 1–7 using find/cursor operations and aggregation stages available in MongoDB 3.6.
3. **Audit:** Query 1 follows the PDF's hometown requirement, correcting the starter comment. Query 2 excludes the source `_id` to avoid duplicate output IDs. Query 3 uses distinct IDs and excludes absent cities. Query 4 uses strict year bounds and checks both friendship directions. Query 5 resolves ties by numeric user ID and needs no helper collections. Query 6 includes users with empty friend arrays and counts only stored edges. Query 7 groups with `$sum: 1`, sorts by month, and produces all 12 represented months on the supplied dataset. On smaller inputs it emits the months present in the data. SQL exceptions propagate instead of returning a partial export.
4. **Test:** Java compilation, JDBC fixture tests, and all seven offline query tests passed. Live database checks were skipped as requested.

Submit `GetData.java` and `query1.js` through `query7.js`; these test files are supplemental.
