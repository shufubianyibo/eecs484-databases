import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONArray;

public class GetData {

    static String prefix = "project3.";

    // You must use the following variable as the JDBC connection
    Connection oracleConnection = null;

    // You must refer to the following variables for the corresponding 
    // tables in your database
    String userTableName = null;
    String friendsTableName = null;
    String cityTableName = null;
    String currentCityTableName = null;
    String hometownCityTableName = null;

    // DO NOT modify this constructor
    public GetData(String u, Connection c) {
        super();
        String dataType = u;
        oracleConnection = c;
        userTableName = prefix + dataType + "_USERS";
        friendsTableName = prefix + dataType + "_FRIENDS";
        cityTableName = prefix + dataType + "_CITIES";
        currentCityTableName = prefix + dataType + "_USER_CURRENT_CITIES";
        hometownCityTableName = prefix + dataType + "_USER_HOMETOWN_CITIES";
    }

    public JSONArray toJSON() throws SQLException {
        JSONArray users_info = new JSONArray();
        Map<Integer, JSONObject> users = new LinkedHashMap<>();

        // Left joins retain users with no current city or hometown.
        String userQuery = "SELECT u.USER_ID, u.FIRST_NAME, u.LAST_NAME, u.GENDER, "
                + "u.YEAR_OF_BIRTH, u.MONTH_OF_BIRTH, u.DAY_OF_BIRTH, "
                + "c.CITY_ID AS CURRENT_ID, c.CITY_NAME AS CURRENT_CITY, "
                + "c.STATE_NAME AS CURRENT_STATE, c.COUNTRY_NAME AS CURRENT_COUNTRY, "
                + "h.CITY_ID AS HOMETOWN_ID, h.CITY_NAME AS HOMETOWN_CITY, "
                + "h.STATE_NAME AS HOMETOWN_STATE, h.COUNTRY_NAME AS HOMETOWN_COUNTRY "
                + "FROM " + userTableName + " u "
                + "LEFT JOIN " + currentCityTableName + " uc ON u.USER_ID = uc.USER_ID "
                + "LEFT JOIN " + cityTableName + " c ON uc.CURRENT_CITY_ID = c.CITY_ID "
                + "LEFT JOIN " + hometownCityTableName + " uh ON u.USER_ID = uh.USER_ID "
                + "LEFT JOIN " + cityTableName + " h ON uh.HOMETOWN_CITY_ID = h.CITY_ID "
                + "ORDER BY u.USER_ID";

        try (Statement stmt = oracleConnection.createStatement();
                ResultSet rows = stmt.executeQuery(userQuery)) {
            while (rows.next()) {
                int id = rows.getInt("USER_ID");
                JSONObject user = new JSONObject();
                user.put("user_id", id);
                user.put("first_name", rows.getString("FIRST_NAME"));
                user.put("last_name", rows.getString("LAST_NAME"));
                user.put("gender", rows.getString("GENDER"));
                user.put("YOB", rows.getInt("YEAR_OF_BIRTH"));
                user.put("MOB", rows.getInt("MONTH_OF_BIRTH"));
                user.put("DOB", rows.getInt("DAY_OF_BIRTH"));
                user.put("friends", new JSONArray());
                user.put("current", readCity(rows, "CURRENT"));
                user.put("hometown", readCity(rows, "HOMETOWN"));
                users.put(id, user);
                users_info.put(user);
            }
        }

        // Normalize symmetric edges and export each only on the smaller ID.
        String friendQuery = "SELECT DISTINCT LEAST(USER1_ID, USER2_ID) AS SMALL_ID, "
                + "GREATEST(USER1_ID, USER2_ID) AS LARGE_ID FROM " + friendsTableName
                + " WHERE USER1_ID <> USER2_ID ORDER BY SMALL_ID, LARGE_ID";
        try (Statement stmt = oracleConnection.createStatement();
                ResultSet rows = stmt.executeQuery(friendQuery)) {
            while (rows.next()) {
                users.get(rows.getInt("SMALL_ID")).getJSONArray("friends")
                        .put(rows.getInt("LARGE_ID"));
            }
        }
        return users_info;
    }

    private JSONObject readCity(ResultSet rows, String label) throws SQLException {
        JSONObject city = new JSONObject();
        if (rows.getObject(label + "_ID") != null) {
            city.put("city", rows.getString(label + "_CITY"));
            city.put("state", rows.getString(label + "_STATE"));
            city.put("country", rows.getString(label + "_COUNTRY"));
        }
        return city;
    }

    // This outputs to a file "output.json"
    // DO NOT MODIFY this function
    public void writeJSON(JSONArray users_info) {
        try {
            FileWriter file = new FileWriter(System.getProperty("user.dir") + "/output.json");
            file.write(users_info.toString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
