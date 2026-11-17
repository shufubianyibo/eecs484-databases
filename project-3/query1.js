// Query 1
// Find users whose hometown is city "city".
// Return an array of user_ids. The order does not matter.

function find_user(city, dbname) {
    db = db.getSiblingDB(dbname);

    let results = [];
    db.users.find({ "hometown.city": city }, { _id: 0, user_id: 1 })
        .forEach(function (user) { results.push(user.user_id); });
	
    return results;
}
