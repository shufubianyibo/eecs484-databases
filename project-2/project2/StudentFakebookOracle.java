package project2;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/*
    The StudentFakebookOracle class is derived from the FakebookOracle class and implements
    the abstract query functions that investigate the database provided via the <connection>
    parameter of the constructor to discover specific information.
*/
public final class StudentFakebookOracle extends FakebookOracle {
    // [Constructor]
    // REQUIRES: <connection> is a valid JDBC connection
    public StudentFakebookOracle(Connection connection) {
        oracle = connection;
    }

    @Override
    // Query 0
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the total number of users for which a birth month is listed
    //        (B) Find the birth month in which the most users were born
    //        (C) Find the birth month in which the fewest users (at least one) were born
    //        (D) Find the IDs, first names, and last names of users born in the month
    //            identified in (B)
    //        (E) Find the IDs, first names, and last name of users born in the month
    //            identified in (C)
    //
    // This query is provided to you completed for reference. Below you will find the appropriate
    // mechanisms for opening up a statement, executing a query, walking through results, extracting
    // data, and more things that you will need to do for the remaining nine queries
    public BirthMonthInfo findMonthOfBirthInfo() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            // Step 1
            // ------------
            // * Find the total number of users with birth month info
            // * Find the month in which the most users were born
            // * Find the month in which the fewest (but at least 1) users were born
            ResultSet rst = stmt.executeQuery(
                    "SELECT COUNT(*) AS Birthed, Month_of_Birth " + // select birth months and number of uses with that birth month
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth IS NOT NULL " + // for which a birth month is available
                            "GROUP BY Month_of_Birth " + // group into buckets by birth month
                            "ORDER BY Birthed DESC, Month_of_Birth ASC"); // sort by users born in that month, descending; break ties by birth month

            int mostMonth = 0;
            int leastMonth = 0;
            int total = 0;
            while (rst.next()) { // step through result rows/records one by one
                if (rst.isFirst()) { // if first record
                    mostMonth = rst.getInt(2); //   it is the month with the most
                }
                if (rst.isLast()) { // if last record
                    leastMonth = rst.getInt(2); //   it is the month with the least
                }
                total += rst.getInt(1); // get the first field's value as an integer
            }
            BirthMonthInfo info = new BirthMonthInfo(total, mostMonth, leastMonth);

            // Step 2
            // ------------
            // * Get the names of users born in the most popular birth month
            rst = stmt.executeQuery(
                    "SELECT User_ID, First_Name, Last_Name " + // select ID, first name, and last name
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth = " + mostMonth + " " + // born in the most popular birth month
                            "ORDER BY User_ID"); // sort smaller IDs first

            while (rst.next()) {
                info.addMostPopularBirthMonthUser(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }

            // Step 3
            // ------------
            // * Get the names of users born in the least popular birth month
            rst = stmt.executeQuery(
                    "SELECT User_ID, First_Name, Last_Name " + // select ID, first name, and last name
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth = " + leastMonth + " " + // born in the least popular birth month
                            "ORDER BY User_ID"); // sort smaller IDs first

            while (rst.next()) {
                info.addLeastPopularBirthMonthUser(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }

            // Step 4
            // ------------
            // * Close resources being used
            rst.close();
            stmt.close(); // if you close the statement first, the result set gets closed automatically

            return info;

        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new BirthMonthInfo(-1, -1, -1);
        }
    }

    @Override
    // Query 1
    // -----------------------------------------------------------------------------------
    // GOALS: (A) The first name(s) with the most letters
    //        (B) The first name(s) with the fewest letters
    //        (C) The first name held by the most users
    //        (D) The number of users whose first name is that identified in (C)
    public FirstNameInfo findNameInfo() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                FirstNameInfo info = new FirstNameInfo();
                info.addLongName("Aristophanes");
                info.addLongName("Michelangelo");
                info.addLongName("Peisistratos");
                info.addShortName("Bob");
                info.addShortName("Sue");
                info.addCommonName("Harold");
                info.addCommonName("Jessica");
                info.setCommonNameCount(42);
                return info;
            */
           
