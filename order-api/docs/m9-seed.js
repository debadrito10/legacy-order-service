const statuses = ["NEW", "PAID", "SHIPPED", "CANCELLED"];
const docs = [];
for (let i = 0; i < 50000; i++) {
    docs.push({
        customer: "c" + (i % 500),
        amountPaise: 1000 + i,
        currency: "INR",
        status: statuses[Math.floor(Math.random() * 4)],
        createdAt: new Date(Date.now() - i * 60000),
        version: 0
    });
}
db.orders.insertMany(docs);
print(db.orders.countDocuments());