import { Kysely, SqliteDialect } from "kysely";
import SQLite from "better-sqlite3";
import { Database } from "./schema";
import Logger from "../utils/logger";
import path from "path";

let dbInstance: Kysely<Database>;

export function getDatabase(): Kysely<Database> {
  if (dbInstance) return dbInstance;

  Logger.info("Initializing Kysely with SQLite Dialect");
  const dbPath = path.resolve(process.cwd(), "database.sqlite");

  dbInstance = new Kysely<Database>({
    dialect: new SqliteDialect({
      database: new SQLite(dbPath),
    }),
  });

  return dbInstance;
}

export async function initDatabase() {
  const db = getDatabase();

  try {
    await db.schema
      .createTable("notifications")
      .ifNotExists()
      .addColumn("id", "text", (col) => col.primaryKey())
      .addColumn("title", "text", (col) => col.notNull())
      .addColumn("message", "text")
      .addColumn("url", "text")
      .addColumn("icon", "text")
      .addColumn("color", "text")
      .addColumn("token", "text")
      .addColumn("createdAt", "text", (col) => col.notNull())
      .execute();

    await db.schema
      .createTable("registrations")
      .ifNotExists()
      .addColumn("token", "text", (col) => col.primaryKey())
      .addColumn("createdAt", "timestamp", (col) =>
        col.defaultTo(new Date().toISOString()),
      )
      .execute();

    Logger.info("Database initialized successfully");
  } catch (err) {
    Logger.error("Failed to initialize database schema", err);
    throw err;
  }
}