            ResultSet rst = stmt.executeQuery(
                    "SELECT First_Name, COUNT(*) AS NameCount " +
                            "FROM " + UsersTable + " " +
                            "GROUP BY First_Name " +
                            "ORDER BY LENGTH(First_Name) DESC, NameCount DESC");
            
            String longestName = "";
            String shortestName = "";
            String mostCommonName = "";
            int mostCommonCount = 0;

            while (rst.next()) {
                String firstName = rst.getString(1);
                int count = rst.getInt(2);

                // Check for longest name
                if (firstName.length() > longestName.length()) {
                    longestName = firstName;
                }

                // Check for shortest name
                if (shortestName.isEmpty() || firstName.length() < shortestName.length()) {
                    shortestName = firstName;
                }

                // Check for most common name
                if (count > mostCommonCount) {
                    mostCommonCount = count;
                    mostCommonName = firstName;
                }
            }

            FirstNameInfo info = new FirstNameInfo();

            // Add longest name(s)
            rst = stmt.executeQuery(
                    "SELECT First_Name, COUNT(*) AS NameCount " +
                            "FROM " + UsersTable + " " +
                            "GROUP BY First_Name " +
                            "HAVING LENGTH(First_Name) = " + longestName.length() +
                            " ORDER BY First_Name");

            while (rst.next()) {
                info.addLongName(rst.getString(1));}
            
            // Add shortest name(s)
            rst = stmt.executeQuery(
                    "SELECT First_Name, COUNT(*) AS NameCount " +
                            "FROM " + UsersTable + " " +
                            "GROUP BY First_Name " +
                            "HAVING LENGTH(First_Name) = " + shortestName.length() +
                            " ORDER BY First_Name");
            while (rst.next()) {
                info.addShortName(rst.getString(1));}

            // Add most common name(s)
            rst = stmt.executeQuery(
                    "SELECT First_Name, COUNT(*) AS NameCount " +
                            "FROM " + UsersTable + " " +
                            "GROUP BY First_Name " +
                            "HAVING COUNT(*) = " + mostCommonCount +
                            " ORDER BY First_Name");
            while (rst.next()) {
                info.addCommonName(rst.getString(1));
                info.setCommonNameCount(mostCommonCount);}
            
            rst.close();
            stmt.close();
           
