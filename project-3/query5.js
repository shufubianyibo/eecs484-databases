// Query 5
// Find the oldest friend for each user who has a friend. For simplicity,
// use only year of birth to determine age, if there is a tie, use the
// one with smallest user_id. You may find query 2 and query 3 helpful.
// You can create selections if you want. Do not modify users collection.
// Return a javascript object : key is the user_id and the value is the oldest_friend id.
// You should return something like this (order does not matter):
// {user1:userx1, user2:userx2, user3:userx3,...}

function oldest_friend(dbname) {
    db = db.getSiblingDB(dbname);

    let results = {};
    let users = {};
    db.users.find({}, { _id: 0, user_id: 1, YOB: 1, friends: 1 })
        .forEach(function (user) { users[user.user_id] = user; });

    function consider(userId, friendId) {
        let previous = results[userId];
        if (previous === undefined || users[friendId].YOB < users[previous].YOB ||
                (users[friendId].YOB === users[previous].YOB && friendId < previous)) {
            results[userId] = friendId;
        }
    }

    Object.keys(users).forEach(function (key) {
        let user = users[key];
        user.friends.forEach(function (friendId) {
            consider(user.user_id, friendId);
            consider(friendId, user.user_id);
        });
    });

    return results;
}
