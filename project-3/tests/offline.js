// Minimal in-memory shell mock: no MongoDB server or network required.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const clone = value => JSON.parse(JSON.stringify(value));
const get = (doc, field) => field.split('.').reduce((v, k) => v == null ? undefined : v[k], doc);
function matches(doc, query) {
    return Object.entries(query).every(([field, condition]) => {
        const value = get(doc, field);
        if (condition && typeof condition === 'object') {
            return Object.entries(condition).every(([op, target]) => {
                switch (op) {
                    case '$exists': return (value !== undefined) === target;
                    case '$ne': return value !== target;
                    case '$gt': return value > target;
                    case '$lt': return value < target;
                    default: throw new Error('Unsupported match: ' + op);
                }
            });
        }
        return value === condition;
    });
}
function expr(doc, value) {
    if (typeof value === 'string' && value.startsWith('$')) return get(doc, value.slice(1));
    if (value && value.$size) return expr(doc, value.$size).length;
    return value;
}
function project(doc, spec) {
    const result = {};
    for (const [key, value] of Object.entries(spec)) {
        if (value !== 0) result[key] = value === 1 ? doc[key] : expr(doc, value);
    }
    if (spec._id === undefined && doc._id !== undefined) result._id = doc._id;
    return result;
}
function cursor(rows) {
    let i = 0;
    return { forEach: fn => rows.forEach(fn), hasNext: () => i < rows.length,
        next: () => rows[i++], toArray: () => rows };
}
function session(data) {
    const collections = { users: clone(data).map((u, i) => ({ ...u, _id: i })) };
    const db = {
        getSiblingDB(name) { assert.equal(name, 'offline'); return db; },
        users: {
            find(query = {}, fields) {
                return cursor(collections.users.filter(u => matches(u, query))
                    .map(u => fields ? project(u, fields) : clone(u)));
            },
            aggregate(pipeline) {
                let rows = clone(collections.users);
                for (const stage of pipeline) {
                    const [op, spec] = Object.entries(stage)[0];
                    switch (op) {
                        case '$match': rows = rows.filter(u => matches(u, spec)); break;
                        case '$unwind': {
                            const key = spec.slice(1);
                            rows = rows.flatMap(u => u[key].map(v => ({ ...u, [key]: v })));
                            break;
                        }
                        case '$project': rows = rows.map(u => project(u, spec)); break;
                        case '$group': {
                            const groups = new Map();
                            for (const row of rows) {
                                const id = expr(row, spec._id);
                                if (!groups.has(id)) groups.set(id, []);
                                groups.get(id).push(row);
                            }
                            rows = Array.from(groups, ([id, members]) => {
                                const row = { _id: id };
                                for (const [field, acc] of Object.entries(spec)) {
                                    if (field === '_id') continue;
                                    const [kind, value] = Object.entries(acc)[0];
                                    const values = members.map(m => expr(m, value));
                                    if (kind === '$addToSet') row[field] = [...new Set(values)];
                                    else if (kind === '$sum') row[field] = values.reduce((a, b) => a + b, 0);
                                    else if (kind === '$avg') row[field] = values.reduce((a, b) => a + b, 0) / values.length;
                                    else throw new Error('Unsupported accumulator: ' + kind);
                                }
                                return row;
                            });
                            break;
                        }
                        case '$sort': rows.sort((a, b) => {
                            for (const [key, direction] of Object.entries(spec)) {
                                if (a[key] !== b[key]) return (a[key] < b[key] ? -1 : 1) * direction;
                            }
                            return 0;
                        }); break;
                        case '$out': {
                            const ids = rows.filter(r => r._id !== undefined).map(r => r._id);
                            assert.equal(new Set(ids).size, ids.length, 'duplicate output _id');
                            collections[spec] = clone(rows); rows = []; break;
                        }
                        default: throw new Error('Unsupported stage: ' + op);
                    }
                }
                return cursor(rows);
            }
        }
    };
    const context = vm.createContext({ db });
    for (let i = 1; i <= 7; i++) vm.runInContext(fs.readFileSync(path.join(root, `query${i}.js`), 'utf8'), context);
    return { context, collections };
}
const canonical = values => Array.from(values, v => JSON.stringify(v)).sort();
function verify(data) {
    const { context: q, collections: c } = session(data);
    const original = clone(c.users);
    const ids = data.map(u => u.user_id);
    const edges = data.flatMap(u => u.friends.map(f => [u.user_id, f]));
    const edgeKeys = new Set(edges.map(([a, b]) => `${Math.min(a, b)},${Math.max(a, b)}`));
    const areFriends = (a, b) => edgeKeys.has(`${Math.min(a, b)},${Math.max(a, b)}`);
    for (const city of new Set(['absent', ...data.map(u => u.hometown.city).filter(Boolean)])) {
        assert.deepEqual(clone(q.find_user(city, 'offline')).sort((a,b) => a-b),
            data.filter(u => u.hometown.city === city).map(u => u.user_id).sort((a,b) => a-b));
    }
    for (let repeat = 0; repeat < 2; repeat++) {
        q.unwind_friends('offline');
        assert.deepEqual(canonical(c.flat_users.map(u => [u.user_id, u.friends])), canonical(edges));
        q.cities_table('offline');
        const expectedCities = [...new Set(data.map(u => u.current.city).filter(v => v != null))];
        assert.equal(c.cities.length, expectedCities.length);
        for (const city of expectedCities) assert.deepEqual(c.cities.find(c => c._id === city).users.sort((a,b)=>a-b),
            data.filter(u => u.current.city === city).map(u => u.user_id).sort((a,b)=>a-b));
        q.users_born_by_month('offline');
        assert.deepEqual(c.countbymonth, [...new Set(data.map(u => u.MOB))].sort((a,b)=>a-b)
            .map(month => ({ MOB: month, borncount: data.filter(u => u.MOB === month).length })));
    }
    for (const diff of [-1, 0, 1, 5, 20]) {
        const expected = [];
        for (const a of data) for (const b of data) {
            if (a.gender === 'male' && b.gender === 'female' && a.hometown.city != null &&
                a.hometown.city === b.hometown.city && Math.abs(a.YOB - b.YOB) < diff &&
                !areFriends(a.user_id, b.user_id)) {
                expected.push([a.user_id, b.user_id]);
            }
        }
        assert.deepEqual(canonical(q.suggest_friends(diff, 'offline')), canonical(expected));
    }
    const oldest = {};
    for (const id of ids) {
        const friends = data.filter(u => areFriends(id, u.user_id));
        friends.sort((a,b) => a.YOB - b.YOB || a.user_id - b.user_id);
        if (friends.length) oldest[id] = friends[0].user_id;
    }
    // Query 5 must also work before any helper collection has been created.
    assert.deepEqual(clone(session(data).context.oldest_friend('offline')), oldest);
    assert.equal(q.find_average_friendcount('offline'), data.length ? edges.length / data.length : 0);
    assert.deepEqual(c.users, original, 'queries must not change users');
    return { q, c, oldest };
}
const sample = JSON.parse(fs.readFileSync(path.join(root, 'sample.json'), 'utf8'));
const { q, c, oldest } = verify(sample);
assert.equal(q.find_user('Bucklebury', 'offline').length, 42);
assert.equal(c.flat_users.length, 21355);
assert.equal(c.cities.find(c => c._id === 'Bucklebury').users.length, 43);
assert.equal(q.suggest_friends(5, 'offline').length, 87);
assert.equal(Object.keys(oldest).length, 798);
assert.equal(oldest[799], 51);
assert.equal(q.find_average_friendcount('offline'), 26.69375);
assert.equal(c.countbymonth.length, 12);
assert.equal(c.countbymonth.find(m => m.MOB === 10).borncount, 81);
const user = (id, gender, year, friends, home = 'A', current = 'B') => ({
    user_id: id, gender, YOB: year, MOB: id % 12 + 1, friends,
    hometown: home === null ? {} : { city: home }, current: current === null ? {} : { city: current }
});
verify([
    user(0, 'female', 1980, [2, 5]), user(2, 'male', 1985, [5]),
    user(5, 'female', 1980, []), user(7, 'male', 1980, []),
    user(8, 'female', 1985, []), user(9, 'male', 1980, [], null, null),
    user(10, 'female', 1980, [], null, null), user(11, 'female', 2000, [], 'C')
]);
verify([]);
verify([user(0, 'male', 2000, [], null, null)]);
console.log('PASS: all seven queries, sample expectations, edge cases, repeatability, and users preservation (offline mock).');