            return info; 
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new FirstNameInfo();
        }
    }

    @Override
    // Query 2
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of users without any friends
    //
    // Be careful! Remember that if two users are friends, the Friends table only contains
    // the one entry (U1, U2) where U1 < U2.
    public FakebookArrayList<UserInfo> lonelyUsers() throws SQLException {
        FakebookArrayList<UserInfo> results = new FakebookArrayList<UserInfo>(", ");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(15, "Abraham", "Lincoln");
                UserInfo u2 = new UserInfo(39, "Margaret", "Thatcher");
                results.add(u1);
                results.add(u2);
            */
            ResultSet rst = stmt.executeQuery(
                    "SELECT User_ID, First_Name, Last_Name " +
                            "FROM " + UsersTable + " " +
                            "WHERE User_ID NOT IN (SELECT User1_ID FROM " + FriendsTable + ") " +
                            "AND User_ID NOT IN (SELECT User2_ID FROM " + FriendsTable + ") " +
                            "ORDER BY User_ID");

            while (rst.next()) {
                long userId = rst.getLong(1);
                String firstName = rst.getString(2);
                String lastName = rst.getString(3);

                UserInfo userInfo = new UserInfo(userId, firstName, lastName);
                results.add(userInfo);
            }


            rst.close();
            stmt.close();
        
           
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 3
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of users who no longer live
    //            in their hometown (i.e. their current city and their hometown are different)
    public FakebookArrayList<UserInfo> liveAwayFromHome() throws SQLException {
        FakebookArrayList<UserInfo> results = new FakebookArrayList<UserInfo>(", ");
        try (Statement stmt = oracle.createStatement();
             ResultSet rst = stmt.executeQuery(
                     "SELECT u.User_ID, u.First_Name, u.Last_Name " +
                     "FROM " + UsersTable + " u " +
                     "JOIN " + CurrentCitiesTable + " cc ON cc.User_ID = u.User_ID " +
                     "JOIN " + HometownCitiesTable + " hc ON hc.User_ID = u.User_ID " +
                     "WHERE cc.Current_City_ID <> hc.Hometown_City_ID ORDER BY u.User_ID")) {
            while (rst.next()) {
                results.add(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }
        }
        return results;
    }

    @Override
    // Query 4
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, links, and IDs and names of the containing album of the top
    //            <num> photos with the most tagged users
    //        (B) For each photo identified in (A), find the IDs, first names, and last names
    //            of the users therein tagged
    public FakebookArrayList<TaggedPhotoInfo> findPhotosWithMostTags(int num) throws SQLException {
        FakebookArrayList<TaggedPhotoInfo> results = new FakebookArrayList<TaggedPhotoInfo>("\n");
        if (num <= 0) {
            return results;
        }
        // Rank photo IDs before joining metadata; fetch all selected tags in one query.
        String sql = "WITH TopPhotos AS (SELECT * FROM (" +
                "SELECT Tag_Photo_ID, COUNT(*) AS TagCount FROM " + TagsTable + " " +
                "GROUP BY Tag_Photo_ID ORDER BY TagCount DESC, Tag_Photo_ID) " +
                "WHERE ROWNUM <= " + num + ") " +
                "SELECT p.Photo_ID, p.Album_ID, p.Photo_Link, a.Album_Name, " +
                "u.User_ID, u.First_Name, u.Last_Name " +
                "FROM TopPhotos top JOIN " + PhotosTable + " p ON p.Photo_ID = top.Tag_Photo_ID " +
                "JOIN " + AlbumsTable + " a ON a.Album_ID = p.Album_ID " +
                "JOIN " + TagsTable + " t ON t.Tag_Photo_ID = p.Photo_ID " +
                "JOIN " + UsersTable + " u ON u.User_ID = t.Tag_Subject_ID " +
                "ORDER BY top.TagCount DESC, p.Photo_ID, u.User_ID";
        try (Statement stmt = oracle.createStatement();
             ResultSet rst = stmt.executeQuery(sql)) {
            TaggedPhotoInfo photo = null;
            long lastPhoto = 0;
            while (rst.next()) {
                long photoId = rst.getLong(1);
                if (photo == null || photoId != lastPhoto) {
                    photo = new TaggedPhotoInfo(new PhotoInfo(photoId, rst.getLong(2),
                            rst.getString(3), rst.getString(4)));
                    results.add(photo);
                    lastPhoto = photoId;
                }
                photo.addTaggedUser(new UserInfo(rst.getLong(5), rst.getString(6), rst.getString(7)));
            }
        }
        return results;
    }

    @Override
    // Query 5
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, last names, and birth years of each of the two
    //            users in the top <num> pairs of users that meet each of the following
    //            criteria:
    //              (i) same gender
    //              (ii) tagged in at least one common photo
    //              (iii) difference in birth years is no more than <yearDiff>
    //              (iv) not friends
    //        (B) For each pair identified in (A), find the IDs, links, and IDs and names of
    //            the containing album of each photo in which they are tagged together
    public FakebookArrayList<MatchPair> matchMaker(int num, int yearDiff) throws SQLException {
        FakebookArrayList<MatchPair> results = new FakebookArrayList<MatchPair>("\n");
        if (num <= 0 || yearDiff < 0) {
            return results;
        }
        // Aggregate only IDs, then retrieve names and shared photos for the top pairs.
        String sql = "WITH TopPairs AS (SELECT * FROM (" +
                "SELECT t1.Tag_Subject_ID AS User1_ID, t2.Tag_Subject_ID AS User2_ID, " +
                "COUNT(*) AS PhotoCount " +
                "FROM " + TagsTable + " t1 JOIN " + TagsTable + " t2 " +
                "ON t1.Tag_Photo_ID = t2.Tag_Photo_ID AND t1.Tag_Subject_ID < t2.Tag_Subject_ID " +
                "JOIN " + UsersTable + " u1 ON u1.User_ID = t1.Tag_Subject_ID " +
                "JOIN " + UsersTable + " u2 ON u2.User_ID = t2.Tag_Subject_ID " +
                "WHERE u1.Gender = u2.Gender " +
                "AND ABS(u1.Year_of_Birth - u2.Year_of_Birth) <= " + yearDiff + " " +
                "AND NOT EXISTS (SELECT 1 FROM " + FriendsTable + " f " +
                "WHERE f.User1_ID = t1.Tag_Subject_ID AND f.User2_ID = t2.Tag_Subject_ID) " +
                "GROUP BY t1.Tag_Subject_ID, t2.Tag_Subject_ID " +
                "ORDER BY PhotoCount DESC, t1.Tag_Subject_ID, t2.Tag_Subject_ID) " +
                "WHERE ROWNUM <= " + num + ") " +
                "SELECT u1.User_ID, u1.First_Name, u1.Last_Name, u1.Year_of_Birth, " +
                "u2.User_ID, u2.First_Name, u2.Last_Name, u2.Year_of_Birth, " +
                "p.Photo_ID, p.Album_ID, p.Photo_Link, a.Album_Name " +
                "FROM TopPairs top " +
                "JOIN " + UsersTable + " u1 ON u1.User_ID = top.User1_ID " +
                "JOIN " + UsersTable + " u2 ON u2.User_ID = top.User2_ID " +
                "JOIN " + TagsTable + " t1 ON t1.Tag_Subject_ID = top.User1_ID " +
                "JOIN " + TagsTable + " t2 ON t2.Tag_Subject_ID = top.User2_ID " +
                "AND t2.Tag_Photo_ID = t1.Tag_Photo_ID " +
                "JOIN " + PhotosTable + " p ON p.Photo_ID = t1.Tag_Photo_ID " +
                "JOIN " + AlbumsTable + " a ON a.Album_ID = p.Album_ID " +
                "ORDER BY top.PhotoCount DESC, top.User1_ID, top.User2_ID, p.Photo_ID";
        try (Statement stmt = oracle.createStatement();
             ResultSet rst = stmt.executeQuery(sql)) {
            MatchPair pair = null;
            long lastUser1 = 0;
            long lastUser2 = 0;
            while (rst.next()) {
                long user1 = rst.getLong(1);
                long user2 = rst.getLong(5);
                if (pair == null || user1 != lastUser1 || user2 != lastUser2) {
                    pair = new MatchPair(new UserInfo(user1, rst.getString(2), rst.getString(3)),
                            rst.getLong(4), new UserInfo(user2, rst.getString(6), rst.getString(7)),
                            rst.getLong(8));
                    results.add(pair);
                    lastUser1 = user1;
                    lastUser2 = user2;
                }
                pair.addSharedPhoto(new PhotoInfo(rst.getLong(9), rst.getLong(10),
                        rst.getString(11), rst.getString(12)));
            }
        }
        return results;
    }

    @Override
    // Query 6
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of each of the two users in
    //            the top <num> pairs of users who are not friends but have a lot of
    //            common friends
    //        (B) For each pair identified in (A), find the IDs, first names, and last names
    //            of all the two users' common friends
    public FakebookArrayList<UsersPair> suggestFriends(int num) throws SQLException {
        FakebookArrayList<UsersPair> results = new FakebookArrayList<UsersPair>("\n");
        if (num <= 0) {
            return results;
        }
        // Each undirected friendship is stored once, with the smaller ID first.
        // Keep the large aggregation narrow and join user details only after top-N.
        String sql = "WITH Friendships AS (" +
                "SELECT User1_ID AS User_ID, User2_ID AS Friend_ID FROM " + FriendsTable +
                " UNION ALL SELECT User2_ID, User1_ID FROM " + FriendsTable + "), " +
                "TopPairs AS (SELECT * FROM (" +
                "SELECT f1.User_ID AS User1_ID, f2.User_ID AS User2_ID, COUNT(*) AS CommonCount " +
                "FROM Friendships f1 JOIN Friendships f2 " +
                "ON f1.Friend_ID = f2.Friend_ID AND f1.User_ID < f2.User_ID " +
                "WHERE NOT EXISTS (SELECT 1 FROM " + FriendsTable + " f " +
                "WHERE f.User1_ID = f1.User_ID AND f.User2_ID = f2.User_ID) " +
                "GROUP BY f1.User_ID, f2.User_ID " +
                "ORDER BY CommonCount DESC, f1.User_ID, f2.User_ID) WHERE ROWNUM <= " + num + ") " +
                "SELECT u1.User_ID, u1.First_Name, u1.Last_Name, " +
                "u2.User_ID, u2.First_Name, u2.Last_Name, u.User_ID, u.First_Name, u.Last_Name " +
                "FROM TopPairs top " +
                "JOIN " + UsersTable + " u1 ON u1.User_ID = top.User1_ID " +
                "JOIN " + UsersTable + " u2 ON u2.User_ID = top.User2_ID " +
                "JOIN Friendships f1 ON f1.User_ID = top.User1_ID " +
                "JOIN Friendships f2 ON f2.User_ID = top.User2_ID AND f2.Friend_ID = f1.Friend_ID " +
                "JOIN " + UsersTable + " u ON u.User_ID = f1.Friend_ID " +
                "ORDER BY top.CommonCount DESC, top.User1_ID, top.User2_ID, u.User_ID";
        try (Statement stmt = oracle.createStatement();
             ResultSet rst = stmt.executeQuery(sql)) {
            UsersPair pair = null;
            long lastUser1 = 0;
            long lastUser2 = 0;
            while (rst.next()) {
                long user1 = rst.getLong(1);
                long user2 = rst.getLong(4);
                if (pair == null || user1 != lastUser1 || user2 != lastUser2) {
                    pair = new UsersPair(new UserInfo(user1, rst.getString(2), rst.getString(3)),
                            new UserInfo(user2, rst.getString(5), rst.getString(6)));
                    results.add(pair);
                    lastUser1 = user1;
                    lastUser2 = user2;
                }
                pair.addSharedFriend(new UserInfo(rst.getLong(7), rst.getString(8), rst.getString(9)));
            }
        }
        return results;
    }

    @Override
    // Query 7
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the name of the state or states in which the most events are held
    //        (B) Find the number of events held in the states identified in (A)
    public EventStateInfo findEventStates() throws SQLException {
        // Events reference cities; aggregate by state and retain every tied maximum.
        String sql = "SELECT State_Name, EventCount FROM (" +
                "SELECT c.State_Name, COUNT(*) AS EventCount, MAX(COUNT(*)) OVER () AS MaxCount " +
                "FROM " + EventsTable + " e " +
                "JOIN " + CitiesTable + " c ON c.City_ID = e.Event_City_ID " +
                "GROUP BY c.State_Name) WHERE EventCount = MaxCount ORDER BY State_Name";
        try (Statement stmt = oracle.createStatement();
             ResultSet rst = stmt.executeQuery(sql)) {
            if (!rst.next()) {
                return new EventStateInfo(0);
            }
            EventStateInfo info = new EventStateInfo(rst.getLong(2));
            do {
                info.addState(rst.getString(1));
            } while (rst.next());
            return info;
        }
    }

    @Override
    // Query 8
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the ID, first name, and last name of the oldest friend of the user
    //            with User ID <userID>
    //        (B) Find the ID, first name, and last name of the youngest friend of the user
    //            with User ID <userID>
    public AgeInfo findAgeInfo(long userID) throws SQLException {
        // Filter the two indexed friendship endpoints before looking up birth dates.
        // Both rankings break equal birthdays in favor of the larger user ID.
        String sql = "WITH UserFriends AS (" +
                "SELECT User2_ID AS Friend_ID FROM " + FriendsTable + " WHERE User1_ID = " + userID +
                " UNION ALL SELECT User1_ID FROM " + FriendsTable + " WHERE User2_ID = " + userID + ") " +
                "SELECT User_ID, First_Name, Last_Name, OldestRank, YoungestRank FROM (" +
                "SELECT u.User_ID, u.First_Name, u.Last_Name, " +
                "ROW_NUMBER() OVER (ORDER BY u.Year_of_Birth, u.Month_of_Birth, " +
                "u.Day_of_Birth, u.User_ID DESC) AS OldestRank, " +
                "ROW_NUMBER() OVER (ORDER BY u.Year_of_Birth DESC, u.Month_of_Birth DESC, " +
                "u.Day_of_Birth DESC, u.User_ID DESC) AS YoungestRank " +
                "FROM UserFriends f JOIN " + UsersTable + " u ON u.User_ID = f.Friend_ID " +
                "WHERE u.Year_of_Birth IS NOT NULL AND u.Month_of_Birth IS NOT NULL " +
                "AND u.Day_of_Birth IS NOT NULL) WHERE OldestRank = 1 OR YoungestRank = 1";
        UserInfo oldest = new UserInfo(-1, "NONE", "NONE");
        UserInfo youngest = new UserInfo(-1, "NONE", "NONE");
        try (Statement stmt = oracle.createStatement();
             ResultSet rst = stmt.executeQuery(sql)) {
            while (rst.next()) {
                UserInfo user = new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3));
                if (rst.getInt(4) == 1) {
                    oldest = user;
                }
                if (rst.getInt(5) == 1) {
                    youngest = user;
                }
            }
        }
        return new AgeInfo(oldest, youngest);
    }

    @Override
    // Query 9
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find all pairs of users that meet each of the following criteria
    //              (i) same last name
    //              (ii) same hometown
    //              (iii) are friends
    //              (iv) less than 10 birth years apart
    public FakebookArrayList<SiblingInfo> findPotentialSiblings() throws SQLException {
        FakebookArrayList<SiblingInfo> results = new FakebookArrayList<SiblingInfo>("\n");
        // The stored edge already guarantees User1_ID < User2_ID.
        String sql = "SELECT u1.User_ID, u1.First_Name, u1.Last_Name, " +
                "u2.User_ID, u2.First_Name, u2.Last_Name " +
                "FROM " + FriendsTable + " f " +
                "JOIN " + UsersTable + " u1 ON u1.User_ID = f.User1_ID " +
                "JOIN " + UsersTable + " u2 ON u2.User_ID = f.User2_ID " +
                "JOIN " + HometownCitiesTable + " h1 ON h1.User_ID = f.User1_ID " +
                "JOIN " + HometownCitiesTable + " h2 ON h2.User_ID = f.User2_ID " +
                "WHERE u1.Last_Name = u2.Last_Name AND h1.Hometown_City_ID = h2.Hometown_City_ID " +
                "AND ABS(u1.Year_of_Birth - u2.Year_of_Birth) < 10 " +
                "ORDER BY u1.User_ID, u2.User_ID";
        try (Statement stmt = oracle.createStatement();
             ResultSet rst = stmt.executeQuery(sql)) {
            while (rst.next()) {
                results.add(new SiblingInfo(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)),
                        new UserInfo(rst.getLong(4), rst.getString(5), rst.getString(6))));
            }
        }
        return results;
    }

    // Member Variables
    private Connection oracle;
    private final String UsersTable = FakebookOracleConstants.UsersTable;
    private final String CitiesTable = FakebookOracleConstants.CitiesTable;
    private final String FriendsTable = FakebookOracleConstants.FriendsTable;
    private final String CurrentCitiesTable = FakebookOracleConstants.CurrentCitiesTable;
    private final String HometownCitiesTable = FakebookOracleConstants.HometownCitiesTable;
    private final String ProgramsTable = FakebookOracleConstants.ProgramsTable;
    private final String EducationTable = FakebookOracleConstants.EducationTable;
    private final String EventsTable = FakebookOracleConstants.EventsTable;
    private final String AlbumsTable = FakebookOracleConstants.AlbumsTable;
    private final String PhotosTable = FakebookOracleConstants.PhotosTable;
    private final String TagsTable = FakebookOracleConstants.TagsTable;
}
