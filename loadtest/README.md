# Load test

A [Gatling](https://gatling.io) simulation of customers using Ledgerline at the same time,
through the frontend's nginx like a browser. Each virtual customer signs up, opens a euro wallet,
adds money, and sends money to the same recipient three times while checking their wallets and
activity. All transfers land in one recipient's wallet, so every user competes for the same ledger
rows.

```bash
docker compose up -d --build --wait          # from the repository root
cd loadtest && ./mvnw gatling:test           # 10 new customers a second for 2 minutes
./mvnw gatling:test -DusersPerSecond=25 -Dseconds=300
```

The run fails if more than 1% of requests fail, if the 95th percentile of all requests is above
500 ms, or if that of a payment is above 300 ms. The HTML report is in `target/gatling/`.

Results and what they mean: [docs/performance.md](../docs/performance.md).
