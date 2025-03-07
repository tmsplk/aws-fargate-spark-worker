db = db.getSiblingDB("testdb");

db.createCollection("testcollection");

db.testcollection.insertMany([
    { name: "Alice", age: 30 },
    { name: "Bob", age: 25 }
]);