# EECS 484 — Database Management

SQL schema design and views, Java/Oracle queries, data export, and MongoDB aggregation.

Coursework portfolio of **Han Sun ([shufubianyibo](https://github.com/shufubianyibo))**. This repository collects source snapshots from local projects so they can be browsed directly from my personal GitHub profile.

## Main projects

### [Relational-to-document export and MongoDB queries — Project 3](project-3/)

A Java/JDBC exporter combines users, cities, and friendship relationships from relational tables into nested JSON documents. Left joins retain users with missing city information, and friendship edges are assembled into per-user data. JavaScript queries then explore the document representation through filtering, relationship traversal, and aggregation, including oldest-friend selection and birth-month counts.

Start with [`GetData.java`](project-3/GetData.java), the [`query1.js`–`query7.js` scripts](project-3/), and the [offline tests](project-3/tests/). This project shows the change in data representation and query style between Oracle and MongoDB.

### [Fakebook relational query application — Project 2](project-2/)

A Java application that runs SQL queries through JDBC against the course Fakebook schema and maps results into typed Java structures. The work focuses on extracting information from related social-network tables using joins, grouping, ordering, and relationship-based queries.

The main implementation is [`StudentFakebookOracle.java`](project-2/project2/StudentFakebookOracle.java). Database accounts are supplied through environment variables in this public copy; see the setup instructions below.

### [Schema, constraints, and views — Project 1](project-1/)

SQL scripts define tables and constraints for users, friendships, cities, events, albums, photos, and related entities. Separate scripts load source data, expose views, and remove the schema, providing the relational foundation for the later query projects.

Review [`createTables.sql`](project-1/createTables.sql), [`createViews.sql`](project-1/createViews.sql), and [`loadData.sql`](project-1/loadData.sql).

## All projects

| Project | Source files |
| --- | ---: |
| [project-1](project-1/) | 5 |
| [project-2](project-2/) | 6 |
| [project-3](project-3/) | 12 |

## Running the projects

Each directory is independent. Read its README, build file, tests, and `PORTFOLIO.md` before running. Original course infrastructure, data, or toolchains may be required. This publication checks source packaging; it does not certify that every historical project builds on a current machine.

## Attribution

These are individual and collaborative course projects. Course starter code, supplied tests, and third-party dependencies are included where needed. Original credits and licenses are retained. Each `PORTFOLIO.md` links to the source repository and records its base commit and commit authors when available. Source-file counts include tests and scaffolding and are not a measure of individually authored work. Snapshots were collected on 2026-11-17; original working directories were left unchanged.

## Database setup

Set `ORACLE_USERNAME` and `ORACLE_PASSWORD` in your environment before running the Java drivers. Hardcoded account credentials were removed from these public copies. Oracle JDBC jars are course-provided dependencies; consult the build files for classpath setup. MongoDB scripts also require a configured local database.
