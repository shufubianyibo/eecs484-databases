// Query 6
// Find the average friend count per user.
// Return a decimal value as the average user friend count of all users in the users collection.

function find_average_friendcount(dbname) {
    db = db.getSiblingDB(dbname);

    let result = db.users.aggregate([
        { $group: { _id: null, average: { $avg: { $size: "$friends" } } } }
    ]);
    return result.hasNext() ? result.next().average : 0;
}
